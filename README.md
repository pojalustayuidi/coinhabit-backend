# CoinHabit backend

Три сервиса Kotlin / Spring Boot: auth (8080), habits (8081), savings и цели (8082).
Инфраструктура: PostgreSQL 17 и RabbitMQ. SMTP необязателен.

- [Запуск на Windows и миграция](docs/WINDOWS_SETUP.md)
- [API авторизации для Android](docs/AUTH_API.md)
- [Принятые решения и ограничения](docs/AUTH_DECISIONS.md)
- [Проверки и фактический статус](docs/VERIFICATION.md)

Android-репозиторий не изменялся. Финансовые правила и обмен событиями сохранены.
