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

/** Lifecycle status values for a question. */
export type QuestionStatus = "draft" | "active" | "retired";

export interface Question {
  id: string;
  categoryId: string;
  categoryName: string;
  questionText: string;
  metricKey: string;
  config: Record<string, unknown>;
  minScore: number;
  difficulty: number;
  /** Lifecycle status: "draft" | "active" | "retired" */
  status: QuestionStatus;
  templateId?: string;
  answerCount: number;
  validDartsCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateQuestionRequest {
  categoryId: string;
  questionText: string;
  metricKey: string;
  config?: Record<string, unknown>;
  minScore?: number;
  difficulty?: number;
}

export interface UpdateQuestionRequest {
  categoryId: string;
  questionText: string;
  metricKey: string;
  config?: Record<string, unknown>;
  minScore?: number;
  difficulty?: number;
}

export interface UpdateStatusRequest {
  status: QuestionStatus;
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
  metadata?: Record<string, unknown>;
  materializedAt: string;
  createdAt: string;
}

export interface CreateAnswerRequest {
  displayText: string;
  score: number;
  metadata?: Record<string, unknown>;
}

export interface BulkCreateAnswersRequest {
  answers: CreateAnswerRequest[];
}

export interface BulkCreateAnswersResponse {
  created: number;
  skipped: number;
  errors: string[];
}
