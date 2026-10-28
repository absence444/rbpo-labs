package ru.rbpo.museum.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import ru.rbpo.museum.exception.BadRequestException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.Exhibition;
import ru.rbpo.museum.model.ExhibitionStatus;
import ru.rbpo.museum.repository.ExhibitPlacementRepository;
import ru.rbpo.museum.repository.ExhibitionRepository;
import ru.rbpo.museum.repository.UserRepository;

@Service
public class ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final UserRepository userRepository;
    private final ExhibitPlacementRepository placementRepository;
    private final Clock clock;

    public ExhibitionService(ExhibitionRepository exhibitionRepository, UserRepository userRepository,
                             ExhibitPlacementRepository placementRepository, Clock clock) {
        this.exhibitionRepository = exhibitionRepository;
        this.userRepository = userRepository;
        this.placementRepository = placementRepository;
        this.clock = clock;
    }

    public List<Exhibition> findAll() {
        return exhibitionRepository.findAll();
    }

    public Exhibition findById(Long id) {
        return exhibitionRepository.findById(id).orElseThrow(() -> new NotFoundException("Выставка", id));
    }

    public Exhibition create(Exhibition exhibition) {
        if (exhibition.getStatus() == null) {
            exhibition.setStatus(ExhibitionStatus.PLANNED);
        }
        ensureCuratorExists(exhibition.getCuratorId());
        ensureDatesValid(exhibition.getStartDate(), exhibition.getEndDate());
        Instant now = Instant.now(clock);
        exhibition.setId(null);
        exhibition.setCreatedAt(now);
        exhibition.setUpdatedAt(now);
        return exhibitionRepository.save(exhibition);
    }

    public Exhibition update(Long id, Exhibition changes) {
        Exhibition exhibition = findById(id);
        LocalDate startDate = changes.getStartDate() != null ? changes.getStartDate() : exhibition.getStartDate();
        LocalDate endDate = changes.getEndDate() != null ? changes.getEndDate() : exhibition.getEndDate();
        ensureDatesValid(startDate, endDate);
        if (changes.getCuratorId() != null) {
            ensureCuratorExists(changes.getCuratorId());
            exhibition.setCuratorId(changes.getCuratorId());
        }
        if (changes.getTitle() != null) {
            exhibition.setTitle(changes.getTitle());
        }
        if (changes.getStatus() != null) {
            exhibition.setStatus(changes.getStatus());
        }
        exhibition.setStartDate(startDate);
        exhibition.setEndDate(endDate);
        exhibition.setUpdatedAt(Instant.now(clock));
        return exhibitionRepository.save(exhibition);
    }

    /** Удаляет выставку вместе со всеми размещениями экспонатов на ней. */
    public void delete(Long id) {
        findById(id);
        placementRepository.findByExhibitionId(id).forEach(placement -> placementRepository.deleteById(placement.getId()));
        exhibitionRepository.deleteById(id);
    }

    private void ensureCuratorExists(Long curatorId) {
        if (curatorId != null && !userRepository.existsById(curatorId)) {
            throw new NotFoundException("Пользователь (куратор)", curatorId);
        }
    }

    private void ensureDatesValid(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("Дата окончания выставки не может быть раньше даты начала");
        }
    }
}
