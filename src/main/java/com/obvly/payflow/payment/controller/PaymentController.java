package com.obvly.payflow.payment.controller;

import com.obvly.payflow.payment.dto.CreatePaymentRequest;
import com.obvly.payflow.payment.dto.PaymentPageResponse;
import com.obvly.payflow.payment.dto.PaymentResponse;
import com.obvly.payflow.payment.model.Payment;
import com.obvly.payflow.payment.model.PaymentStatus;
import com.obvly.payflow.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id) {
        Payment payment = paymentService.getPayment(id);
        return PaymentResponse.from(payment);
    }

    @PostMapping
    @Operation(summary = "Create payment")
    @ApiResponse(responseCode = "201", description = "Payment created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must contain 1 to 100 characters and must not be blank"
            );
        }

        Payment payment = paymentService.createPayment(
                request.orderId(),
                request.amount(),
                request.currency(),
                request.description(),
                idempotencyKey
        );

        return PaymentResponse.from(payment);
    }

    @GetMapping
    public PaymentPageResponse getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<PaymentResponse> paymentPage = paymentService
                .getPayments(status, pageable)
                .map(PaymentResponse::from);

        return PaymentPageResponse.from(paymentPage);
    }

    @PostMapping("/{id}/process")
    public PaymentResponse startProcessing(@PathVariable UUID id) {
        return PaymentResponse.from(
                paymentService.startProcessing(id)
        );
    }

    @PostMapping("/{id}/complete")
    public PaymentResponse complete(@PathVariable UUID id) {
        return PaymentResponse.from(
                paymentService.complete(id)
        );
    }

    @PostMapping("/{id}/fail")
    public PaymentResponse fail(@PathVariable UUID id) {
        return PaymentResponse.from(
                paymentService.fail(id)
        );
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancel(@PathVariable UUID id) {
        return PaymentResponse.from(
                paymentService.cancel(id)
        );
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable UUID id) {
        return PaymentResponse.from(
                paymentService.refund(id)
        );
    }
}
