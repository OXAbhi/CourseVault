import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Quiz,
  QuizAttempt,
  QuizAttemptSummary,
  QuizGenerateRequest,
  QuizQuestionUpdateRequest,
} from '../models/quiz.models';

@Injectable({ providedIn: 'root' })
export class QuizApiService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  listForCourse(courseId: number): Observable<Quiz[]> {
    return this.http.get<Quiz[]>(`${this.api}/api/courses/${courseId}/quizzes`);
  }

  get(quizId: number): Observable<Quiz> {
    return this.http.get<Quiz>(`${this.api}/api/quizzes/${quizId}`);
  }

  generateFromResource(resourceId: number, body: QuizGenerateRequest): Observable<Quiz> {
    return this.http.post<Quiz>(`${this.api}/api/resources/${resourceId}/quizzes/generate`, body);
  }

  publish(quizId: number, published: boolean): Observable<Quiz> {
    return this.http.patch<Quiz>(`${this.api}/api/quizzes/${quizId}/publish`, { published });
  }

  deleteQuiz(quizId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/api/quizzes/${quizId}`);
  }

  updateQuestion(
    quizId: number,
    questionId: number,
    body: QuizQuestionUpdateRequest
  ): Observable<Quiz> {
    return this.http.put<Quiz>(`${this.api}/api/quizzes/${quizId}/questions/${questionId}`, body);
  }

  deleteQuestion(quizId: number, questionId: number): Observable<Quiz> {
    return this.http.delete<Quiz>(`${this.api}/api/quizzes/${quizId}/questions/${questionId}`);
  }

  listAttempts(quizId: number): Observable<QuizAttemptSummary[]> {
    return this.http.get<QuizAttemptSummary[]>(`${this.api}/api/quizzes/${quizId}/attempts`);
  }

  submitAttempt(
    quizId: number,
    answers: { questionId: number; selectedIndex: number }[]
  ): Observable<QuizAttempt> {
    return this.http.post<QuizAttempt>(`${this.api}/api/quizzes/${quizId}/attempts`, { answers });
  }

  getLatestAttempt(quizId: number): Observable<QuizAttempt> {
    return this.http.get<QuizAttempt>(`${this.api}/api/quizzes/${quizId}/attempts/latest`);
  }
}