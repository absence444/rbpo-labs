package ru.rbpo.museum.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class ExhibitionHallDto {

    private ExhibitionHallDto() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull @Positive Double area,
            Boolean available) {
    }

    public record PatchRequest(
            @Size(min = 1, max = 200) String name,
            @Positive Double area,
            Boolean available) {
    }

    public record Response(Long id, String name, Double area, Boolean available,
                           Instant createdAt, Instant updatedAt) {
    }
}
