package ru.rbpo.museum.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('EXHIBITION_MANAGE_ANY') or @access.isCuratorOfExhibition(authentication, #request.exhibitionId())")
    @ResponseStatus(HttpStatus.CREATED)
    public ExhibitPlacementDto.Response create(@Valid @RequestBody ExhibitPlacementDto.CreateRequest request) {
        return toResponse(placementService.create(request.exhibitId(), request.exhibitionId(), request.hallId()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('EXHIBITION_MANAGE_ANY') or (@access.isCuratorOfPlacement(authentication, #id)"
            + " and (#request.exhibitionId() == null or @access.isCuratorOfExhibition(authentication, #request.exhibitionId())))")
    public ExhibitPlacementDto.Response update(@PathVariable Long id,
                                               @Valid @RequestBody ExhibitPlacementDto.PatchRequest request) {
        return toResponse(placementService.update(id, request.exhibitId(), request.exhibitionId(), request.hallId(),
                request.mountingStatus()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EXHIBITION_MANAGE_ANY') or @access.isCuratorOfPlacement(authentication, #id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        placementService.delete(id);
    }

    /** Бизнес-операция: смонтировать экспонат в зале. */
    @PostMapping("/{id}/mount")
    @PreAuthorize("hasAuthority('EXHIBITION_MANAGE_ANY') or @access.isCuratorOfPlacement(authentication, #id)")
    public ExhibitPlacementDto.Response mount(@PathVariable Long id) {
        return toResponse(placementService.mount(id));
    }

    /** Бизнес-операция: перенести экспонат в другой зал. */
    @PostMapping("/{id}/move")
    @PreAuthorize("hasAuthority('EXHIBITION_MANAGE_ANY') or @access.isCuratorOfPlacement(authentication, #id)")
    public ExhibitPlacementDto.Response move(@PathVariable Long id,
                                             @Valid @RequestBody ExhibitPlacementDto.MoveRequest request) {
        return toResponse(placementService.move(id, request.hallId()));
    }

    private static ExhibitPlacementDto.Response toResponse(ExhibitPlacement placement) {
        return new ExhibitPlacementDto.Response(placement.getId(), placement.getExhibit().getId(),
                placement.getExhibition().getId(), placement.getHall().getId(), placement.getMountingStatus(),
                placement.getMountedAt(), placement.getCreatedAt(), placement.getUpdatedAt());
    }
}
