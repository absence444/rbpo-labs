# Лабораторная работа 2. Вариант 9 — Музейные выставки

Spring Boot 4.0.9, Maven, Java 21. Данные хранятся в памяти (коллекции).

## Архитектура
- `controller` — обработка запросов и маппинг DTO ⇄ модель, обработчик ошибок;
- `service` — бизнес-логика и расчёты (проверка площади зала, каскадное удаление, время);
- `repository` — хранение и выборка данных;
- `dto` — запросы и ответы API, `model` — бизнес-сущности.

## Ресурсы
| Ресурс | URL | Поля |
|---|---|---|
| Экспонат | `/api/exhibits` | `name`, `condition` (EXCELLENT, GOOD, SATISFACTORY, NEEDS_RESTORATION), `requiredArea` |
| Выставка | `/api/exhibitions` | `title`, `startDate`, `endDate`, `status` (PLANNED, ACTIVE, COMPLETED, CANCELLED), `curatorId` |
| Экспозиционный зал | `/api/exhibition-halls` | `name`, `area`, `available` |
| Размещение экспоната | `/api/exhibit-placements` | `exhibitId`, `exhibitionId`, `hallId`, `mountingStatus` (PLANNED, IN_PROGRESS, MOUNTED, DISMANTLED) |
| Пользователь | `/api/users` | `username`, `fullName`, `email` |

Для каждого ресурса: `GET /api/{resources}`, `GET /api/{resources}/{id}`, `POST /api/{resources}`,
`PATCH /api/{resources}/{id}`, `DELETE /api/{resources}/{id}`.

## Ответы
- чтение — `200` и JSON ресурса/массива;
- создание — `201` и JSON созданного ресурса с `id`;
- обновление — `200` и JSON ресурса;
- удаление — `204`;
- ошибки — `400` (валидация), `404` (не найдено), `409` (нарушение бизнес-правил) в формате `application/problem+json`.

## Бизнес-правила
- `id`, `createdAt`, `updatedAt` задаёт сервер; эти поля из запроса игнорируются.
- Дата окончания выставки не раньше даты начала.
- Размещать экспонаты можно только в доступном зале; суммарная площадь экспонатов
  выставки в зале не превышает площадь зала; один экспонат размещается на выставке один раз.
- Удаление без недействительных ссылок: при удалении экспоната, выставки или зала удаляются
  связанные размещения; при удалении пользователя он снимается с роли куратора выставок.

## Пример
```bash
curl -X POST localhost:8080/api/exhibition-halls -H 'Content-Type: application/json' \
     -d '{"name": "Зал 1", "area": 120}'
curl -X POST localhost:8080/api/exhibits -H 'Content-Type: application/json' \
     -d '{"name": "Ваза эпохи Мин", "condition": "GOOD", "requiredArea": 2.5}'
```

## Запуск
```bash
./mvnw spring-boot:run
./mvnw test
```
