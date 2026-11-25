# Tasks: Форма ввода товаров (таблица 30 строк)

**Спека:** [spec.md](./spec.md) · **План:** [plan.md](./plan.md)

## Фаза 1. Данные

- [x] **T-1** (FR-5) Миграция `V2__create_products.sql`
  - файлы: `src/main/resources/db/migration/V2__create_products.sql`
  - проверка: `flyway_schema_history` v2 `create products` success=t
- [x] **T-2** (FR-5) `Product`, `ProductRepository`
  - файлы: `domain/Product.java`, `repository/ProductRepository.java`

## Фаза 2. API

- [x] **T-3** (FR-4, FR-6) DTO: `ProductRequest`, `ProductBulkRequest`, `ProductResponse`, `BulkSaveResponse`
  - файлы: `web/dto/ProductRequest.java`, `ProductBulkRequest.java`, `ProductResponse.java`, `BulkSaveResponse.java`
- [x] **T-4** (FR-4) `ProductService.saveAll` в одной транзакции
  - файлы: `service/ProductService.java`
  - тесты: `ProductServiceTest` — 3 теста
- [x] **T-5** (FR-4) `ProductController`: `POST /api/v1/products/bulk`, `GET /api/v1/products`
  - файлы: `web/ProductController.java`
  - тесты: `ProductControllerTest` — 9 тестов

## Фаза 3. Фронтенд

- [x] **T-6** (FR-1, FR-2) `index.html` + генерация 30 строк × 2 колонки
  - файлы: `static/index.html`, `static/js/app.js`
  - проверка: 30 `tr`, по 2 `td` в строке
- [x] **T-7** (FR-3) Прокрутка области таблицы, липкая шапка, центрирование
  - файлы: `static/css/styles.css`
  - проверка: `clientHeight=418`, `scrollHeight=1284` — прокрутка есть
- [x] **T-8** (FR-7, FR-8) Отправка в API, отображение ошибок, очистка после успеха
  - файлы: `static/js/app.js`
  - проверка: при 2 невалидных строках POST не отправлен, строки подсвечены
- [x] **T-9** (FR-9) Список последних сохранённых записей
  - файлы: `static/js/app.js` (`refreshRecent`, `renderRecent`)

## Фаза 4. Верификация

- [x] **T-10** (AC-3, AC-5) Проверка в браузере на реальной PostgreSQL
  - 3 строки → 3 записи в `products`; вторая проверка: кириллица `Проверка кириллицы` сохранена
- [x] **T-11** (AC-7) `mvnw test` — 28 тестов, 0 падений
- [x] **T-12** README обновлён (страница, API products)

## Отклонения от плана

| Задача | Что изменилось | Причина |
|--------|----------------|---------|
| T-6 | Добавлены `aria-label` для полей строк | Доступность: 60 одинаковых инпутов без меток нечитаемы для скринридера |
| T-6 | Placeholder количества `Кол-во` вместо `0` | `0` выглядел как введённое значение и путал при валидации |

## Итог

- Дата завершения: 2026-11-25
- Коммиты: см. историю git
- Незакрытые вопросы: редактирование/удаление из UI, аутентификация — вне скоупа
