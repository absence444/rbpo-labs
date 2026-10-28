package ru.rbpo.museum.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {
    }

    /** Самостоятельная регистрация. Поля роли нет: новый пользователь всегда получает роль VISITOR. */
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_.-]+$") String username,
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password) {

        @Override
        public String toString() {
            return "RegisterRequest[username=" + username + ", password=***]";
        }
    }

    public record CsrfResponse(String headerName, String token) {
    }
}
