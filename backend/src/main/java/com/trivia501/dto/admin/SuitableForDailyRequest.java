package com.trivia501.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SuitableForDailyRequest {
    @NotNull(message = "suitable field is required")
    private Boolean suitable;
}
