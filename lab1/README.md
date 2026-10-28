# Лабораторная работа 1. Подготовка репозитория

Spring Boot 4.0.9, Maven, Java 21. `spring.application.name` = `24067`.

## Эндпоинты
- `GET /api/hello?name=Ivan` — текстовое приветствие.
- `GET /api/squares?count=5` — список квадратов чисел: `[1,4,9,16,25]`.

## Запуск
```bash
./mvnw spring-boot:run
./mvnw test
```
