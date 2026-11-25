# Tasks: Страница удаления товаров (мягкое удаление)

**Спека:** [spec.md](./spec.md) · **План:** [plan.md](./plan.md)

## Фаза 1. Данные

- [x] **T-1** (FR-6) Миграция `V3__add_products_deleted_at.sql`
  - файлы: `src/main/resources/db/migration/V3__add_products_deleted_at.sql`
  - проверка: `flyway_schema_history` v3 `add products deleted at` success=t
- [x] **T-2** (FR-6) Поле `deletedAt` и `markDeleted()` в `Product`
  - файлы: `domain/Product.java`
- [x] **T-3** (FR-7) Методы репозитория с фильтром `deletedAt IS NULL`
  - файлы: `repository/ProductRepository.java`
  - методы: `findAllByDeletedAtIsNull`, `findByIdAndDeletedAtIsNull`, `findAllByIdInAndDeletedAtIsNull`

## Фаза 2. API

- [x] **T-4** (FR-6, FR-7, FR-10) `ProductService.softDelete`, `softDeleteAll`, фильтрация `findAll`
  - файлы: `service/ProductService.java`
  - тесты: `ProductServiceTest` — 7 тестов
- [x] **T-5** (FR-8, FR-9) `DELETE /{id}` и `POST /bulk-delete` + DTO
  - файлы: `web/ProductController.java`, `web/dto/BulkDeleteRequest.java`, `BulkDeleteResponse.java`
  - тесты: `ProductControllerTest` — 18 тестов

## Фаза 3. Интерфейс

- [x] **T-6** (FR-1, FR-2, FR-3) `products-delete.html`: таблица с чекбоксами и кнопкой в строке
  - файлы: `static/products-delete.html`
- [x] **T-7** (FR-4, FR-5, FR-10) `products-delete.js`: выбор, диалог подтверждения, удаление, обновление
  - файлы: `static/js/products-delete.js`
  - проверка: выбор 2 из 4 → «Удалить выбранные (2)», шапка в состоянии indeterminate
- [x] **T-8** (FR-11) Навигация на трёх страницах
  - файлы: `static/index.html`, `static/products.html`, `static/products-delete.html`
- [x] **T-9** (NFR-2) Стили чекбоксов, кнопки в строке, диалога
  - файлы: `static/css/styles.css`

## Фаза 4. Верификация

- [x] **T-10** (AC-1…AC-7) Проверка в браузере на реальной PostgreSQL
  - одиночное: 4 → 3 записи, DELETE 204
  - пачечное: выбраны все 4 → «Удалено записей: 4», пустое состояние
  - отмена диалога: ничего не изменено
  - повторный DELETE id=1 → 404 `NOT_FOUND`
- [x] **T-11** (AC-4, AC-5) Интеграционный тест: `softDeletedProductIsHiddenButStillInDatabase`, `bulkSoftDeleteHidesAllAndKeepsRows`
- [x] **T-12** (AC-8) `mvnw test` — 43 теста, 0 падений
- [x] **T-13** README: новая страница и API удаления

## Отклонения от плана

| Задача | Что изменилось | Причина |
|--------|----------------|---------|
| T-7 | `setStatus('')` убран из `load()` | Найдено при проверке: сообщение «Удалено записей: N» стиралось вызовом `load()` сразу после установки |
| T-7 | `load()` вызывается до `setStatus` в ветке 404 | По той же причине: обновление списка перезаписывало текст об ошибке |

## Итог

- Дата завершения: 2026-11-25
- Коммиты: см. историю git
- В БД: 6 записей с заполненным `deleted_at` (проверка мягкого удаления), 4 активные демо-записи
- Незакрытые вопросы: восстановление из корзины и её очистка — вне скоупа
