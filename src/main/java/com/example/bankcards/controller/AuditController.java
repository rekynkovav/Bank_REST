package com.example.bankcards.controller;

import com.example.bankcards.entity.AuditLog;
import com.example.bankcards.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Просмотр журнала аудита")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Получить журнал аудита с фильтрацией")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @ParameterObject @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        Page<AuditLog> logs = auditService.searchAuditLogs(
                userId, action, entityType, startDate, endDate, pageable);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Получить аудит действий пользователя")
    public ResponseEntity<Page<AuditLog>> getUserAuditLogs(
            @PathVariable Long userId,
            @ParameterObject @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        Page<AuditLog> logs = auditService.getUserAuditLogs(userId, pageable);
        return ResponseEntity.ok(logs);
    }
}