package ru.rbpo.museum.dto;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import ru.rbpo.museum.model.Role;
import ru.rbpo.museum.model.UserStatus;

public final class UserDto {

    private UserDto() {
    }

    /** Создание учётной записи администратором (роль по умолчанию — VISITOR). */
    public record CreateRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_.-]+$") String username,
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password,
            Role role) {

        @Override
        public String toString() {
            return "CreateRequest[username=" + username + ", role=" + role + ", password=***]";
        }
    }

    /** Изменение учётной записи администратором; status=ACTIVE также снимает временную блокировку. */
    public record PatchRequest(
            @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_.-]+$") String username,
            @Size(min = 1, max = 150) String fullName,
            @Email @Size(max = 254) String email,
            @Size(min = 1, max = 128) String password,
            Role role,
            UserStatus status) {

        @Override
        public String toString() {
            return "PatchRequest[username=" + username + ", role=" + role + ", status=" + status + ", password=***]";
        }
    }

    /** Пароль и его хеш никогда не возвращаются клиенту. */
    public record Response(Long id, String username, String fullName, String email, Role role, UserStatus status,
                           Instant lockedUntil, Instant createdAt, Instant updatedAt) {
    }
}
