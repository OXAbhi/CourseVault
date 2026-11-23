import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Batch, BatchCreate, Enrollment } from '../models/portal.models';

@Injectable({ providedIn: 'root' })
export class BatchApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api`;

  listForCourse(courseId: number): Observable<Batch[]> {
    return this.http.get<Batch[]>(`${this.base}/courses/${courseId}/batches`);
  }

  create(courseId: number, body: BatchCreate): Observable<Batch> {
    return this.http.post<Batch>(`${this.base}/courses/${courseId}/batches`, body);
  }

  enrollments(batchId: number): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`${this.base}/batches/${batchId}/enrollments`);
  }
}