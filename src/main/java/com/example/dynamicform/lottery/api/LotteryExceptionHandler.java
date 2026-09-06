package com.example.dynamicform.lottery.api;

import com.example.dynamicform.lottery.domain.DuplicateLotteryException;
import com.example.dynamicform.lottery.domain.LotteryNotFoundException;
import com.example.dynamicform.lottery.domain.LotteryOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = LotteryController.class)
public class LotteryExceptionHandler {

    @ExceptionHandler(LotteryNotFoundException.class)
    ProblemDetail handleNotFound(LotteryNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Lottery not found");
        return problem;
    }

    @ExceptionHandler(DuplicateLotteryException.class)
    ProblemDetail handleDuplicate(DuplicateLotteryException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Lottery already exists");
        return problem;
    }

    @ExceptionHandler(LotteryOperationException.class)
    ProblemDetail handleOperation(LotteryOperationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Lottery operation not allowed");
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Lottery operation not allowed");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid lottery request");
        return problem;
    }
}
