import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Assignment, Submission } from '../models/portal.models';

@Injectable({ providedIn: 'root' })
export class AssignmentApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api`;

  listForCourse(courseId: number): Observable<Assignment[]> {
    return this.http.get<Assignment[]>(`${this.base}/courses/${courseId}/assignments`);
  }

  get(id: number): Observable<Assignment> {
    return this.http.get<Assignment>(`${this.base}/assignments/${id}`);
  }

  create(
    courseId: number,
    fields: {
      title: string;
      description?: string;
      dueAt: string;
      maxMarks: number;
      published: boolean;
    },
    file?: File
  ): Observable<Assignment> {
    const form = new FormData();
    form.append('title', fields.title);
    if (fields.description) {
      form.append('description', fields.description);
    }
    form.append('dueAt', fields.dueAt);
    form.append('maxMarks', String(fields.maxMarks));
    form.append('published', String(fields.published));
    if (file) {
      form.append('file', file);
    }
    return this.http.post<Assignment>(`${this.base}/courses/${courseId}/assignments`, form);
  }

  listSubmissions(assignmentId: number): Observable<Submission[]> {
    return this.http.get<Submission[]>(`${this.base}/assignments/${assignmentId}/submissions`);
  }

  submit(assignmentId: number, file: File): Observable<Submission> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Submission>(`${this.base}/assignments/${assignmentId}/submissions`, form);
  }

  grade(submissionId: number, marks: number, feedback: string): Observable<Submission> {
    return this.http.put<Submission>(`${this.base}/submissions/${submissionId}/grade`, {
      marks,
      feedback,
    });
  }
}