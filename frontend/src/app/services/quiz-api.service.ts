import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Quiz, QuizAttempt } from '../models/quiz.models';

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

  generateFromResource(resourceId: number, questionCount = 5): Observable<Quiz> {
    return this.http.post<Quiz>(`${this.api}/api/resources/${resourceId}/quizzes/generate`, {
      questionCount,
    });
  }

  publish(quizId: number, published: boolean): Observable<Quiz> {
    return this.http.patch<Quiz>(`${this.api}/api/quizzes/${quizId}/publish`, { published });
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