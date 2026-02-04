export interface Category {
    id: string;
    name: string;
    slug: string;
    description: string;
    questionCount: number;
    createdAt: string;
    updatedAt: string;
}

export interface CreateCategoryRequest {
    name: string;
    slug: string;
    description?: string;
}

export interface UpdateCategoryRequest {
    name: string;
    description?: string;
}

export interface Question {
    id: string;
    categoryId: string;
    categoryName: string;
    questionText: string;
    metricKey: string;
    config: Record<string, any>;
    minScore: number;
    isActive: boolean;
    answerCount: number;
    validDartsCount: number;
    createdAt: string;
    updatedAt: string;
}

export interface CreateQuestionRequest {
    categoryId: string;
    questionText: string;
    metricKey: string;
    config?: Record<string, any>;
    minScore?: number;
}

export interface UpdateQuestionRequest {
    categoryId: string;
    questionText: string;
    metricKey: string;
    config?: Record<string, any>;
    minScore?: number;
}

export interface QuestionListResponse {
    content: Question[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
}

export interface Answer {
    id: string;
    questionId: string;
    answerKey: string;
    displayText: string;
    score: number;
    isValidDarts: boolean;
    isBust: boolean;
    metadata?: Record<string, any>;
    createdAt: string;
}

export interface CreateAnswerRequest {
    displayText: string;
    score: number;
    metadata?: Record<string, any>;
}

export interface BulkCreateAnswersRequest {
    answers: CreateAnswerRequest[];
}

export interface BulkCreateAnswersResponse {
    created: number;
    skipped: number;
    errors: string[];
}
