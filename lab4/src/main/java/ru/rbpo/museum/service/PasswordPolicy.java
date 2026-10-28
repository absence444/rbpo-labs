package ru.rbpo.museum.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

/** Требования к надёжности пароля. */
@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    /** BCrypt учитывает только первые 72 байта пароля. */
    public static final int MAX_BYTES = 72;

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password1", "password123", "qwerty", "qwerty123", "qwertyuiop", "123456789",
            "1234567890", "12345678", "111111", "iloveyou", "admin", "administrator", "welcome", "letmein",
            "museum", "p@ssw0rd", "passw0rd");

    /** Возвращает список нарушений; пустой список — пароль подходит. */
    public List<String> validate(String password, String username, String email) {
        List<String> violations = new ArrayList<>();
        if (password == null || password.isBlank()) {
            violations.add("Пароль обязателен");
            return violations;
        }
        if (password.length() < MIN_LENGTH) {
            violations.add("Не короче " + MIN_LENGTH + " символов");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            violations.add("Не длиннее " + MAX_BYTES + " байт");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            violations.add("Хотя бы одна заглавная буква");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            violations.add("Хотя бы одна строчная буква");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            violations.add("Хотя бы одна цифра");
        }
        if (password.chars().allMatch(Character::isLetterOrDigit)) {
            violations.add("Хотя бы один спецсимвол");
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            violations.add("Без пробельных символов");
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (COMMON_PASSWORDS.stream().anyMatch(lower::contains)) {
            violations.add("Не должен содержать распространённые пароли");
        }
        if (username != null && username.length() >= 3 && lower.contains(username.toLowerCase(Locale.ROOT))) {
            violations.add("Не должен содержать имя пользователя");
        }
        if (email != null) {
            String local = email.toLowerCase(Locale.ROOT).split("@")[0];
            if (local.length() >= 3 && lower.contains(local)) {
                violations.add("Не должен содержать email");
            }
        }
        return violations;
    }
}
