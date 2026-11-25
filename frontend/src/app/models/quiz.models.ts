export interface QuizQuestion {
  id: number;
  questionText: string;
  options: string[];
  correctIndex: number;
  sortOrder: number;
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