# Plan: Страница со списком товаров из БД

**Спека:** [spec.md](./spec.md)
**Дата:** 2026-11-25

## 1. Подход

| Вариант | Плюсы | Минусы | Решение |
|---------|-------|--------|---------|
| Отдельный `products.html` + общий CSS, данные через существующий `GET /api/v1/products` | Нет новых эндпоинтов, переиспользование стилей | Дублирование навигации в двух HTML | **выбрано** |
| Server-side Thymeleaf-шаблон | Один рендер, нет fetch | Противоречит решению «только JSON API», новая зависимость | отклонено |
| SPA-роутинг на одной странице | Один файл | Усложняет без сборщика, ломает прямые ссылки | отклонено |

Итог: статическая страница `products.html` + `js/products.js`, данные из
существующего API. Бэкенд не меняется, кроме увеличения допустимого `size`.

## 2. Архитектура

```
/products.html  (View)
      |  fetch GET /api/v1/products?size=10000
      v
ProductController.list -> ProductService.findAll -> PostgreSQL
      |
      v
PageResponse<ProductResponse>  ->  рендер строк таблицы
```

### Лимит размера страницы

Spring Data по умолчанию ограничивает `size` значением 2000
(`PageableHandlerMethodArgumentResolver#setMaxPageSize`). Для «всех сразу»
поднимаем лимит до 10000 в конфигурации MVC, не меняя контроллер.

```java
// config/WebConfig.java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setMaxPageSize(10_000);
        resolvers.add(resolver);
    }
}
```

Альтернатива — новый эндпоинт `/api/v1/products/all` без пагинации. Отклонено:
дублирует существующий контракт, а `PageResponse` уже отдаёт `totalItems`,
нужный для FR-4.

## 3. Структура файлов

```
src/main/java/com/duxae/projecta/config/WebConfig.java     (новый)
src/main/resources/static/
  products.html                                            (новый)
  js/products.js                                           (новый)
  css/styles.css                                           (изменить: nav, .meta, states)
  index.html                                               (изменить: ссылка на список)
src/test/java/com/duxae/projecta/web/ProductControllerTest.java  (дополнить)
```

## 4. Модель данных

Схема БД не меняется. Используются поля `ProductResponse`:
`id`, `name`, `quantity`, `createdAt`.

## 5. Зависимости

Новых нет.

## 6. Риски и допущения

| #   | Риск | Вероятность | Влияние | Митигация |
|-----|------|-------------|---------|-----------|
| R-1 | 10 000 записей тяжёлым JSON подвесят браузер | низкая | среднее | Ограничение 10 000, рендер через `DocumentFragment`, NFR-3 проверяется на 1000 записей |
| R-2 | Часовой пояс: `createdAt` в UTC, пользователь ждёт локальное время | высокая | низкое | `toLocaleString('ru-RU')` на клиенте |
| R-3 | Дублирование навигации в двух HTML | высокая | низкое | Навигация — 2 ссылки, без шаблонизатора; вынесем при появлении третьей страницы |
| R-4 | Изменение конфигурации `Pageable` влияет на другие эндпоинты | средняя | низкое | Лимит только увеличивается; тест на существующий `size` |

## 7. План тестирования

| Уровень | Что проверяем | Тест |
|---------|---------------|------|
| Web | `GET /products.html` отдаётся (200) | `ProductControllerTest` |
| Web | `size=5000` не обрезается до 2000 | `ProductControllerTest` |
| Ручной | 4 столбца, прокрутка, «Всего: N», пустое состояние, дата | браузер + AC |

## 8. Трассировка требований

| Требование | Реализация | Тест |
|------------|------------|------|
| FR-1 | `static/products.html` | AC-1 |
| FR-2 | `products.html` (thead) | AC-1 |
| FR-3 | `styles.css` (`.table-wrap--tall`), `products.js` | AC-2 |
| FR-4 | `products.js` (totalItems) | AC-3 |
| FR-5 | `products.js` (empty state) | AC-4 |
| FR-6 | `products.js` (error state + retry) | AC-5 |
| FR-7 | `index.html`, `products.html` (nav) | AC-6 |
| FR-8 | `products.js` (`formatDate`) | AC-7 |
| FR-9 | страница без кнопок действий | AC-1 |
| NFR-2 | `GET /api/v1/products?size=10000` | `ProductControllerTest` |
| NFR-3 | `DocumentFragment`, лимит 10 000 | ручная проверка на 1000 |
