package com.obvly.payflow.payment.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency-Key has already been used with a different payment request");
    }
}
