package com.campus.learningspace.config;

import com.campus.learningspace.common.ReservationRuleException;
import com.campus.learningspace.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReservationRuleException.class)
    public ResponseEntity<Result<Void>> handleReservationRule(ReservationRuleException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(400, e.getMessage()));
    }
}
