package com.football501.service;

import com.football501.dto.admin.CategoryResponse;
import com.football501.dto.admin.CreateCategoryRequest;
import com.football501.dto.admin.UpdateCategoryRequest;
import com.football501.exception.CategoryHasQuestionsException;
import com.football501.exception.DuplicateEntityException;
import com.football501.model.Category;
import com.football501.repository.CategoryRepository;
import com.football501.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new DuplicateEntityException("Category with slug '" + request.getSlug() + "' already exists");
        }
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateEntityException("Category with name '" + request.getName() + "' already exists");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(request.getSlug().toLowerCase());
        category.setDescription(request.getDescription());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        Category saved = categoryRepository.save(category);
        log.info("Created new category: {}", saved.getName());
        return mapToResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        // Check name uniqueness if changed
        if (!category.getName().equals(request.getName()) && 
            categoryRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateEntityException("Category with name '" + request.getName() + "' already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setUpdatedAt(LocalDateTime.now());

        Category saved = categoryRepository.save(category);
        log.info("Updated category: {}", saved.getName());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (questionRepository.countByCategoryId(id) > 0) {
            throw new CategoryHasQuestionsException("Cannot delete category with existing questions");
        }
        categoryRepository.deleteById(id);
        log.info("Deleted category with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setSlug(category.getSlug());
        response.setDescription(category.getDescription());
        response.setQuestionCount(questionRepository.countByCategoryId(category.getId()));
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }
}
