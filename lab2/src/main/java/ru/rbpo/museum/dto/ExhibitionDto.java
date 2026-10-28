package ru.rbpo.museum.dto;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import ru.rbpo.museum.model.ExhibitionStatus;

public final class ExhibitionDto {

    private ExhibitionDto() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            ExhibitionStatus status,
            @Positive Long curatorId) {
    }

    public record PatchRequest(
            @Size(min = 1, max = 200) String title,
            LocalDate startDate,
            LocalDate endDate,
            ExhibitionStatus status,
            @Positive Long curatorId) {
    }

    public record Response(Long id, String title, LocalDate startDate, LocalDate endDate,
                           ExhibitionStatus status, Long curatorId, Instant createdAt, Instant updatedAt) {
    }
}
