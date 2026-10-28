-- Демонстрационные данные. Подключаются только в основной конфигурации (не в тестах).
INSERT INTO users (username, full_name, email, created_at, updated_at) VALUES
    ('a.petrova', 'Анна Петрова', 'a.petrova@museum.example', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('i.smirnov', 'Игорь Смирнов', 'i.smirnov@museum.example', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO exhibition_halls (name, area, available, created_at, updated_at) VALUES
    ('Белый зал', 120, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Малый зал', 15, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Зал на реконструкции', 200, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO exhibits (name, condition, required_area, created_at, updated_at) VALUES
    ('Портрет неизвестной', 'EXCELLENT', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Амфора с Боспора', 'GOOD', 2.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Икона «Спас»', 'NEEDS_RESTORATION', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Бронзовый всадник (модель)', 'SATISFACTORY', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO exhibitions (title, start_date, end_date, status, curator_id, created_at, updated_at) VALUES
    ('Искусство античности', CURRENT_DATE, CURRENT_DATE + 90, 'PLANNED',
     (SELECT id FROM users WHERE username = 'a.petrova'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Русская икона', CURRENT_DATE + 30, CURRENT_DATE + 120, 'PLANNED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO exhibit_placements (exhibit_id, exhibition_id, hall_id, mounting_status, created_at, updated_at) VALUES
    ((SELECT id FROM exhibits WHERE name = 'Амфора с Боспора'),
     (SELECT id FROM exhibitions WHERE title = 'Искусство античности'),
     (SELECT id FROM exhibition_halls WHERE name = 'Белый зал'), 'PLANNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ((SELECT id FROM exhibits WHERE name = 'Икона «Спас»'),
     (SELECT id FROM exhibitions WHERE title = 'Русская икона'),
     (SELECT id FROM exhibition_halls WHERE name = 'Малый зал'), 'PLANNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
