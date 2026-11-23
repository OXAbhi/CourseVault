import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Enrollment, EnrollmentStatus } from '../models/portal.models';

@Injectable({ providedIn: 'root' })
export class EnrollmentApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/enrollments`;

  list(): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(this.base);
  }

  create(studentId: number, batchId: number): Observable<Enrollment> {
    return this.http.post<Enrollment>(this.base, { studentId, batchId });
  }

  patchStatus(id: number, status: EnrollmentStatus): Observable<Enrollment> {
    return this.http.patch<Enrollment>(`${this.base}/${id}`, { status });
  }
}