package ru.rbpo.museum.exception;

import java.util.List;

public class WeakPasswordException extends RuntimeException {

    private final List<String> violations;

    public WeakPasswordException(List<String> violations) {
        super("Пароль не соответствует требованиям безопасности");
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
