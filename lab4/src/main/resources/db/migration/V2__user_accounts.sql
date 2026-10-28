-- Учётные записи: пароль (BCrypt), роль, состояние и защита от подбора пароля.
ALTER TABLE users ADD COLUMN password_hash VARCHAR(100);
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'VISITOR';
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;

ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'CURATOR', 'VISITOR'));
ALTER TABLE users ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'BLOCKED'));
ALTER TABLE users ADD CONSTRAINT ck_users_failed_attempts CHECK (failed_login_attempts >= 0);
-- Хеш BCrypt всегда начинается с $2a$/$2b$/$2y$ и имеет длину 60 символов.
ALTER TABLE users ADD CONSTRAINT ck_users_password_hash
    CHECK (password_hash IS NULL OR (LENGTH(password_hash) = 60 AND password_hash LIKE '$2%'));
