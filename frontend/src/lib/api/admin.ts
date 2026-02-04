import type {
    Category,
    CreateCategoryRequest,
    UpdateCategoryRequest,
    Question,
    QuestionListResponse,
    CreateQuestionRequest,
    UpdateQuestionRequest,
    Answer,
    CreateAnswerRequest,
    BulkCreateAnswersRequest,
    BulkCreateAnswersResponse
} from '$lib/types/admin';

const API_BASE = 'http://localhost:8080/api/admin';

class AdminApiClient {
    private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options.headers,
            },
        });

        if (!response.ok) {
            let errorMessage = 'An error occurred';
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (e) {
                // ignore
            }
            throw new Error(errorMessage);
        }

        if (response.status === 204) {
            return {} as T;
        }

        return response.json();
    }

    // Categories
    async listCategories(): Promise<Category[]> {
        return this.request<Category[]>('/categories');
    }

    async getCategory(id: string): Promise<Category> {
        return this.request<Category>(`/categories/${id}`);
    }

    async createCategory(data: CreateCategoryRequest): Promise<Category> {
        return this.request<Category>('/categories', {
            method: 'POST',
            body: JSON.stringify(data),
        });
    }

    async updateCategory(id: string, data: UpdateCategoryRequest): Promise<Category> {
        return this.request<Category>(`/categories/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data),
        });
    }

    async deleteCategory(id: string): Promise<void> {
        return this.request<void>(`/categories/${id}`, {
            method: 'DELETE',
        });
    }

    // Questions
    async listQuestions(categoryId?: string, isActive?: boolean, page = 0, size = 10): Promise<QuestionListResponse> {
        const params = new URLSearchParams();
        if (categoryId) params.append('categoryId', categoryId);
        if (isActive !== undefined) params.append('isActive', String(isActive));
        params.append('page', String(page));
        params.append('size', String(size));

        return this.request<QuestionListResponse>(`/questions?${params.toString()}`);
    }

    async getQuestion(id: string): Promise<Question> {
        return this.request<Question>(`/questions/${id}`);
    }

    async createQuestion(data: CreateQuestionRequest): Promise<Question> {
        return this.request<Question>('/questions', {
            method: 'POST',
            body: JSON.stringify(data),
        });
    }

    async updateQuestion(id: string, data: UpdateQuestionRequest): Promise<Question> {
        return this.request<Question>(`/questions/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data),
        });
    }

    async toggleQuestionActive(id: string): Promise<Question> {
        return this.request<Question>(`/questions/${id}/toggle-active`, {
            method: 'PATCH',
        });
    }

    async deleteQuestion(id: string): Promise<void> {
        return this.request<void>(`/questions/${id}`, {
            method: 'DELETE',
        });
    }

    // Answers
    async listAnswers(questionId: string): Promise<Answer[]> {
        return this.request<Answer[]>(`/questions/${questionId}/answers`);
    }

    async createAnswer(questionId: string, data: CreateAnswerRequest): Promise<Answer> {
        return this.request<Answer>(`/questions/${questionId}/answers`, {
            method: 'POST',
            body: JSON.stringify(data),
        });
    }

    async bulkCreateAnswers(questionId: string, data: BulkCreateAnswersRequest): Promise<BulkCreateAnswersResponse> {
        return this.request<BulkCreateAnswersResponse>(`/questions/${questionId}/answers/bulk`, {
            method: 'POST',
            body: JSON.stringify(data),
        });
    }

    async updateAnswer(id: string, data: CreateAnswerRequest): Promise<Answer> {
        return this.request<Answer>(`/answers/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data),
        });
    }

    async deleteAnswer(id: string): Promise<void> {
        return this.request<void>(`/answers/${id}`, {
            method: 'DELETE',
        });
    }

    async deleteAnswers(ids: string[]): Promise<void> {
        return this.request<void>('/answers/bulk', {
            method: 'DELETE',
            body: JSON.stringify({ ids }),
        });
    }
}

export const adminApi = new AdminApiClient();
