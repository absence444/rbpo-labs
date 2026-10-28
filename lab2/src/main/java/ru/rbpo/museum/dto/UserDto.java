package ru.rbpo.museum.dto;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDto {

    private UserDto() {
    }

    public record CreateRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_.-]+$") String username,
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 254) String email) {
    }

    public record PatchRequest(
            @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_.-]+$") String username,
            @Size(min = 1, max = 150) String fullName,
            @Email @Size(max = 254) String email) {
    }

    public record Response(Long id, String username, String fullName, String email,
                           Instant createdAt, Instant updatedAt) {
    }
}
