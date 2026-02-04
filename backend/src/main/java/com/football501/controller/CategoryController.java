package com.football501.controller;

import com.football501.model.Category;
import com.football501.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for retrieving category information.
 *
 * Endpoints:
 * - GET /api/categories - Get all available categories
 */
@RestController
@RequestMapping("/api/categories")
@Slf4j
public class CategoryController {

    private final QuestionService questionService;

    public CategoryController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * Get all categories.
     *
     * @return list of categories
     */
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        log.debug("Getting all categories");
        List<Category> categories = questionService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
}
