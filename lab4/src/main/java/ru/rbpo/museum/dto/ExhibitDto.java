package ru.rbpo.museum.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import ru.rbpo.museum.model.ExhibitCondition;

public final class ExhibitDto {

    private ExhibitDto() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull ExhibitCondition condition,
            @NotNull @Positive Double requiredArea) {
    }

    public record PatchRequest(
            @Size(min = 1, max = 200) String name,
            ExhibitCondition condition,
            @Positive Double requiredArea) {
    }

    public record Response(Long id, String name, ExhibitCondition condition, Double requiredArea,
                           Instant createdAt, Instant updatedAt) {
    }
}
