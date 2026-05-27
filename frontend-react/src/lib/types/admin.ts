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
  /** Sum of scores for all valid-darts, non-bust answers. Below 501 = unfinishable. */
  totalPointsPool: number;
  /** Count of valid-darts answers with score 101–180. Indicates finishing power. */
  highValueAnswerCount: number;
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

// ── Question Templates ─────────────────────────────────────────────────────

export interface QuestionTemplate {
  id: string;
  categoryId: string;
  slug: string;
  displayName: string;
  textTemplate: string;
  materializerKey: string;
  metricKey: string;
  defaultMinScore: number | null;
  /** Whether the template itself is enabled for generation. */
  active: boolean;
  /**
   * True when a matching QuestionMaterializer bean is registered on the
   * backend.  False means draft questions will be created but activation
   * will fail until the materializer is implemented.
   */
  hasMaterializer: boolean;
  /** Draft questions currently generated from this template. */
  draftCount: number;
  /** Active questions currently generated from this template. */
  activeCount: number;
  createdAt: string;
  updatedAt: string;
}

/** Result returned by the generator endpoints. */
export interface GeneratorResult {
  created: number;
  skipped: number;
  total: number;
  /** Set when a single-template run is requested. */
  template_id?: string;
  message: string;
}

/** Result returned by the re-materialize endpoint. */
export interface RematerializeResult {
  questionId: string;
  answersUpserted: number;
}

export interface EntityBackfillResult {
  inserted: number;
  skipped:  number;
}

export interface BulkActivateResult {
  activated:       number;
  answersUpserted: number;
  errors:          number;
  remainingDraft:  number;
}

/** All valid metric_key values understood by the materializers. */
export const METRIC_KEY_OPTIONS = [
  { value: "goals",           label: "Goals" },
  { value: "appearances",     label: "Appearances" },
  { value: "assists",         label: "Assists" },
  { value: "goals_assists",   label: "Goals + Assists" },
  { value: "clean_sheets",    label: "Clean sheets" },
  { value: "sub_appearances", label: "Substitute appearances" },
] as const;

export type MetricKey = typeof METRIC_KEY_OPTIONS[number]["value"];
