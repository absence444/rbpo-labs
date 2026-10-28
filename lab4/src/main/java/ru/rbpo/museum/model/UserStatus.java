package ru.rbpo.museum.model;

public enum UserStatus {
    /** Может входить в систему. */
    ACTIVE,
    /** Заблокирован администратором: вход запрещён. */
    BLOCKED
}
