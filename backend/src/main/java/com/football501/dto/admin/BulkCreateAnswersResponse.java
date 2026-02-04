package com.football501.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class BulkCreateAnswersResponse {
    private int created;
    private int skipped;
    private List<String> errors;
}
