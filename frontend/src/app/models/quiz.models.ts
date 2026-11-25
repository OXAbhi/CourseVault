export interface QuizQuestion {
  id: number;
  questionText: string;
  options: string[];
  correctIndex: number;
  sortOrder: number;
  explanation?: string | null;
}

export interface Quiz {
  id: number;
  title: string;
  published: boolean;
  createdAt: string;
  courseId: number;
  sourceResourceId: number;
  sourceResourceTitle: string;
  createdByUsername: string;
  questionCount: number;
  questions: QuizQuestion[];
}

export interface QuizGenerateRequest {
  instructions?: string;
  pages?: string;
  questionCount: 5 | 10 | 15;
  difficulty: 'easy' | 'medium' | 'hard';
}

export interface QuizQuestionUpdateRequest {
  questionText: string;
  options: [string, string, string, string];
  correctIndex: number;
  explanation?: string;
}

export interface QuizAttemptSummary {
  attemptId: number;
  studentUsername: string;
  studentDisplayName: string;
  score: number;
  totalQuestions: number;
  submittedAt: string;
}

export interface QuizAttemptResult {
  questionId: number;
  questionText: string;
  options: string[];
  selectedIndex: number;
  correctIndex: number;
  correct: boolean;
}

export interface QuizAttempt {
  attemptId: number;
  quizId: number;
  quizTitle: string;
  score: number;
  totalQuestions: number;
  submittedAt: string;
  results: QuizAttemptResult[];
}