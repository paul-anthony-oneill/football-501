package com.football501.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;
}
