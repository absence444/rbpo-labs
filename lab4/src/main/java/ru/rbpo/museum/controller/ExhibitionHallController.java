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

import ru.rbpo.museum.dto.ExhibitionHallDto;
import ru.rbpo.museum.model.ExhibitionHall;
import ru.rbpo.museum.service.ExhibitionHallService;

@RestController
@RequestMapping("/api/exhibition-halls")
public class ExhibitionHallController {

    private final ExhibitionHallService hallService;

    public ExhibitionHallController(ExhibitionHallService hallService) {
        this.hallService = hallService;
    }

    @GetMapping
    public List<ExhibitionHallDto.Response> findAll() {
        return hallService.findAll().stream().map(ExhibitionHallController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ExhibitionHallDto.Response findById(@PathVariable Long id) {
        return toResponse(hallService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HALL_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ExhibitionHallDto.Response create(@Valid @RequestBody ExhibitionHallDto.CreateRequest request) {
        ExhibitionHall hall = new ExhibitionHall();
        hall.setName(request.name());
        hall.setArea(request.area());
        hall.setAvailable(request.available());
        return toResponse(hallService.create(hall));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('HALL_MANAGE')")
    public ExhibitionHallDto.Response update(@PathVariable Long id,
                                             @Valid @RequestBody ExhibitionHallDto.PatchRequest request) {
        ExhibitionHall changes = new ExhibitionHall();
        changes.setName(request.name());
        changes.setArea(request.area());
        changes.setAvailable(request.available());
        return toResponse(hallService.update(id, changes));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HALL_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hallService.delete(id);
    }

    private static ExhibitionHallDto.Response toResponse(ExhibitionHall hall) {
        return new ExhibitionHallDto.Response(hall.getId(), hall.getName(), hall.getArea(), hall.getAvailable(),
                hall.getCreatedAt(), hall.getUpdatedAt());
    }
}
