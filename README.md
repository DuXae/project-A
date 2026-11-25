# project-A

JSON API на **Spring Boot 3.5 / Java 21** с архитектурой **MVC**, PostgreSQL и
миграциями Flyway. Проект ведётся по Spec Driven Development — см. [AGENTS.md](./AGENTS.md).

## Архитектура: MVC + слой service

Классический MVC в терминах Spring MVC:

| Слой | Пакет | Ответственность |
|---|---|---|
| **Model** | `domain` | JPA-сущности, правила предметной области, не зависят от web |
| **View** | `web/dto` | сериализация в JSON через DTO (никаких сущностей наружу) |
| **Controller** | `web` | HTTP-эндпоинты, разбор запроса, вызов сервиса |
| *(бизнес-логика)* | `service` | транзакции, правила, оркестрация репозиториев |
| *(доступ к данным)* | `repository` | Spring Data JPA |

```
HTTP -> NoteController -> NoteService -> NoteRepository -> PostgreSQL
              |                 |
          DTO (View)      @Transactional
```

Правила зависимостей: `web` → `service` → `repository` → `domain`.
`service` не знает про HTTP и DTO; `domain` не зависит ни от чего.

Подробности решения — [specs/mvc-skeleton/plan.md](./specs/mvc-skeleton/plan.md).

## Требования

- JDK 21
- PostgreSQL 17 (локально или в контейнере)
- Maven не нужен — в проекте есть wrapper `mvnw`

## Запуск

1. Создать базу:
   ```powershell
   $env:PGPASSWORD="postgres"
   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h localhost -c "CREATE DATABASE project_a;"
   ```
2. Задать переменные окружения (или скопировать `.env.example` → `.env`):
   ```powershell
   $env:DB_HOST="localhost"; $env:DB_PORT="5432"; $env:DB_NAME="project_a"
   $env:DB_USERNAME="postgres"; $env:DB_PASSWORD="postgres"
   ```
3. Запустить:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
4. Проверить:
   ```powershell
   Invoke-RestMethod http://localhost:8080/actuator/health
   ```

Flyway создаст таблицу `notes` автоматически при старте.

## API

Базовый путь: `/api/v1/notes`

| Метод | Путь | Описание | Ответ |
|---|---|---|---|
| GET | `/api/v1/notes?page=0&size=20` | Список с пагинацией | 200 `PageResponse<NoteResponse>` |
| GET | `/api/v1/notes/{id}` | Одна запись | 200 / 404 |
| POST | `/api/v1/notes` | Создать | 201 + `Location` |
| PUT | `/api/v1/notes/{id}` | Обновить | 200 / 404 |
| DELETE | `/api/v1/notes/{id}` | Удалить | 204 / 404 |

Пример:

```powershell
$body = @{ title = "Первая заметка"; content = "Привет" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/notes `
  -ContentType "application/json" -Body $body
```

### Формат ошибки

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "details": ["title: title must not be blank"],
  "timestamp": "2026-11-25T07:30:00Z"
}
```

Коды: `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `NOT_FOUND`, `INTERNAL_ERROR`.

## Веб-страница ввода

После запуска откройте **http://localhost:8080/** — форма по центру экрана:
таблица «Товар / Количество» из 30 строк с прокруткой и закреплённой шапкой.

- Заполняйте только нужные строки, пустые пропускаются.
- «Сохранить» отправляет пачку одной транзакцией в `POST /api/v1/products/bulk`.
- Ошибки валидации подсвечивают строку и показываются текстом.
- После успеха поля очищаются, ниже обновляется список последних записей.

## API товаров

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/v1/products?page=0&size=10` | Последние сохранённые товары |
| POST | `/api/v1/products/bulk` | Сохранить пачку строк одной транзакцией |

```json
{ "items": [ { "name": "Гвоздь 50мм", "quantity": 100 } ] }
```

## Тесты

```powershell
.\mvnw.cmd test
```

Покрытие: unit-тесты сервиса (Mockito), тесты контроллера (MockMvc),
интеграционный тест контекста с миграциями Flyway на H2.

## Структура

```
pom.xml
mvnw / mvnw.cmd / .mvn/
src/main/java/com/duxae/projecta/
  ProjectAApplication.java
  domain/Note.java
  repository/NoteRepository.java
  service/NoteService.java
  service/ResourceNotFoundException.java
  web/NoteController.java
  web/dto/NoteRequest.java, NoteResponse.java, PageResponse.java
  web/error/ErrorResponse.java, GlobalExceptionHandler.java
src/main/resources/
  application.yml
  db/migration/V1__create_notes.sql
src/test/...
specs/                     SDD-артефакты
```

## Как добавить новую сущность

По SDD: спека → план → задачи → код. Технически повторяем цепочку
`domain` → `repository` → `service` → `dto` → `controller`, добавляем миграцию
`V2__...sql` и тесты на три уровня.

## Реестр фич

| Фича | Статус | Спека |
|------|--------|-------|
| Каркас проекта на MVC-архитектуре | done | [specs/mvc-skeleton](./specs/mvc-skeleton/spec.md) |
| Форма ввода товаров (30 строк) | done | [specs/product-form](./specs/product-form/spec.md) |
