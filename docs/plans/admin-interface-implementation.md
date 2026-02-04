# Admin Interface Implementation Plan

## Overview

Build a comprehensive admin interface for managing categories, questions, and answers in the Football 501 application. The admin panel will allow adding, editing, and removing data while maintaining database integrity and following existing codebase patterns.

## Architecture Summary

**Backend**: Spring Boot REST API at `/api/admin/` namespace
**Frontend**: SvelteKit pages at `/admin/` route with reusable components
**Database**: PostgreSQL with existing schema (categories, questions, answers tables)

## Implementation Phases

### Phase 1: Backend - Category Management

Create admin services and controllers for category CRUD operations.

**Files to Create:**

1. **backend/src/main/java/com/football501/dto/admin/CreateCategoryRequest.java**
   - Fields: name, slug, description
   - Validation: @NotBlank on name and slug

2. **backend/src/main/java/com/football501/dto/admin/UpdateCategoryRequest.java**
   - Fields: name, description (slug immutable after creation)

3. **backend/src/main/java/com/football501/dto/admin/CategoryResponse.java**
   - Fields: id, name, slug, description, questionCount, createdAt, updatedAt

4. **backend/src/main/java/com/football501/service/AdminCategoryService.java**
   - Methods:
     - `CategoryResponse createCategory(CreateCategoryRequest)`
     - `CategoryResponse updateCategory(UUID id, UpdateCategoryRequest)`
     - `void deleteCategory(UUID id)` - throws if questions exist
     - `List<CategoryResponse> listCategories()`
     - `CategoryResponse getCategory(UUID id)`
   - Validation: Check slug uniqueness, prevent delete if questions exist

5. **backend/src/main/java/com/football501/controller/AdminCategoryController.java**
   - Endpoints:
     - `POST /api/admin/categories` - Create category
     - `GET /api/admin/categories` - List all categories with question counts
     - `GET /api/admin/categories/{id}` - Get single category
     - `PUT /api/admin/categories/{id}` - Update category
     - `DELETE /api/admin/categories/{id}` - Delete category (error if questions exist)
   - Error handling with @ExceptionHandler

6. **backend/src/main/java/com/football501/exception/DuplicateEntityException.java**
   - Custom exception for duplicate slug/name validation

7. **backend/src/main/java/com/football501/exception/CategoryHasQuestionsException.java**
   - Custom exception for delete prevention

**Modifications:**
- **backend/src/main/java/com/football501/repository/QuestionRepository.java**
  - Add: `long countByCategoryId(UUID categoryId)`

### Phase 2: Backend - Question Management

Implement question CRUD with soft delete and filtering.

**Files to Create:**

1. **backend/src/main/java/com/football501/dto/admin/CreateQuestionRequest.java**
   - Fields: categoryId, questionText, metricKey, config (Map<String, Object>), minScore

2. **backend/src/main/java/com/football501/dto/admin/UpdateQuestionRequest.java**
   - Same fields as create (allow full update)

3. **backend/src/main/java/com/football501/dto/admin/QuestionResponse.java**
   - Fields: id, categoryId, categoryName, questionText, metricKey, config, minScore, isActive, answerCount, validDartsCount, createdAt, updatedAt

4. **backend/src/main/java/com/football501/dto/admin/QuestionListResponse.java**
   - Paginated wrapper with: content (List<QuestionResponse>), totalElements, totalPages, currentPage

5. **backend/src/main/java/com/football501/service/AdminQuestionService.java**
   - Methods:
     - `QuestionResponse createQuestion(CreateQuestionRequest)` - creates with isActive=false
     - `QuestionResponse updateQuestion(UUID id, UpdateQuestionRequest)`
     - `QuestionResponse toggleActive(UUID id)` - soft delete toggle
     - `QuestionListResponse listQuestions(UUID categoryId, Boolean isActive, Pageable)`
     - `QuestionResponse getQuestion(UUID id)` - includes computed counts

