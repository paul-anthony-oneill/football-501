package com.football501.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkCreateAnswersRequest {
    @NotEmpty(message = "Answers list cannot be empty")
    @Valid
    private List<CreateAnswerRequest> answers;
}
