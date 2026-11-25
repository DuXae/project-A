# project-A

Проект ведётся по Spec Driven Development — см. [AGENTS.md](./AGENTS.md).

## Быстрый старт с новой фичей

1. Скопировать шаблон:
   ```powershell
   Copy-Item -Recurse specs\000-template specs\<feature-name>
   ```
2. Заполнить `spec.md` (ЧТО и ЗАЧЕМ), согласовать с заказчиком,
   поставить статус `approved`.
3. Заполнить `plan.md` (КАК) и `tasks.md` (ШАГИ).
4. Реализовывать по задачам, отмечая чекбоксы и фиксируя отклонения.

## Структура

```
AGENTS.md            правила процесса
specs/
  000-template/      шаблоны spec / plan / tasks
```

## Реестр фич

| Фича | Статус | Спека |
|------|--------|-------|
| —    | —      | —     |
