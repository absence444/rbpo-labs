package ru.rbpo.museum.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Настройки безопасности.
 *
 * @param bcryptStrength    стоимость BCrypt (log2 числа раундов)
 * @param maxFailedAttempts число неудачных входов подряд до временной блокировки
 * @param lockDuration      длительность временной блокировки
 * @param admin             учётная запись первого администратора
 */
@ConfigurationProperties(prefix = "museum.security")
public record SecurityProperties(
        @DefaultValue("12") int bcryptStrength,
        @DefaultValue("5") int maxFailedAttempts,
        @DefaultValue("15m") Duration lockDuration,
        Admin admin) {

    public record Admin(String username, String email, String password) {
    }
}
