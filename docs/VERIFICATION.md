# Проверки

## Фактически выполнено в среде подготовки

- Прочитана backend main `796d2fbdea6737d6252c869f6b09acb912facffc`; AGENTS.md и README на ней отсутствовали.
- Проверены DTO/AuthApi/AuthRepository Android main `341b613a4397a85e03286f5aee3439829b486062`;
  Android не изменялся.
- `git diff --check`: успешно.
- `python -m py_compile scripts/smoke_auth.py`: успешно.
- `git bundle verify` и проверка применения patch к исходной main: успешно.
- YAML конфигурации проверены парсером; переменные обязательных секретов оставлены пустыми
  в `.env.example`, реальные секреты не добавлялись.
- Попытка Gradle wrapper: **не запустилась**, скачивание Gradle завершилось
  `java.net.SocketException: Network is unreachable`. Установлен только Java 17,
  проект требует JDK 25, локального Gradle cache нет. Docker в среде отсутствует.
- Сборка приложений, JUnit и интеграционный Compose **не выполнялись успешно**.
- GitHub отклонил создание ветки: `403 Resource not accessible by integration`.
  Push через локальный git также не удался из-за отсутствия credentials.
  Поэтому PR и GitHub Actions пока не запущены. Это ограничения доступа, не результаты тестов.

## Добавленные проверки (результат выполнения пока не получен)

| Набор | Проверяемое поведение |
| --- | --- |
| AuthMvpIntegrationTest | Реальные PostgreSQL, Flyway, MVC validation/security/commit: регистрация/login/me без подтверждения, явная отправка, SMTP-исключение, одноразовая ссылка, cooldown, независимые гости, upgrade с тем же ID, занятый email, неверные данные/JWT, старые verified/null аккаунты |
| AuthMigrationIntegrationTest | Принятие существующей схемы, сохранение UUID/хеша/true-флага, null→false, повторный migrate, отказ при коллизии email без объединения |
| GuestCreationRateLimiterTest | Лимит IP, независимость другого IP, глобальный бюджет при ротации IP |
| JwtAuthenticationFilterTest / JwtServiceTest | Неподтверждённый пользователь допускается, связь UUID с существующим пользователем, подпись и истечение |
| GuestJwtTest в habit/savings | UUID-subject JWT, два владельца, отсутствие exp/UUID, некорректный UUID, неверная подпись и истечение |
| scripts/smoke_auth.py | Полный HTTP стек без SMTP, реальные записи привычки/цели/накоплений, запрет чужого чтения/изменения, сохранение данных после upgrade, недействительные JWT во всех сервисах |
| Существующие тесты | Продолжают запускаться, финансовые правила не меняются |

GitHub workflow `Backend MVP auth`: отдельные jobs для `build` всех сервисов с PostgreSQL/RabbitMQ
и для Docker Compose smoke. Он должен дать фактический результат после публикации ветки/PR.
Не считать наличие workflow доказательством успешных проверок. Версии JDK/Gradle/Kotlin/Spring
из исходного репозитория сохранены; springdoc в habits согласован с auth (3.1.0),
поскольку [официальная матрица](https://springdoc.org/faq.html) требует 3.x для Boot 4; доступность скачивания зависимостей здесь не подтверждена.

Дальнейшие ограничения и решения перечислены в AUTH_DECISIONS.md.
