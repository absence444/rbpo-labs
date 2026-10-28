package ru.rbpo.museum.service;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.rbpo.museum.exception.ConflictException;
import ru.rbpo.museum.exception.NotFoundException;
import ru.rbpo.museum.model.Exhibition;
import ru.rbpo.museum.model.ExhibitionHall;
import ru.rbpo.museum.model.ExhibitionStatus;
import ru.rbpo.museum.repository.ExhibitPlacementRepository;
import ru.rbpo.museum.repository.ExhibitionHallRepository;

/**
 * Расчёт занятости залов. Зал одновременно используют все незавершённые выставки,
 * чьи периоды проведения пересекаются, поэтому их экспонаты суммарно не должны превышать площадь зала.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class HallCapacityService {

    static final Set<ExhibitionStatus> OCCUPYING_STATUSES = EnumSet.of(ExhibitionStatus.PLANNED, ExhibitionStatus.ACTIVE);
    private static final long NO_PLACEMENT = -1L;

    private final ExhibitionHallRepository hallRepository;
    private final ExhibitPlacementRepository placementRepository;

    public HallCapacityService(ExhibitionHallRepository hallRepository,
                               ExhibitPlacementRepository placementRepository) {
        this.hallRepository = hallRepository;
        this.placementRepository = placementRepository;
    }

    /** Блокирует строку зала до конца транзакции, чтобы параллельные операции не превысили его площадь. */
    public ExhibitionHall lockHall(Long hallId) {
        return hallRepository.findByIdForUpdate(hallId)
                .orElseThrow(() -> new NotFoundException("Экспозиционный зал", hallId));
    }

    public double occupiedArea(ExhibitionHall hall, Exhibition exhibition, Long excludePlacementId) {
        return placementRepository.sumOccupiedArea(hall.getId(), exhibition.getStartDate(), exhibition.getEndDate(),
                OCCUPYING_STATUSES, excludePlacementId != null ? excludePlacementId : NO_PLACEMENT);
    }

    /** Проверяет, что в зале хватит места для экспоната площадью {@code requiredArea} на время выставки. */
    public void ensureCapacity(ExhibitionHall hall, Exhibition exhibition, double requiredArea,
                               Long excludePlacementId) {
        double occupied = occupiedArea(hall, exhibition, excludePlacementId);
        if (occupied + requiredArea > hall.getArea()) {
            throw new ConflictException("Недостаточно площади в зале '" + hall.getName() + "' на период выставки: свободно "
                    + format(hall.getArea() - occupied) + " м², требуется " + format(requiredArea) + " м²");
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", Math.max(value, 0));
    }
}
