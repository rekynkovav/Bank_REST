package com.example.bankcards.service;

import com.example.bankcards.entity.AuditLog;
import com.example.bankcards.entity.CustomUserDetails;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.AuditLogRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, Long entityId, String details) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = null;

            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                var userDetails = (CustomUserDetails) authentication.getPrincipal();
                user = userRepository.findById(userDetails.getId()).orElse(null);
            }

            String ipAddress = getClientIp();
            String userAgent = getUserAgent();

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .user(user)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} - {} - {}", action, entityType, entityId);

        } catch (Exception e) {
            log.error("Failed to save audit log: {} - {}", action, entityType, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, Long entityId) {
        logAction(action, entityType, entityId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String details) {
        logAction(action, null, null, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> searchAuditLogs(
            Long userId,
            String action,
            String entityType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        if (startDate == null) {
            startDate = endDate.minusMonths(1);
        }

        return auditLogRepository.searchLogs(
                userId, action, entityType, startDate, endDate, pageable);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }

                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("Could not get client IP", e);
        }
        return "unknown";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.warn("Could not get user agent", e);
        }
        return "unknown";
    }

    public static class Actions {
        public static final String USER_REGISTERED = "USER_REGISTERED";
        public static final String USER_LOGIN = "USER_LOGIN";
        public static final String USER_LOGOUT = "USER_LOGOUT";
        public static final String CARD_CREATED = "CARD_CREATED";
        public static final String CARD_BLOCKED = "CARD_BLOCKED";
        public static final String CARD_ACTIVATED = "CARD_ACTIVATED";
        public static final String CARD_DELETED = "CARD_DELETED";
        public static final String TRANSFER_COMPLETED = "TRANSFER_COMPLETED";
        public static final String TRANSFER_FAILED = "TRANSFER_FAILED";
        public static final String BALANCE_CHECKED = "BALANCE_CHECKED";
        public static final String PROFILE_UPDATED = "PROFILE_UPDATED";
        public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
        public static final String ADMIN_ACTION = "ADMIN_ACTION";
    }

    public static class EntityTypes {
        public static final String USER = "User";
        public static final String BANK_CARD = "BankCard";
        public static final String CARD_TRANSACTION = "CardTransaction";
        public static final String AUDIT_LOG = "AuditLog";
    }
}