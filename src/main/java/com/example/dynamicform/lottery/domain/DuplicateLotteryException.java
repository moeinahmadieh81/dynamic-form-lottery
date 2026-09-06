package com.example.dynamicform.lottery.domain;

public class DuplicateLotteryException extends RuntimeException {
    public DuplicateLotteryException(Long formId) {
        super("A lottery already exists for form " + formId);
    }
}
