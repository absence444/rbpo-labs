package ru.rbpo.museum.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.rbpo.museum.model.Role;
import ru.rbpo.museum.repository.ExhibitPlacementRepository;
import ru.rbpo.museum.repository.ExhibitionRepository;

/**
 * Контекстные правила доступа для {@code @PreAuthorize}: куратор управляет только теми выставками
 * (и размещениями на них), куратором которых он назначен.
 */
@Component("access")
@Transactional(readOnly = true)
public class AccessPolicy {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitPlacementRepository placementRepository;

    public AccessPolicy(ExhibitionRepository exhibitionRepository, ExhibitPlacementRepository placementRepository) {
        this.exhibitionRepository = exhibitionRepository;
        this.placementRepository = placementRepository;
    }

    /** Текущий пользователь — куратор указанной выставки. */
    public boolean isCuratorOfExhibition(Authentication authentication, Long exhibitionId) {
        Long userId = curatorId(authentication);
        return userId != null && exhibitionId != null
                && exhibitionRepository.existsByIdAndCuratorId(exhibitionId, userId);
    }

    /** Текущий пользователь — куратор выставки, к которой относится размещение. */
    public boolean isCuratorOfPlacement(Authentication authentication, Long placementId) {
        Long userId = curatorId(authentication);
        return userId != null && placementId != null
                && placementRepository.existsByIdAndExhibitionCuratorId(placementId, userId);
    }

    /** Куратор может назначить куратором выставки только себя (или не указывать куратора). */
    public boolean isSelfOrUnset(Authentication authentication, Long curatorId) {
        return curatorId == null
                || authentication.getPrincipal() instanceof SecurityUser user && curatorId.equals(user.getId());
    }

    private static Long curatorId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser user
                && user.getRole() == Role.CURATOR) {
            return user.getId();
        }
        return null;
    }
}
