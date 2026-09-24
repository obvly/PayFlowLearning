package com.obvly.payflow.audit.controller;

import com.obvly.payflow.audit.dto.AuditEventPageResponse;
import com.obvly.payflow.audit.dto.AuditEventResponse;
import com.obvly.payflow.audit.service.AuditEventService;
import com.obvly.payflow.payment.model.PaymentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping
    public ResponseEntity<AuditEventPageResponse> findAll(
            @PageableDefault(
                    size = 20,
                    sort = "occurredAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,
            @RequestParam(required = false) String status
    ) {
        String normalizedStatus = normalizeStatus(status);
        Page<AuditEventResponse> page = (normalizedStatus == null
                ? auditEventService.findAll(pageable)
                : auditEventService.findByStatus(normalizedStatus, pageable))
                .map(AuditEventResponse::from);

        AuditEventPageResponse response = new AuditEventPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return PaymentStatus.valueOf(status.toUpperCase()).name();
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<AuditEventResponse>> findByPaymentId(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) String status
    ) {
        String normalizedStatus = normalizeStatus(status);
        List<AuditEventResponse> response = (normalizedStatus == null
                ? auditEventService.findByPaymentId(paymentId)
                : auditEventService.findByPaymentIdAndStatus(
                        paymentId, normalizedStatus))
                .stream()
                .map(AuditEventResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}
