package com.obvly.payflow.payment.exception;

import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handlePaymentNotFound(PaymentNotFoundException exception) {
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "PAYMENT_NOT_FOUND",
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidPaymentState(
            InvalidPaymentStateException exception
    ) {
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "INVALID_PAYMENT_STATE",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_REQUEST",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIdempotencyConflict(IdempotencyConflictException exception) {
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "IDEMPOTENCY_CONFLICT",
                exception.getMessage()
        );
    }

    public record ErrorResponse(
            Instant timestamp,
            int status,
            String code,
            String message
    ) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationError(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                message
        );
    }
}
