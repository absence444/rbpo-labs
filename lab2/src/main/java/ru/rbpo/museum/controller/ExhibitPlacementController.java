package ru.rbpo.museum.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.rbpo.museum.dto.ExhibitPlacementDto;
import ru.rbpo.museum.model.ExhibitPlacement;
import ru.rbpo.museum.service.ExhibitPlacementService;

@RestController
@RequestMapping("/api/exhibit-placements")
public class ExhibitPlacementController {

    private final ExhibitPlacementService placementService;

    public ExhibitPlacementController(ExhibitPlacementService placementService) {
        this.placementService = placementService;
    }

    @GetMapping
    public List<ExhibitPlacementDto.Response> findAll() {
        return placementService.findAll().stream().map(ExhibitPlacementController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ExhibitPlacementDto.Response findById(@PathVariable Long id) {
        return toResponse(placementService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExhibitPlacementDto.Response create(@Valid @RequestBody ExhibitPlacementDto.CreateRequest request) {
        ExhibitPlacement placement = new ExhibitPlacement();
        placement.setExhibitId(request.exhibitId());
        placement.setExhibitionId(request.exhibitionId());
        placement.setHallId(request.hallId());
        placement.setMountingStatus(request.mountingStatus());
        return toResponse(placementService.create(placement));
    }

    @PatchMapping("/{id}")
    public ExhibitPlacementDto.Response update(@PathVariable Long id,
                                               @Valid @RequestBody ExhibitPlacementDto.PatchRequest request) {
        ExhibitPlacement changes = new ExhibitPlacement();
        changes.setExhibitId(request.exhibitId());
        changes.setExhibitionId(request.exhibitionId());
        changes.setHallId(request.hallId());
        changes.setMountingStatus(request.mountingStatus());
        return toResponse(placementService.update(id, changes));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        placementService.delete(id);
    }

    private static ExhibitPlacementDto.Response toResponse(ExhibitPlacement placement) {
        return new ExhibitPlacementDto.Response(placement.getId(), placement.getExhibitId(),
                placement.getExhibitionId(), placement.getHallId(), placement.getMountingStatus(),
                placement.getCreatedAt(), placement.getUpdatedAt());
    }
}
