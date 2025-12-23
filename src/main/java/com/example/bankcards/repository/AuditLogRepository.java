package com.example.bankcards.repository;

import com.example.bankcards.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end")
    Page<AuditLog> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId")
    Page<AuditLog> findByEntity(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.user.id = :userId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(a.createdAt BETWEEN :startDate AND :endDate)")
    Page<AuditLog> searchLogs(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}