package com.example.bankcards.controller;

import com.example.bankcards.entity.AuditLog;
import com.example.bankcards.config.JwtAuthenticationFilter;
import com.example.bankcards.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@WebMvcTest(
        controllers = AuditController.class,
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false) // Отключаем все фильтры безопасности
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    void getAuditLogs_Success() throws Exception {
        // Arrange
        AuditLog log1 = AuditLog.builder()
                .id(1L)
                .action("CREATE")
                .entityType("CARD")
                .createdAt(LocalDateTime.now())
                .build();

        AuditLog log2 = AuditLog.builder()
                .id(2L)
                .action("UPDATE")
                .entityType("USER")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(log1, log2));

        when(auditService.searchAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit")
                        .param("userId", "1")
                        .param("action", "CREATE")
                        .param("startDate", "2024-01-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"))
                .andExpect(jsonPath("$.content[1].entityType").value("USER"));
    }

    @Test
    void getUserAuditLogs_Success() throws Exception {
        // Arrange
        AuditLog log = AuditLog.builder()
                .id(1L)
                .action("LOGIN")
                .entityType("USER")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(log));

        when(auditService.getUserAuditLogs(anyLong(), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit/user/1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].action").value("LOGIN"))
                .andExpect(jsonPath("$.content[0].entityType").value("USER"));
    }
}