6. **backend/src/main/java/com/football501/controller/AdminQuestionController.java**
   - Endpoints:
     - `POST /api/admin/questions` - Create question
     - `GET /api/admin/questions?categoryId=&isActive=&page=&size=` - List with filters
     - `GET /api/admin/questions/{id}` - Get single question with counts
     - `PUT /api/admin/questions/{id}` - Update question
     - `PATCH /api/admin/questions/{id}/toggle-active` - Toggle active status
     - `DELETE /api/admin/questions/{id}` - Hard delete (cascade to answers)

**Modifications:**
- **backend/src/main/java/com/football501/repository/QuestionRepository.java**
  - Add: `Page<Question> findByCategoryIdAndIsActive(UUID categoryId, Boolean isActive, Pageable)`
  - Add: `Page<Question> findAll(Pageable)` (already exists in JpaRepository)

### Phase 3: Backend - Answer Management

Implement answer CRUD with bulk import and validation.

**Files to Create:**

1. **backend/src/main/java/com/football501/dto/admin/CreateAnswerRequest.java**
   - Fields: displayText, score, metadata (Map<String, Object>)

2. **backend/src/main/java/com/football501/dto/admin/BulkCreateAnswersRequest.java**
   - Field: answers (List<CreateAnswerRequest>)

3. **backend/src/main/java/com/football501/dto/admin/AnswerResponse.java**
   - Fields: id, questionId, answerKey, displayText, score, isValidDarts, isBust, metadata, createdAt

4. **backend/src/main/java/com/football501/dto/admin/BulkCreateAnswersResponse.java**
   - Fields: created (int), skipped (int), errors (List<String>)

5. **backend/src/main/java/com/football501/service/AdminAnswerService.java**
   - Methods:
     - `AnswerResponse createAnswer(UUID questionId, CreateAnswerRequest)` - validates darts score
     - `BulkCreateAnswersResponse bulkCreateAnswers(UUID questionId, BulkCreateAnswersRequest)`
     - `List<AnswerResponse> listAnswers(UUID questionId)` - ordered by score DESC
     - `AnswerResponse updateAnswer(UUID id, CreateAnswerRequest)`
     - `void deleteAnswer(UUID id)` - hard delete
     - `void deleteAnswers(List<UUID> ids)` - bulk delete
   - Helper methods:
     - `String normalizeAnswerKey(String displayText)` - lowercase trim
     - `boolean isValidDartsScore(int score)` - check against invalid list
     - `boolean isBust(int score)` - score > 180

6. **backend/src/main/java/com/football501/controller/AdminAnswerController.java**
   - Endpoints:
     - `POST /api/admin/questions/{questionId}/answers` - Create single answer
     - `POST /api/admin/questions/{questionId}/answers/bulk` - Bulk create (CSV/JSON)
     - `GET /api/admin/questions/{questionId}/answers` - List all answers for question
     - `PUT /api/admin/answers/{id}` - Update answer
     - `DELETE /api/admin/answers/{id}` - Delete single answer
     - `DELETE /api/admin/answers/bulk` - Delete multiple answers (body: {ids: []})

7. **backend/src/main/java/com/football501/util/DartsScoreValidator.java**
   - Static utility class with invalid scores constant: [163, 166, 169, 172, 173, 175, 176, 178, 179]
   - Method: `public static boolean isValid(int score)`

**Modifications:**
- **backend/src/main/java/com/football501/repository/AnswerRepository.java**
  - Add: `List<Answer> findByQuestionIdOrderByScoreDesc(UUID questionId)`
  - Add: `boolean existsByQuestionIdAndAnswerKey(UUID questionId, String answerKey)`

### Phase 4: Frontend - Shared Components & API Client

Build reusable components and centralized API utilities.

**Files to Create:**

1. **frontend/src/lib/api/admin.ts**
   - Centralized API client with methods for all admin operations
   - Base URL from environment variable (fallback to localhost:8080)
   - Generic error handling
   - Methods: createCategory, updateCategory, deleteCategory, listCategories, etc.

2. **frontend/src/lib/components/admin/Table/DataTable.svelte**
   - Props: columns (array), data (array), onEdit, onDelete, loading
   - Features: Sortable columns, pagination controls, loading skeleton
   - Emits: edit, delete, pageChange events

