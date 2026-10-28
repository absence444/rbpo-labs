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

import ru.rbpo.museum.dto.ExhibitDto;
import ru.rbpo.museum.model.Exhibit;
import ru.rbpo.museum.service.ExhibitService;

@RestController
@RequestMapping("/api/exhibits")
public class ExhibitController {

    private final ExhibitService exhibitService;

    public ExhibitController(ExhibitService exhibitService) {
        this.exhibitService = exhibitService;
    }

    @GetMapping
    public List<ExhibitDto.Response> findAll() {
        return exhibitService.findAll().stream().map(ExhibitController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ExhibitDto.Response findById(@PathVariable Long id) {
        return toResponse(exhibitService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExhibitDto.Response create(@Valid @RequestBody ExhibitDto.CreateRequest request) {
        Exhibit exhibit = new Exhibit();
        exhibit.setName(request.name());
        exhibit.setCondition(request.condition());
        exhibit.setRequiredArea(request.requiredArea());
        return toResponse(exhibitService.create(exhibit));
    }

    @PatchMapping("/{id}")
    public ExhibitDto.Response update(@PathVariable Long id, @Valid @RequestBody ExhibitDto.PatchRequest request) {
        Exhibit changes = new Exhibit();
        changes.setName(request.name());
        changes.setCondition(request.condition());
        changes.setRequiredArea(request.requiredArea());
        return toResponse(exhibitService.update(id, changes));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        exhibitService.delete(id);
    }

    /** Бизнес-операция: отправить экспонат на реставрацию. */
    @PostMapping("/{id}/send-to-restoration")
    public ExhibitDto.Response sendToRestoration(@PathVariable Long id) {
        return toResponse(exhibitService.sendToRestoration(id));
    }

    private static ExhibitDto.Response toResponse(Exhibit exhibit) {
        return new ExhibitDto.Response(exhibit.getId(), exhibit.getName(), exhibit.getCondition(),
                exhibit.getRequiredArea(), exhibit.getCreatedAt(), exhibit.getUpdatedAt());
    }
}
