package com.football501.mapper;

import com.football501.dto.admin.CategoryResponse;
import com.football501.model.Category;
import com.football501.repository.QuestionRepository;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper for {@link Category} → {@link CategoryResponse}.
 *
 * <p>Direct fields ({@code id}, {@code name}, {@code slug}, {@code description},
 * {@code createdAt}, {@code updatedAt}) are mapped automatically by MapStruct.
 *
 * <p>{@code questionCount} is not a field on {@link Category} — it requires a
 * database query.  The {@link #fillQuestionCount} method is called by the
 * generated code after all auto-mapped fields are set, injecting the count
 * from {@link QuestionRepository}.
 *
 * <p>This class is declared {@code abstract} (rather than an interface) so that
 * the {@code @Autowired} field injection and the {@code @AfterMapping} method
 * can coexist in the same type.  MapStruct generates a concrete Spring
 * {@code @Component} subclass at compile time.
 *
 * <p>Inject via {@code @Autowired CategoryMapper categoryMapper} or via Lombok
 * {@code @RequiredArgsConstructor} on a {@code final} field.
 */
@Mapper(componentModel = "spring")
public abstract class CategoryMapper {

    /**
     * Injected by Spring into the generated subclass.  Used in
     * {@link #fillQuestionCount} to count questions per category.
     */
    @Autowired
    protected QuestionRepository questionRepository;

    /**
     * Maps a {@link Category} entity to its admin API response DTO.
     *
     * <p>{@code questionCount} is excluded from the auto-mapping and instead
     * filled in by {@link #fillQuestionCount} after the primary mapping runs.
     */
    @Mapping(target = "questionCount", ignore = true)
    public abstract CategoryResponse toResponse(Category category);

    /**
     * Fills in {@code questionCount} after MapStruct has populated all
     * directly-mapped fields.  One additional DB round-trip per category.
     */
    @AfterMapping
    protected void fillQuestionCount(Category category,
                                     @MappingTarget CategoryResponse response) {
        response.setQuestionCount(questionRepository.countByCategoryId(category.getId()));
    }
}