3. **frontend/src/lib/components/admin/Form/TextField.svelte**
   - Props: label, value, error, placeholder, disabled, required
   - Two-way binding with bind:value
   - Displays error message below input

4. **frontend/src/lib/components/admin/Form/TextArea.svelte**
   - Similar to TextField but multiline

5. **frontend/src/lib/components/admin/Form/Select.svelte**
   - Props: label, value, options (array of {value, label}), error, disabled, required

6. **frontend/src/lib/components/admin/Modal/ConfirmDialog.svelte**
   - Props: open, title, message, confirmText, cancelText, onConfirm, onCancel
   - Displays warning message with confirm/cancel buttons

7. **frontend/src/lib/components/admin/Modal/FormModal.svelte**
   - Props: open, title, onClose, onSave, loading
   - Slot for form content
   - Save/Cancel buttons with loading states

8. **frontend/src/lib/components/admin/Feedback/Toast.svelte**
   - Props: message, type (success/error/info), duration
   - Auto-dismiss after duration
   - Positioned fixed top-right

9. **frontend/src/lib/types/admin.ts**
   - TypeScript interfaces for all admin DTOs
   - Interfaces: Category, Question, Answer, CreateCategoryRequest, etc.

### Phase 5: Frontend - Admin Layout & Navigation

Create admin section structure with navigation.

**Files to Create:**

1. **frontend/src/routes/admin/+layout.svelte**
   - Admin layout with sidebar navigation
   - Links: Categories, Questions, Dashboard (future)
   - Styled with dark theme matching existing design
   - Slot for child routes

2. **frontend/src/lib/components/admin/Navigation/Sidebar.svelte**
   - Navigation links with active state
   - Props: currentPath
   - Items: Categories, Questions

### Phase 6: Frontend - Category Management UI

Implement category list and CRUD forms.

**Files to Create:**

1. **frontend/src/routes/admin/categories/+page.svelte**
   - Category list page with DataTable
   - Features:
     - Display all categories with question counts
     - Create button opens FormModal
     - Edit button opens FormModal with existing data
     - Delete button shows ConfirmDialog with warning if questions exist
   - State: categories array, loading, error, selectedCategory, modals (create/edit/delete open states)

2. **frontend/src/lib/components/admin/Category/CategoryForm.svelte**
   - Form component for create/edit
   - Props: category (optional for edit mode), onSubmit, onCancel
   - Fields: name, slug, description
   - Validation: Required name and slug, slug format (lowercase, hyphens only)

### Phase 7: Frontend - Question Management UI

Implement question list and detail pages.

**Files to Create:**

1. **frontend/src/routes/admin/questions/+page.svelte**
   - Question list page with filters
   - Features:
     - Filter by category (dropdown)
     - Filter by active status (dropdown: All/Active/Inactive)
     - Pagination controls
     - Create button navigates to create page
     - Edit button navigates to detail page
     - Toggle active button (PATCH request)
   - DataTable shows: question text, category, answer count, active status, actions

2. **frontend/src/routes/admin/questions/create/+page.svelte**
   - Question create form page
   - Form fields: category (select), questionText (textarea), metricKey (select), minScore (number), config (JSON textarea)
   - Submit creates question with isActive=false, redirects to detail page

3. **frontend/src/routes/admin/questions/[id]/+page.svelte**
   - Question detail page with embedded answer management
   - Top section: Question details with edit/toggle active/delete buttons
   - Middle section: Answer statistics (total count, valid darts count)
   - Bottom section: Answer table with add/edit/delete actions
   - Features:
     - Add answer button opens FormModal
     - Bulk import button opens CSV upload modal
     - Answer table shows: display text, score, valid darts indicator, actions
     - Edit answer inline or modal
     - Delete answer with confirmation

4. **frontend/src/lib/components/admin/Question/QuestionForm.svelte**
   - Reusable form for create/edit question
   - Props: question (optional), categories (array), onSubmit, onCancel

