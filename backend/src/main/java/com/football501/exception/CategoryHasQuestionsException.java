package com.football501.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CategoryHasQuestionsException extends RuntimeException {
    public CategoryHasQuestionsException(String message) {
        super(message);
    }
}
