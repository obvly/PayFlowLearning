package com.obvly.payflow.payment.exception;

public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