5. **frontend/src/lib/components/admin/Answer/AnswerForm.svelte**
   - Form for single answer create/edit
   - Fields: displayText, score, metadata (JSON textarea)
   - Validation: Required display text and score, score 1-300 range

6. **frontend/src/lib/components/admin/Answer/BulkImportModal.svelte**
   - CSV upload modal for bulk answer import
   - Features:
     - File input (accept .csv)
     - CSV format instructions (columns: displayText, score)
     - Preview parsed data (first 5 rows)
     - Submit button triggers bulk create API
     - Display results (created/skipped counts, errors)

### Phase 8: Integration & Testing

**Testing Strategy:**

1. **Backend Integration Tests**
   - Test each controller endpoint with valid/invalid inputs
   - Test cascade delete scenarios
   - Test validation (duplicate slug, category with questions)
   - Test bulk import with large datasets

2. **Frontend E2E Tests (Manual for MVP)**
   - Create category → Create question → Add answers → Toggle active
   - Edit existing category/question/answer
   - Delete with confirmation dialogs
   - Bulk import CSV with 50+ answers
   - Pagination navigation
   - Filter and search

3. **Data Integrity Tests**
   - Verify answer normalization (case insensitive)
   - Verify darts score validation
   - Verify cascade deletes (category → questions → answers)
   - Verify soft delete (toggle active)

## Critical Implementation Details

### Answer Validation Logic

```java
// In AdminAnswerService.java
private static final Set<Integer> INVALID_DARTS_SCORES = Set.of(
    163, 166, 169, 172, 173, 175, 176, 178, 179
);

private String normalizeAnswerKey(String displayText) {
    return displayText.toLowerCase().trim();
}

private boolean isValidDartsScore(int score) {
    return score >= 1 && score <= 180 && !INVALID_DARTS_SCORES.contains(score);
}

private boolean isBust(int score) {
    return score > 180;
}
```

### Bulk Answer Import CSV Format

```csv
displayText,score
Erling Haaland,52
Kevin De Bruyne,31
Jack Grealish,28
```

### Database Cascade Behavior

- **Delete Category**: Cascades to Questions → Answers (via FK ON DELETE CASCADE)
- **Delete Question**: Cascades to Answers (via FK ON DELETE CASCADE)
- **Delete Answer**: Direct delete (no cascades)
- **Soft Delete**: Toggle `isActive` flag on Question (preserves all data)

### API Error Response Format

```json
{
  "error": "DUPLICATE_ENTITY",
  "message": "Category with slug 'football' already exists",
  "timestamp": "2024-01-28T10:30:45Z"
}
```

## File Structure Summary

### Backend (Spring Boot)
```
backend/src/main/java/com/football501/
├── controller/
│   ├── AdminCategoryController.java (NEW)
│   ├── AdminQuestionController.java (NEW)
│   └── AdminAnswerController.java (NEW)
├── service/
│   ├── AdminCategoryService.java (NEW)
│   ├── AdminQuestionService.java (NEW)
│   └── AdminAnswerService.java (NEW)
├── dto/admin/
│   ├── CreateCategoryRequest.java (NEW)
│   ├── UpdateCategoryRequest.java (NEW)
│   ├── CategoryResponse.java (NEW)
│   ├── CreateQuestionRequest.java (NEW)
│   ├── UpdateQuestionRequest.java (NEW)
│   ├── QuestionResponse.java (NEW)
│   ├── QuestionListResponse.java (NEW)
│   ├── CreateAnswerRequest.java (NEW)
│   ├── BulkCreateAnswersRequest.java (NEW)
│   ├── AnswerResponse.java (NEW)
│   └── BulkCreateAnswersResponse.java (NEW)
├── exception/
│   ├── DuplicateEntityException.java (NEW)
│   └── CategoryHasQuestionsException.java (NEW)
├── util/
│   └── DartsScoreValidator.java (NEW)
└── repository/
    └── QuestionRepository.java (MODIFY - add countByCategoryId)
```

