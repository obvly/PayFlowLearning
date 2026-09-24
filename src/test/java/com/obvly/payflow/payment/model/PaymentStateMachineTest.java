package com.obvly.payflow.payment.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.obvly.payflow.payment.exception.InvalidPaymentStateException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PaymentStateMachineTest {

    private static Stream<Arguments> transitions() {
        return Arrays.stream(PaymentStatus.values()).flatMap(state ->
                Stream.of("process", "complete", "fail", "cancel", "refund")
                        .map(action -> Arguments.of(state, action)));
    }

    @ParameterizedTest
    @MethodSource("transitions")
    void shouldEnforceEveryStateActionPair(PaymentStatus state, String action) {
        Payment payment = paymentInState(state);
        PaymentStatus expected = switch (action) {
            case "process" -> state == PaymentStatus.PENDING ? PaymentStatus.PROCESSING : null;
            case "cancel" -> state == PaymentStatus.PENDING ? PaymentStatus.CANCELLED : null;
            case "complete" -> state == PaymentStatus.PROCESSING ? PaymentStatus.COMPLETED : null;
            case "fail" -> state == PaymentStatus.PROCESSING ? PaymentStatus.FAILED : null;
            case "refund" -> state == PaymentStatus.COMPLETED ? PaymentStatus.REFUNDED : null;
            default -> throw new IllegalArgumentException(action);
        };

        if (expected == null) {
            assertThatThrownBy(() -> apply(payment, action))
                    .isInstanceOf(InvalidPaymentStateException.class);
            assertThat(payment.getStatus()).isEqualTo(state);
        } else {
            apply(payment, action);
            assertThat(payment.getStatus()).isEqualTo(expected);
        }
    }

    private Payment paymentInState(PaymentStatus state) {
        Payment payment = new Payment("ORD", BigDecimal.ONE, "EUR", null);
        switch (state) {
            case PENDING -> { }
            case CANCELLED -> payment.cancel();
            case PROCESSING -> payment.startProcessing();
            case FAILED -> {
                payment.startProcessing();
                payment.fail();
            }
            case COMPLETED, REFUNDED -> {
                payment.startProcessing();
                payment.complete();
                if (state == PaymentStatus.REFUNDED) {
                    payment.refund();
                }
            }
            default -> throw new IllegalArgumentException(state.name());
        }
        return payment;
    }

    private void apply(Payment payment, String action) {
        switch (action) {
            case "process" -> payment.startProcessing();
            case "complete" -> payment.complete();
            case "fail" -> payment.fail();
            case "cancel" -> payment.cancel();
            case "refund" -> payment.refund();
            default -> throw new IllegalArgumentException(action);
        }
    }
}
