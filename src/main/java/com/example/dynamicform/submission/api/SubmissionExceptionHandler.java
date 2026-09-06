package com.example.dynamicform.submission.api;

import com.example.dynamicform.submission.domain.DuplicateSubmissionException;
import com.example.dynamicform.submission.domain.SubmissionNotAllowedException;
import com.example.dynamicform.submission.domain.SubmissionValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SubmissionExceptionHandler {

    @ExceptionHandler(SubmissionValidationException.class)
    ProblemDetail handleValidation(SubmissionValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Invalid submission");
        problem.setProperty("errors", ex.getErrors());
        return problem;
    }

    @ExceptionHandler(DuplicateSubmissionException.class)
    ProblemDetail handleDuplicate(DuplicateSubmissionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Duplicate submission");
        return problem;
    }

    @ExceptionHandler(SubmissionNotAllowedException.class)
    ProblemDetail handleNotAllowed(SubmissionNotAllowedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Submission not allowed");
        return problem;
    }
}
