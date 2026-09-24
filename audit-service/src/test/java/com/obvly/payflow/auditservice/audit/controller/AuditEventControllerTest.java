package com.obvly.payflow.auditservice.audit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.obvly.payflow.auditservice.audit.service.AuditEventService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditEventController.class)
class AuditEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditEventService auditEventService;

    @Test
    void shouldRejectUnknownStatusOnBothEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/audit-events").param("status", "unknown"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/audit-events/payment/{id}", UUID.randomUUID())
                        .param("status", "unknown"))
                .andExpect(status().isBadRequest());
    }
}
