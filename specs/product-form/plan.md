# Plan: Форма ввода товаров (таблица 30 строк)

**Спека:** [spec.md](./spec.md)
**Дата:** 2026-11-25

## 1. Подход

| Вариант | Плюсы | Минусы | Решение |
|---------|-------|--------|---------|
| Статика в `src/main/resources/static` + REST на том же origin | Нет CORS, нет отдельной сборки, деплой одним артефактом | Нет SPA-роутинга | **выбрано** |
| React/Vite отдельным приложением | Богатый UI | Сборка, node_modules, CORS, второй деплой | отклонено |
| Server-side Thymeleaf | Всё на сервере | Смешивает View и API, противоречит решению «только JSON API» | отклонено |

Итог: статическая страница `index.html` + `app.js` + `styles.css` в
`resources/static`, взаимодействие через `fetch` с `/api/v1/products`.

## 2. Архитектура

Фронтенд — слой **View**. MVC-структура бэкенда не меняется: добавляется
новая ветка `web → service → repository → domain` для `Product`.

```
index.html (View)
   | fetch POST /api/v1/products  {items:[{name,quantity}]}
   v
ProductController  ->  ProductService  ->  ProductRepository  -> products
   |                        @Transactional        JPA
   v
ErrorResponse (400/500)
```

### Контракт API

`POST /api/v1/products/bulk` — сохраняет пачку строк одной транзакцией:

```json
{ "items": [ { "name": "Гвоздь 50мм", "quantity": 100 } ] }
```

Ответ: `201 Created`

```json
{ "saved": 3, "items": [ { "id": 1, "name": "Гвоздь 50мм", "quantity": 100 } ] }
```

Почему bulk, а не 30 отдельных POST: одна транзакция — либо сохранилось всё,
либо ничего; меньше запросов; проще валидация «хотя бы одна строка».

`GET /api/v1/products?page=0&size=20` — для списка под формой (FR-9).

## 3. Структура файлов

```
src/main/java/com/duxae/projecta/
  domain/Product.java
  repository/ProductRepository.java
  service/ProductService.java
  web/ProductController.java
  web/dto/ProductRequest.java          (name, quantity)
  web/dto/ProductBulkRequest.java      (List<ProductRequest> items, @NotEmpty)
  web/dto/ProductResponse.java
  web/dto/BulkSaveResponse.java        (saved, items)

src/main/resources/
  db/migration/V2__create_products.sql
  static/index.html
  static/css/styles.css
  static/js/app.js

src/test/java/com/duxae/projecta/
  service/ProductServiceTest.java
  web/ProductControllerTest.java
```

## 4. Модель данных

Таблица `products`:

| Колонка    | Тип           | Ограничения              |
|------------|---------------|--------------------------|
| id         | BIGINT IDENTITY | PK                     |
| name       | VARCHAR(200)  | NOT NULL                 |
| quantity   | INTEGER       | NOT NULL, CHECK > 0      |
| created_at | TIMESTAMPTZ   | NOT NULL DEFAULT now()   |

Индекс `idx_products_created_at`.

## 5. Зависимости

Новых нет: Jakarta Validation уже подключена, статику отдаёт
`spring-boot-starter-web`.

## 6. Риски и допущения

| #   | Риск | Вероятность | Влияние | Митигация |
|-----|------|-------------|---------|-----------|
| R-1 | Пользователь заполнит 30 строк и потеряет данные при ошибке | средняя | высокое | Bulk-транзакция + ошибки показываются, поля не очищаются при ошибке |
| R-2 | Кириллица в JSON ломается | низкая | среднее | `charset=UTF-8` в заголовке, тест с кириллицей |
| R-3 | Ввод количества с пробелами/запятой | средняя | низкое | `inputmode="numeric"`, trim и парсинг на клиенте, повторная проверка на сервере |
| R-4 | 30 строк × большой текст = тяжёлый запрос | низкая | низкое | Лимит `name` 200 символов, `items` не более 100 |

## 7. План тестирования

| Уровень | Что проверяем | Тест |
|---------|---------------|------|
| Unit | bulk-сохранение, пустой список, маппинг | `ProductServiceTest` |
| Web | 201, 400 при пустом name, 400 при quantity <= 0, 400 при пустом items | `ProductControllerTest` |
| Ручной | 30 строк, прокрутка, шапка, сохранение в БД | чек-лист AC |

## 8. Трассировка требований

| Требование | Реализация | Тест |
|------------|------------|------|
| FR-1 | `static/index.html`, `css/styles.css` | AC-1 |
| FR-2 | `app.js` (генерация 30 строк) | AC-1 |
| FR-3 | `styles.css` (`.table-scroll`, sticky thead) | AC-2 |
| FR-4 | `app.js` (fetch bulk) | AC-3 |
| FR-5 | `V2__create_products.sql`, `ProductService` | AC-3 |
| FR-6 | `ProductRequest` (@NotBlank, @Min(1)) | AC-4 |
| FR-7 | `app.js` (рендер ErrorResponse) | AC-5 |
| FR-8 | `app.js` (сброс заполненных строк) | AC-6 |
| NFR-1 | `resources/static` | ревью |
| NFR-2 | без CDN | ревью |
| NFR-4 | — | `mvnw test` |