### Frontend (SvelteKit)
```
frontend/src/
├── lib/
│   ├── api/
│   │   └── admin.ts (NEW)
│   ├── types/
│   │   └── admin.ts (NEW)
│   └── components/admin/
│       ├── Table/
│       │   └── DataTable.svelte (NEW)
│       ├── Form/
│       │   ├── TextField.svelte (NEW)
│       │   ├── TextArea.svelte (NEW)
│       │   └── Select.svelte (NEW)
│       ├── Modal/
│       │   ├── ConfirmDialog.svelte (NEW)
│       │   └── FormModal.svelte (NEW)
│       ├── Feedback/
│       │   └── Toast.svelte (NEW)
│       ├── Navigation/
│       │   └── Sidebar.svelte (NEW)
│       ├── Category/
│       │   └── CategoryForm.svelte (NEW)
│       ├── Question/
│       │   └── QuestionForm.svelte (NEW)
│       └── Answer/
│           ├── AnswerForm.svelte (NEW)
│           └── BulkImportModal.svelte (NEW)
└── routes/
    └── admin/
        ├── +layout.svelte (NEW)
        ├── categories/
        │   └── +page.svelte (NEW)
        └── questions/
            ├── +page.svelte (NEW)
            ├── create/
            │   └── +page.svelte (NEW)
            └── [id]/
                └── +page.svelte (NEW)
```

## Verification Steps

After implementation, verify the following end-to-end workflows:

### Workflow 1: Create Complete Question
1. Navigate to /admin/categories
2. Create new category "Test Category" (slug: "test-category")
3. Navigate to /admin/questions
4. Click "Create Question"
5. Fill form: category=Test Category, questionText="Premier League Appearances", metricKey="appearances"
6. Submit (isActive=false by default)
7. Click on created question to view detail
8. Add answers individually (3-5 answers)
9. Use bulk import to add 20 more answers via CSV
10. Verify answer list shows all answers with valid darts indicators
11. Toggle question to active
12. Verify question appears in game (start practice game, select category)

### Workflow 2: Edit and Delete
1. Edit category description
2. Edit question text and minScore
3. Edit individual answer (change score)
4. Delete individual answer (with confirmation)
5. Try to delete category with questions (should fail with error)
6. Toggle question inactive
7. Delete question (cascades to answers)
8. Now delete category (should succeed)

### Workflow 3: Bulk Operations
1. Create question with 0 answers
2. Bulk import CSV with 100 answers
3. Verify all answers created (check created count in response)
4. Try importing same CSV again (should skip duplicates)
5. Bulk delete 10 answers
6. Verify remaining count is 90

### Workflow 4: Data Integrity
1. Create answer with score 163 (invalid darts score)
2. Verify isValidDarts = false in database
3. Create answer with score 52 (valid darts score)
4. Verify isValidDarts = true in database
5. Verify answerKey is normalized (lowercase) in database
6. Verify displayText preserves original case

## Future Enhancements (Out of Scope)

- Authentication/authorization with role-based access control
- Audit logging (track who created/modified what)
- Advanced filtering (search by text, date ranges)
- Export functionality (download questions/answers as CSV/JSON)
- Answer validation against external API (verify player exists)
- Duplicate detection during bulk import (fuzzy matching)
- Question preview mode (test question before activating)
- Answer statistics (most/least used answers)
- Category reordering (display order)
- Soft delete for categories
- Multi-select bulk operations (delete multiple questions at once)
- Keyboard shortcuts (Ctrl+K for search, Ctrl+N for new)

## Success Criteria

The admin interface is complete when:
- ✅ All CRUD operations work for categories, questions, and answers
- ✅ Data validation prevents invalid states (duplicate slugs, invalid darts scores)
- ✅ Cascade deletes maintain referential integrity
- ✅ Bulk import handles 100+ answers efficiently
- ✅ UI provides clear feedback for success/error states
- ✅ No authentication required (open admin for MVP)
- ✅ All existing game functionality still works
- ✅ Database schema unchanged (no migrations needed)
- ✅ Code follows existing patterns (controllers, services, DTOs, components)
