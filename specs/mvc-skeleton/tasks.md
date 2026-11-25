# Tasks: Каркас проекта на MVC-архитектуре

**Спека:** [spec.md](./spec.md) · **План:** [plan.md](./plan.md)

## Фаза 1. Сборка и конфигурация

- [x] **T-1** (FR-1) Сгенерировать Maven-проект: `pom.xml`, wrapper `mvnw`, `ProjectAApplication`
  - файлы: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `src/main/java/com/duxae/projecta/ProjectAApplication.java`
  - тесты: `mvnw -DskipTests package` — OK
- [x] **T-2** (FR-8) `application.yml` + профиль `test` на H2, `.env.example`
  - файлы: `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `.env.example`
  - тесты: контекст стартует с H2 — OK

## Фаза 2. Домен и данные

- [x] **T-3** (FR-6) Миграция Flyway `V1__create_notes.sql`
  - файлы: `src/main/resources/db/migration/V1__create_notes.sql`
  - тесты: применяется на H2 и на PostgreSQL (`flyway_schema_history: v1 create notes success=t`)
- [x] **T-4** (FR-2) Сущность `Note` и `NoteRepository`
  - файлы: `domain/Note.java`, `repository/NoteRepository.java`

## Фаза 3. Слой web и service

- [x] **T-5** (FR-3) `NoteService` с CRUD и транзакциями
  - файлы: `service/NoteService.java`, `service/ResourceNotFoundException.java`
  - тесты: `NoteServiceTest` — 8 тестов зелёные
- [x] **T-6** (FR-3) DTO `NoteRequest`, `NoteResponse`, `PageResponse`
  - файлы: `web/dto/*.java`
- [x] **T-7** (FR-3) `NoteController` с CRUD-эндпоинтами
  - файлы: `web/NoteController.java`
  - тесты: `NoteControllerTest` — 7 тестов зелёные
- [x] **T-8** (FR-4, FR-5) Единый формат ошибок и обработчик исключений
  - файлы: `web/error/ErrorResponse.java`, `web/error/GlobalExceptionHandler.java`
  - тесты: 400 `VALIDATION_FAILED`, 400 `MALFORMED_REQUEST`, 404 `NOT_FOUND` — OK

## Фаза 4. Верификация

- [x] **T-9** (AC-1, AC-6) Запуск приложения, проверка `/actuator/health`
  - результат: `{"status":"UP"}`
- [x] **T-10** (AC-3) Ручная проверка CRUD через HTTP на PostgreSQL
  - create 201 + Location, get 200, list 200 с пагинацией, update 200, delete 204, после delete 404
  - кириллица сохраняется корректно (`notes.title = 'Обновлённая'`)
- [x] **T-11** (AC-7) `mvnw test` — 16 тестов, 0 падений
- [x] **T-12** (NFR-4) README с описанием MVC, `.gitignore`, `.env.example`

## Отклонения от плана

| Задача | Что изменилось | Причина |
|--------|----------------|---------|
| T-8 | `NotFoundException` заменён на `ResourceNotFoundException` в пакете `service` | Иначе слой `service` зависел бы от `web`, что нарушает NFR-1 |
| — | Удалён `config/AppProperties.java` | Дублировал биндинг datasource, который Spring делает сам |

## Итог

- Дата завершения: 2026-11-25
- Коммиты: см. историю git в ветке `main`
- Незакрытые вопросы: Swagger/OpenAPI, docker-compose, аутентификация — следующие итерации
