package com.obvly.payflow.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obvly.payflow.audit.model.AuditEvent;
import com.obvly.payflow.audit.service.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditEventController.class)
class AuditEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditEventService auditEventService;

    @Test
    void shouldReturnAllAuditEvents() throws Exception {
        UUID paymentId = UUID.randomUUID();
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                paymentId,
                "PAYMENT_COMPLETED",
                "COMPLETED",
                Instant.now()
        );

        when(auditEventService.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/audit-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentId")
                        .value(paymentId.toString()))
                .andExpect(jsonPath("$.content[0].eventType")
                        .value("PAYMENT_COMPLETED"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("COMPLETED"));
    }

    @Test
    void shouldReturnPaymentAuditHistory() throws Exception {
        UUID paymentId = UUID.randomUUID();
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                paymentId,
                "PAYMENT_PENDING",
                "PENDING",
                Instant.now()
        );

        when(auditEventService.findByPaymentId(paymentId))
                .thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/audit-events/payment/{paymentId}",
                        paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId")
                        .value(paymentId.toString()))
                .andExpect(jsonPath("$[0].status")
                        .value("PENDING"));
    }

    @Test
    void shouldReturnPagedAuditEvents() throws Exception {
        UUID paymentId = UUID.randomUUID();
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                paymentId,
                "PAYMENT_COMPLETED",
                "COMPLETED",
                Instant.now()
        );
        PageRequest pageRequest = PageRequest.of(
                1,
                2,
                Sort.by(Sort.Direction.DESC, "occurredAt")
        );

        when(auditEventService.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(event), pageRequest, 5));

        mockMvc.perform(get("/api/v1/audit-events")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "occurredAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void shouldFilterAuditEventsByStatus() throws Exception {
        UUID paymentId = UUID.randomUUID();
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                paymentId,
                "PAYMENT_COMPLETED",
                "COMPLETED",
                Instant.now()
        );

        when(auditEventService.findByStatus(eq("COMPLETED"), any()))
                .thenReturn(new PageImpl<>(List.of(event)));

        mockMvc.perform(get("/api/v1/audit-events")
                        .param("status", "completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status")
                        .value("COMPLETED"));
    }

    @Test
    void shouldReturnBadRequestForUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/v1/audit-events")
                        .param("status", "unknown"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(auditEventService);
    }
}
