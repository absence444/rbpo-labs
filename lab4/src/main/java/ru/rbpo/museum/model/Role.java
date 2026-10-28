package ru.rbpo.museum.model;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    /** Администратор музея: полный доступ. */
    ADMIN(EnumSet.allOf(Permission.class)),
    /** Куратор: ведёт фонд экспонатов и управляет своими выставками. */
    CURATOR(EnumSet.of(Permission.EXHIBIT_MANAGE, Permission.EXHIBITION_CREATE)),
    /** Посетитель (роль по умолчанию при регистрации): только просмотр. */
    VISITOR(EnumSet.noneOf(Permission.class));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}
