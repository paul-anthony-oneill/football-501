package com.trivia501.mapper;

import com.trivia501.dto.admin.AnswerResponse;
import com.trivia501.model.Answer;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link Answer} → {@link AnswerResponse}.
 *
 * <p>All fields in {@link AnswerResponse} map 1:1 from {@link Answer} — no
 * computed values or extra repository calls are needed.  MapStruct generates
 * the implementation at compile time; the {@code componentModel = "spring"}
 * setting registers it as a Spring bean.
 *
 * <p>Inject via {@code @Autowired AnswerMapper answerMapper} or via Lombok
 * {@code @RequiredArgsConstructor} on a {@code final} field.
 *
 * <p>Fields present on {@link Answer} but absent from {@link AnswerResponse}
 * (e.g. {@code materializedAt}) are silently ignored — no {@code @Mapping}
 * annotation is required for them.
 */
@Mapper(componentModel = "spring")
public interface AnswerMapper {

    /**
     * Maps an {@link Answer} entity to its admin API response DTO.
     * The generated implementation calls a getter-setter pair for each
     * matching field name, identical to the hand-written code it replaces.
     */
    AnswerResponse toResponse(Answer answer);
}
