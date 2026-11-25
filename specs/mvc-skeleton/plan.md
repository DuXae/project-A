# Plan: Каркас проекта на MVC-архитектуре

**Спека:** [spec.md](./spec.md)
**Дата:** 2026-11-25

## 1. Подход

| Вариант | Плюсы | Минусы | Решение |
|---------|-------|--------|---------|
| Spring Boot 3 + Spring MVC + Spring Data JPA | Стандарт индустрии, MVC из коробки, экосистема, валидация и миграции готовыми стартерами | Больше «магии», заметный старт | **выбрано** |
| Spring WebFlux + R2DBC | Реактивный стек, высокая нагрузка | Сложнее, избыточно для CRUD-каркаса | отклонено |
| Jakarta EE / Quarkus | Быстрый старт, низкая память | Меньше примеров, сложнее с наймом | отклонено |
| Чистый Servlet API + JDBC | Полный контроль, никакой магии | Много рутины, ручные транзакции | отклонено |

Итог: Java 21 + Spring Boot 3.x + Spring MVC (слой web) + Spring Data JPA
(слой repository) + Flyway + PostgreSQL + Bean Validation.

## 2. Архитектура

MVC в терминах Spring MVC: **Model** — домен + JPA-сущности, **View** —
сериализация DTO в JSON, **Controller** — HTTP-эндпоинты. Между контроллером и
репозиторием добавлен слой service (бизнес-логика, транзакции).

```
HTTP client
    |  JSON
    v
[web]  NoteController        @RestController, /api/v1/notes
    |  NoteRequest / NoteResponse (DTO)
    v
[web]  GlobalExceptionHandler  -> ErrorResponse
    |
    v
[service] NoteService         @Transactional, маппинг DTO <-> entity
    |
    v
[repository] NoteRepository   Spring Data JPA
    |
    v
[domain] Note (entity)
    |
    v
PostgreSQL  <- Flyway миграции
```

Правила зависимостей (проверяются на ревью):
- `web` зависит от `service`, но не от `repository`;
- `service` зависит от `repository` и `domain`;
- `domain` не зависит ни от чего внутри проекта;
- DTO не покидают слой `web` (кроме передачи в service как параметров).

## 3. Структура пакетов и файлов

```
src/main/java/com/duxae/projecta/
  ProjectAApplication.java
  config/OpenApiConfig.java            (опционально)
  domain/Note.java                     (entity, @Entity)
  repository/NoteRepository.java       (JpaRepository<Note, Long>)
  service/NoteService.java             (@Transactional)
  web/
    NoteController.java
    dto/NoteRequest.java               (record + validation)
    dto/NoteResponse.java              (record)
    dto/PageResponse.java              (обёртка пагинации)
    error/ErrorResponse.java
    error/GlobalExceptionHandler.java  (@RestControllerAdvice)
    error/NotFoundException.java
src/main/resources/
  application.yml
  db/migration/V1__create_notes.sql
src/test/java/com/duxae/projecta/
  service/NoteServiceTest.java
  web/NoteControllerTest.java          (@WebMvcTest)
  ProjectAIntegrationTest.java         (опционально, Testcontainers нет -> H2)
```

## 4. Модель данных

Таблица `notes`:

| Колонка     | Тип          | Ограничения            |
|-------------|--------------|------------------------|
| id          | BIGSERIAL    | PK                     |
| title       | VARCHAR(200) | NOT NULL               |
| content     | TEXT         | NULL                   |
| created_at  | TIMESTAMPTZ  | NOT NULL DEFAULT now() |
| updated_at  | TIMESTAMPTZ  | NOT NULL DEFAULT now() |

Индекс: `idx_notes_created_at` для сортировки списка.

## 5. Зависимости

| Артефакт | Зачем |
|---|---|
| `spring-boot-starter-web` | Spring MVC, Jackson |
| `spring-boot-starter-data-jpa` | JPA/Hibernate, репозитории |
| `spring-boot-starter-validation` | Bean Validation |
| `spring-boot-starter-actuator` | health-check |
| `org.flywaydb:flyway-core`, `flyway-database-postgresql` | миграции |
| `org.postgresql:postgresql` | JDBC-драйвер (runtime) |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc |
| `com.h2database:h2` (test) | тесты без внешней БД |

## 6. Риски и допущения

| #   | Риск | Вероятность | Влияние | Митигация |
|-----|------|-------------|---------|-----------|
| R-1 | PostgreSQL не установлен на машине разработчика | высокая | высокое | H2-профиль для тестов, README с инструкцией, `docker-compose.yml` добавим позже |
| R-2 | Пароль БД попадёт в репозиторий | средняя | высокое | `${DB_PASSWORD:postgres}` + `.env.example`, `.gitignore` уже настроен |
| R-3 | Расхождение MVC «академического» и Spring MVC | средняя | низкое | В README пояснить соответствие Model/View/Controller |
| R-4 | Тесты с H2 не ловят различия SQL с PostgreSQL | средняя | среднее | Миграция написана в переносимом SQL; интеграционный тест на PG — в следующей итерации |

## 7. План тестирования

| Уровень | Что проверяем | Инструмент |
|---------|---------------|------------|
| Unit (service) | CRUD, 404 при отсутствии, маппинг DTO | JUnit 5 + Mockito |
| Web (@WebMvcTest) | коды ответов, валидация 400, формат ErrorResponse | MockMvc |
| Контекст | приложение поднимается на H2 | @SpringBootTest |

Привязка к AC: AC-3 -> NoteControllerTest, AC-4 -> NoteControllerTest,
AC-7 -> полный `mvnw test`.

## 8. Трассировка требований

| Требование | Реализация (файл) | Тест |
|------------|-------------------|------|
| FR-1 | `pom.xml`, `mvnw` | `mvnw test` |
| FR-2 | структура пакетов | ревью + ArchUnit (позже) |
| FR-3 | `NoteController`, `NoteService`, `NoteRepository` | `NoteControllerTest`, `NoteServiceTest` |
| FR-4 | `NoteRequest` + `GlobalExceptionHandler` | `NoteControllerTest` |
| FR-5 | `ErrorResponse` | `NoteControllerTest` |
| FR-6 | `db/migration/V1__create_notes.sql` | старт на чистой БД |
| FR-7 | `application.yml` (actuator) | `GET /actuator/health` |
| FR-8 | `application.yml` с `${ENV}` | ревью |
| NFR-2 | — | все тесты |
