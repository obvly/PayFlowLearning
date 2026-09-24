package com.obvly.payflow.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotBlank
        @Size(max = 64)
        String orderId,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "[A-Z]{3}", message = "must contain three uppercase letters")
        String currency,

        @Size(max = 500)
        String description
) {
}
