# Авторизация Android MVP

Базовый URL auth: `http://<адрес компьютера>:8080`. Habits: 8081; savings: 8082.
В production используйте HTTPS. JSON в UTF-8, `Content-Type: application/json`.

## Контракт

| Метод | Путь | Авторизация | Ответ |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Нет | 201, `token` и `user` |
| POST | `/api/auth/login` | Нет | 200, `token` и `user` |
| POST | `/api/auth/guest` | Нет | 201, `token` и `user`, тело не нужно |
| POST | `/api/auth/guest/upgrade` | Bearer гостя | 200, `token` и `user`, тот же UUID |
| GET | `/api/auth/me` | Bearer | 200, актуальный `user` |
| POST | `/api/auth/request-verification` | Bearer обычного аккаунта | 200, запрос принят, тело не нужно |
| POST | `/api/auth/resend-verification` | Нет, сохранён прежний endpoint | 200, одинаковый ответ для неизвестного, подтверждённого и неподтверждённого email |
| GET | `/api/auth/verify-email?token=...` | Нет | 200 при подтверждении; 400 для неверной, просроченной или использованной ссылки |

Регистрация и преобразование гостя принимают одинаковое тело:

```json
{"email":"person@example.com","nickname":"Егор","password":"example-password"}
```

Успешная регистрация, вход или преобразование:

```json
{
  "token": "<signed JWT>",
  "user": {
    "id": "71619113-5551-4557-bffc-c447b66a6220",
    "email": "person@example.com",
    "nickname": "Егор",
    "role": "USER",
    "emailVerified": false,
    "isGuest": false
  }
}
```

Вход:

```http
POST /api/auth/login
Content-Type: application/json

{"email":"person@example.com","password":"example-password"}
```

Создание гостя:

```http
POST /api/auth/guest
```

```json
{
  "token": "<guest JWT>",
  "user": {
    "id": "71619113-5551-4557-bffc-c447b66a6220",
    "email": "",
    "nickname": "Гость",
    "role": "USER",
    "emailVerified": false,
    "isGuest": true
  }
}
```

Пустая строка email сохраняет совместимость с текущим Android `UserDto.email: String`.
В БД email и password_hash гостя — SQL NULL; вымышленные адреса не используются.

Преобразование:

```http
POST /api/auth/guest/upgrade
Authorization: Bearer <guest JWT>
Content-Type: application/json

{"email":"person@example.com","nickname":"Егор","password":"example-password"}
```

Ответ имеет тот же `user.id`, `isGuest=false`, `emailVerified=false`. Привычки, цели и
подтверждённые накопления продолжают принадлежать этому UUID. При занятом email — 409;
аккаунты не объединяются, гость остаётся гостем. Повторное преобразование — 409.

Все защищённые запросы отправляют `Authorization: Bearer <token>`.
UUID владельца берётся из проверенного JWT, а не из параметров клиента:

```http
GET /api/habits
Authorization: Bearer <token>

GET /api/savings/me
Authorization: Bearer <token>

GET /api/savings/goals
Authorization: Bearer <token>
```

## Добровольное подтверждение

Регистрация, вход и преобразование не создают письмо. По нажатию кнопки клиента:

```http
POST /api/auth/request-verification
Authorization: Bearer <token>
```

```json
{"message":"Запрос принят. При отсутствии подтверждения будет предпринята отправка письма"}
```

Это подтверждение приёма запроса, не доставки письма. Отправка выполняется после commit,
ошибка SMTP не превращает успешную транзакцию в ошибку HTTP. При сбое повторить после
cooldown. Письмо содержит ссылку `VERIFICATION_URL?token=...`; срок 24 часа, одно использование.
Новый запрос аннулирует прежние неиспользованные ссылки. Лимит: 2 минуты между запросами,
не более 5 за 24 часа на email в одном экземпляре auth. Запросы для уже подтверждённого
email ничего не меняют. После перехода по ссылке обновить `GET /api/auth/me`.
Гостю endpoint вернёт 409 с предложением сначала зарегистрировать аккаунт.

Старый публичный resend принимает `{"email":"person@example.com"}`; его ответ не раскрывает
наличие аккаунта, лимит и результат доставки. Новый клиент использует authenticated endpoint.

## Валидация и ошибки

Email проверяется на формат и длину до 254 символов; при сохранении и поиске приводится
к lowercase/trim. Никнейм обязателен, до 80 символов. Новый пароль: 6–72 символа и не более
72 байт UTF-8 (ограничение BCrypt). Проверка существующих паролей при входе сохранена.

| HTTP | Условие |
| --- | --- |
| 400 | Некорректный JSON, поля, пароль или ссылка подтверждения |
| 401 | Неверный email/пароль; отсутствующий, повреждённый, просроченный JWT, неверная подпись или UUID |
| 409 | Email занят, аккаунт уже преобразован, запрос подтверждения от гостя |
| 429 | Превышен лимит создания гостевых сессий |
| 403 / 404 | Попытка изменить чужую привычку / получить или изменить чужую цель |

Ошибки auth из контроллера:

```json
{"status":409,"message":"Email уже занят","timestamp":1793820000000}
```

Ошибки уровня security могут иметь пустое или стандартное servlet-тело. Клиент должен
обрабатывать HTTP-код без обязательного разбора `message`. Verify-email возвращает
`{"message":"Электронная почта успешно подтверждена"}` или 400 с полем `error`.

## Что подключить в Android (в этом PR Android не меняется)

1. Регистрация и login уже ожидают `AuthResponse(token,user)` — контракт восстановлен.
2. Добавить guest, upgrade и request-verification методы Retrofit; сохранять guest-токен
   через тот же TokenManager, что обычный. Не создавать гостя заново при каждом запуске.
3. Добавить `emailVerified` и `isGuest` в `UserDto` с defaults `false`; ID и email остаются String.
4. После upgrade заменить токен ответом сервера, обновить user; локальные данные по UUID
   не переносить и не удалять. При 409 оставить гостевую сессию активной.
5. Показывать гостю срок из JWT `exp` и возможность регистрации до истечения срока.
   Разбор `exp` на клиенте нужен только для интерфейса; доверие к токену определяет сервер.
6. На 401 обычного аккаунта предложить вход; гостю объяснить потерю доступа. Не создавать
   новый аккаунт незаметно и не выдавать его за восстановление старых данных.
