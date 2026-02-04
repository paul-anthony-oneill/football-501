package com.football501.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.football501.dto.admin.CategoryResponse;
import com.football501.dto.admin.CreateCategoryRequest;
import com.football501.service.AdminCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCategoryController.class)
@DisplayName("AdminCategoryController Tests")
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminCategoryService adminCategoryService;

    private CreateCategoryRequest createRequest;
    private CategoryResponse categoryResponse;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();

        createRequest = new CreateCategoryRequest();
        createRequest.setName("Premier League");
        createRequest.setSlug("premier-league");
        createRequest.setDescription("English Premier League stats");

        categoryResponse = new CategoryResponse();
        categoryResponse.setId(categoryId);
        categoryResponse.setName("Premier League");
        categoryResponse.setSlug("premier-league");
        categoryResponse.setDescription("English Premier League stats");
        categoryResponse.setQuestionCount(0);
        categoryResponse.setCreatedAt(LocalDateTime.now());
        categoryResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create category and return created response")
    void shouldCreateCategory() throws Exception {
        // Given
        when(adminCategoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(categoryResponse);

        // When/Then
        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(categoryId.toString()))
            .andExpect(jsonPath("$.name").value("Premier League"))
            .andExpect(jsonPath("$.slug").value("premier-league"));
    }

    @Test
    @DisplayName("Should return 400 when creating category with invalid data")
    void shouldReturn400WhenInvalidRequest() throws Exception {
        // Given
        CreateCategoryRequest invalidRequest = new CreateCategoryRequest();
        // Missing name and slug

        // When/Then
        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }
}
