# Plan: Страница удаления товаров (мягкое удаление)

**Спека:** [spec.md](./spec.md)
**Дата:** 2026-11-25

## 1. Подход

| Вариант | Плюсы | Минусы | Решение |
|---------|-------|--------|---------|
| Явное поле `deleted_at` + фильтрация в репозитории | Просто, видно в SQL, не влияет на остальные запросы магией | Нужно не забывать фильтровать в новых запросах | **выбрано** |
| Hibernate `@SQLDelete` + `@SQLRestriction` (аннотации) | Прозрачно, фильтрация автоматическая | Скрытая магия, фильтр легко потерять при native-запросах, сложнее тестировать | отклонено |
| Физическое удаление (`DELETE FROM`) | Проще всего | Необратимо, противоречит решению заказчика | отклонено |

Итог: колонка `deleted_at TIMESTAMPTZ NULL` + запросы, которые её учитывают
явно. Такой вариант читается в коде без знания Hibernate-аннотаций.

## 2. Архитектура

```
/products-delete.html  (View)
   |  GET  /api/v1/products           — активные записи
   |  DELETE /api/v1/products/{id}    — мягкое удаление одной
   |  POST /api/v1/products/bulk-delete  { ids: [...] }  — пачка
   v
ProductController -> ProductService -> ProductRepository -> products
                         @Transactional        deleted_at IS NULL
```

Слой `service` получает метод `softDelete(id)` и `softDeleteAll(ids)`.

## 3. Изменения в кодовой базе

| Файл | Действие | Что меняется |
|---|---|---|
| `db/migration/V3__add_products_deleted_at.sql` | создать | колонка `deleted_at`, индекс |
| `domain/Product.java` | изменить | поле `deletedAt`, метод `markDeleted()` |
| `repository/ProductRepository.java` | изменить | `findAllByDeletedAtIsNull`, `findByIdAndDeletedAtIsNull`, `findAllByIdInAndDeletedAtIsNull` |
| `service/ProductService.java` | изменить | `findAll` фильтрует удалённых, `softDelete`, `softDeleteAll` |
| `web/ProductController.java` | изменить | `DELETE /{id}`, `POST /bulk-delete` |
| `web/dto/BulkDeleteRequest.java` | создать | `List<Long> ids` с `@NotEmpty` |
| `web/dto/BulkDeleteResponse.java` | создать | `deleted: int` |
| `static/products-delete.html` | создать | страница |
| `static/js/products-delete.js` | создать | логика |
| `static/products.html` | изменить | навигация |
| `static/index.html` | изменить | навигация |
| `static/css/styles.css` | изменить | чекбоксы, кнопка в строке, диалог |

## 4. Модель данных

```sql
ALTER TABLE products ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_products_deleted_at ON products (deleted_at);
```

`NULL` = активная запись, заполненное значение = удалённая.
Индекс нужен, потому что каждый список фильтрует по `deleted_at IS NULL`.

## 5. Зависимости

Новых нет. Подтверждение в интерфейсе — собственный модальный диалог вместо
`window.confirm`: нужен перечень удаляемых товаров (FR-5), а системный диалог
показывает только текст.

## 6. Риски и допущения

| #   | Риск | Вероятность | Влияние | Митигация |
|-----|------|-------------|---------|-----------|
| R-1 | Забудут фильтр по `deleted_at` в новом запросе, и удалённые всплывут | средняя | высокое | Отдельные методы репозитория с явным именем + тест «удалённый не виден в списке» |
| R-2 | Двойное удаление одной записи из двух вкладок | средняя | низкое | `UPDATE ... WHERE deleted_at IS NULL`, при 0 затронутых строк → 404 |
| R-3 | Изменение `findAll` сломает существующие тесты | высокая | низкое | Тесты мокают `ProductService`, поэтому контракт не меняется; `ProjectAApplicationTest` проверяется отдельно |
| R-4 | В БД накапливаются «удалённые» записи | низкая | низкое | Вне скоупа: очистка корзины отдельной фичей |
| R-5 | Пачка с несуществующим id | средняя | низкое | Удаляются только найденные, ответ `deleted: N`; неповлиявших не считаем ошибкой |

## 7. План тестирования

| Уровень | Что проверяем | Тест |
|---------|---------------|------|
| Unit (service) | `softDelete` ставит дату, 404 на отсутствии, `softDeleteAll` | `ProductServiceTest` |
| Web | DELETE 204, повторный DELETE 404, bulk-delete 200 + `deleted`, пустой `ids` → 400 | `ProductControllerTest` |
| Интеграционный | удалённая запись не видна в `findAll`, но остаётся в БД | `ProjectAApplicationTest` |
| Ручной | страница, чекбоксы, диалог, обновление обеих страниц | AC |

## 8. Трассировка требований

| Требование | Реализация | Тест |
|------------|------------|------|
| FR-1, FR-2 | `products-delete.html`, `products-delete.js` | AC-1 |
| FR-3, FR-4 | `products-delete.js` | AC-2 |
| FR-5 | модальный диалог в `products-delete.js` | AC-3 |
| FR-6 | `V3__add_products_deleted_at.sql`, `Product.markDeleted` | AC-4 |
| FR-7 | `ProductService.findAll` через `findAllByDeletedAtIsNull` | AC-5, интеграционный тест |
| FR-8 | `ProductController.delete` | AC-6 |
| FR-9 | `ProductController.bulkDelete` | AC-6 |
| FR-10 | `ResourceNotFoundException` → 404 | AC-7 |
| FR-11 | `products.html`, `index.html`, `products-delete.html` | ревью |
| NFR-4 | — | полный `mvnw test` |
