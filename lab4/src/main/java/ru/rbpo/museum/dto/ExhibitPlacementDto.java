package ru.rbpo.museum.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import ru.rbpo.museum.model.MountingStatus;

public final class ExhibitPlacementDto {

    private ExhibitPlacementDto() {
    }

    /** Новое размещение всегда создаётся в статусе PLANNED. */
    public record CreateRequest(
            @NotNull @Positive Long exhibitId,
            @NotNull @Positive Long exhibitionId,
            @NotNull @Positive Long hallId) {
    }

    public record PatchRequest(
            @Positive Long exhibitId,
            @Positive Long exhibitionId,
            @Positive Long hallId,
            MountingStatus mountingStatus) {
    }

    public record MoveRequest(@NotNull @Positive Long hallId) {
    }

    public record Response(Long id, Long exhibitId, Long exhibitionId, Long hallId,
                           MountingStatus mountingStatus, Instant mountedAt, Instant createdAt, Instant updatedAt) {
    }
}
