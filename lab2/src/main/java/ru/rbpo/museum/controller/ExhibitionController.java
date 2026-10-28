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

import ru.rbpo.museum.dto.ExhibitionDto;
import ru.rbpo.museum.model.Exhibition;
import ru.rbpo.museum.service.ExhibitionService;

@RestController
@RequestMapping("/api/exhibitions")
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    public ExhibitionController(ExhibitionService exhibitionService) {
        this.exhibitionService = exhibitionService;
    }

    @GetMapping
    public List<ExhibitionDto.Response> findAll() {
        return exhibitionService.findAll().stream().map(ExhibitionController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ExhibitionDto.Response findById(@PathVariable Long id) {
        return toResponse(exhibitionService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExhibitionDto.Response create(@Valid @RequestBody ExhibitionDto.CreateRequest request) {
        Exhibition exhibition = new Exhibition();
        exhibition.setTitle(request.title());
        exhibition.setStartDate(request.startDate());
        exhibition.setEndDate(request.endDate());
        exhibition.setStatus(request.status());
        exhibition.setCuratorId(request.curatorId());
        return toResponse(exhibitionService.create(exhibition));
    }

    @PatchMapping("/{id}")
    public ExhibitionDto.Response update(@PathVariable Long id,
                                         @Valid @RequestBody ExhibitionDto.PatchRequest request) {
        Exhibition changes = new Exhibition();
        changes.setTitle(request.title());
        changes.setStartDate(request.startDate());
        changes.setEndDate(request.endDate());
        changes.setStatus(request.status());
        changes.setCuratorId(request.curatorId());
        return toResponse(exhibitionService.update(id, changes));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        exhibitionService.delete(id);
    }

    private static ExhibitionDto.Response toResponse(Exhibition exhibition) {
        return new ExhibitionDto.Response(exhibition.getId(), exhibition.getTitle(), exhibition.getStartDate(),
                exhibition.getEndDate(), exhibition.getStatus(), exhibition.getCuratorId(),
                exhibition.getCreatedAt(), exhibition.getUpdatedAt());
    }
}
