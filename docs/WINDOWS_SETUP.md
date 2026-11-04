# Запуск Windows и миграция

## Полный запуск через Docker Desktop

Нужны Git, Docker Desktop с Linux containers / WSL2 и PowerShell. Java/Gradle на Windows
для этого варианта не нужны: образы собираются с Java 25 внутри Docker.

```powershell
git clone https://github.com/pojalustayuidi/coinhabit-backend.git
cd coinhabit-backend
git switch feat/android-mvp-auth
Copy-Item .env.example .env
```

Создать три независимых случайных значения в `.env` (PowerShell 5.1+):

```powershell
function New-CoinHabitSecret {
    $bytes = New-Object byte[] 48
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
$lines = Get-Content .env
foreach ($key in @('POSTGRES_PASSWORD', 'RABBITMQ_PASSWORD', 'JWT_SECRET')) {
    $value = New-CoinHabitSecret
    $lines = $lines -replace "^$key=.*$", "$key=$value"
}
$lines | Set-Content -Encoding Ascii .env
```

Повторно генератор для существующей БД не запускать: смена переменной пароля не меняет
пароль роли внутри PostgreSQL/RabbitMQ, а смена JWT_SECRET аннулирует все текущие JWT.
Файл `.env` исключён из Git и Docker build context. Значения в примере пустые намеренно.

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 auth-service habit-service savings-service
```

Запускаются PostgreSQL, RabbitMQ, идемпотентное создание habit_db/savings_db и все три приложения.
Отсутствие SMTP не блокирует запуск, регистрацию и вход. Для локальных писем:

```powershell
docker compose --profile mail up -d --build
```

Mailpit UI: http://localhost:8025. RabbitMQ UI: http://localhost:15672 (пользователь `coinhabit`,
пароль из `.env`). Базы: localhost:15432, пользователь postgres; auth DB — coinhabit_auth.
Если SMTP внешний, заполнить SMTP_HOST/PORT/USERNAME/PASSWORD/AUTH/STARTTLS/STARTTLS_REQUIRED.
VERIFICATION_URL должен быть доступен получателю письма. Для S25+ вместо localhost указать
локальный IP компьютера или использовать `adb reverse tcp:8080 tcp:8080` для открытия ссылки
на телефоне через USB.

Проверка без писем:

```powershell
$body = @{email='tester@example.com'; nickname='Tester'; password='test-password'} | ConvertTo-Json
$auth = Invoke-RestMethod http://localhost:8080/api/auth/register -Method Post -ContentType 'application/json' -Body $body
Invoke-RestMethod http://localhost:8080/api/auth/me -Headers @{Authorization="Bearer $($auth.token)"}
$guest = Invoke-RestMethod http://localhost:8080/api/auth/guest -Method Post
Invoke-RestMethod http://localhost:8081/api/habits -Headers @{Authorization="Bearer $($guest.token)"}
```

Для Android по USB:

```powershell
adb reverse tcp:8080 tcp:8080
adb reverse tcp:8081 tcp:8081
adb reverse tcp:8082 tcp:8082
```

Тогда клиент использует `http://127.0.0.1:8080`, `:8081`, `:8082` на устройстве. По Wi-Fi —
IPv4 компьютера из `ipconfig`, одна сеть и разрешение портов в Windows Firewall для частной
сети. Порты API Compose доступны с устройств сети; БД и management UI привязаны к loopback.
Не публикуйте локальный стек напрямую в Интернет. Android-конфигурацию cleartext и base URL
в этом PR не меняем.

Остановка сохраняет данные:

```powershell
docker compose down
```

## Обновление существующего окружения

1. Остановить старые приложения, сохранить резервные копии всех трёх БД (например, через
   `pg_dump` в pgAdmin с форматом Custom). Убедиться, что пароли `.env` соответствуют
   существующим ролям. Не удалять volumes и не выполнять `down -v`.
2. Собрать новую ветку и выполнить `docker compose up -d --build`. Init job создаст только
   отсутствующие habit_db/savings_db, в том числе на старом postgres volume.
3. Auth применяет Flyway V1 автоматически: существующая схема принимается как baseline 0;
   новая создаётся с нуля. Затем Hibernate только проверяет схему (`validate`). Миграция
   PostgreSQL транзакционная: ID, password_hash, role и true-флаги остаются прежними;
   null-флаг становится false. Добавляется is_guest=false, email/password_hash допускают
   NULL только у гостя, вводится нормализованная уникальность email.
4. Если адреса существующих аккаунтов отличаются только регистром или пробелами, миграция
   остановится, не объединяя их. Нужна ручная проверка владельцев и разрешение конфликта
   до повторного запуска. Не удалять записи автоматически. Перевод email в lowercase может
   потребовать повторный вход для старого JWT с ненормализованным subject.
5. Проверить `/api/auth/me`, вход, чтение привычек/целей/накоплений. Неподтверждённые старые
   аккаунты получают доступ без искусственного выставления verified=true. Старые ссылки
   остаются действительными до исходного истечения/использования или нового запроса.

Habit/savings остаются на существующем Hibernate `update`; их таблицы и финансовые правила
не мигрируют в рамках этой задачи. Понижение версии после появления гостей потребует
возврата всех трёх БД из согласованной резервной копии: старый auth не поддерживает NULL
email/password_hash. Автоматического разрушительного rollback нет.

## Приложения из IDE / Gradle на хосте

Нужен полный JDK 25 в JAVA_HOME. В отдельных PowerShell-окнах из корня репозитория загрузить
переменные `.env` в окружение процесса (Spring Boot сам `.env` не читает):

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
$env:DB_PASSWORD = $env:POSTGRES_PASSWORD
$env:SMTP_HOST = 'localhost'
docker compose up -d --wait postgres rabbitmq
docker compose run --rm init-databases
```

В первом окне `auth-service\gradlew.bat -p auth-service bootRun`, во втором
`habit-service\gradlew.bat -p habit-service bootRun`, в третьем
`savings-service\gradlew.bat -p savings-service bootRun`. Порты не должны быть заняты
контейнерными экземплярами приложений. По умолчанию используются БД на localhost:15432.
DB_URL/DB_USER можно задать отдельно каждому процессу; JWT_SECRET одинаков во всех.

## Проверки

На одноразовой тестовой БД с той же инфраструктурой и переменными окружения:

```powershell
auth-service\gradlew.bat -p auth-service build
habit-service\gradlew.bat -p habit-service build
savings-service\gradlew.bat -p savings-service build
```

Cross-service smoke требует Python 3 и запущенного полного Compose. Использовать только
одноразовый стек: скрипт создаёт пользователей, привычку, цель и тестовую подтверждённую
транзакцию через psql. Для проверки недоступного SMTP запустить без mail-профиля, с
SMTP_HOST=127.0.0.1 и SMTP_PORT=1 в `.env`, пересоздав auth контейнер.

```powershell
python scripts/smoke_auth.py
```

JUnit XML/HTML: `<service>/build/test-results/test` и `<service>/build/reports/tests/test`.
GitHub Actions содержит сборку/тесты и отдельный полный Compose smoke без SMTP.
