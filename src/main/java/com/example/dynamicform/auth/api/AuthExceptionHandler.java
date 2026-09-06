package com.example.dynamicform.auth.api;

import com.example.dynamicform.auth.domain.EmailAlreadyExistsException;
import com.example.dynamicform.auth.domain.InvalidCredentialsException;
import com.example.dynamicform.auth.domain.UserAccessDeniedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Email already registered");
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Authentication failed");
        return problem;
    }

    @ExceptionHandler(UserAccessDeniedException.class)
    ProblemDetail handleUserAccessDenied(UserAccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Access denied");
        return problem;
    }
}
