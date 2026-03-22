package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.AuditLogRepository;
import de.elyseeevents.portal.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(String action, String entityType, Long entityId, String details) {
        Long userId = getCurrentUserId();
        auditLogRepository.log(userId, action, entityType, entityId, details);
    }

    public void log(String action) {
        log(action, null, null, null);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userRepository.findByEmail(auth.getName())
                .map(User::getId)
                .orElse(null);
    }
}
