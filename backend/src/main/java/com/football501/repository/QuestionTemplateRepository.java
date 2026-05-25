package com.football501.repository;

import com.football501.model.QuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, UUID> {

    Optional<QuestionTemplate> findBySlug(String slug);

    List<QuestionTemplate> findByCategoryId(UUID categoryId);

    List<QuestionTemplate> findByIsActiveTrue();
}
