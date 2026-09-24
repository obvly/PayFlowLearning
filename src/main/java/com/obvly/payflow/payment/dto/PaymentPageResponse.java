package com.obvly.payflow.payment.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaymentPageResponse(
        List<PaymentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static PaymentPageResponse from(Page<PaymentResponse> paymentPage) {
        return new PaymentPageResponse(
                paymentPage.getContent(),
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements(),
                paymentPage.getTotalPages()
        );
    }
}
