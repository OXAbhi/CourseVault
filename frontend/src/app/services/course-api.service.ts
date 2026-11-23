import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Course, CourseCreate } from '../models/portal.models';

@Injectable({ providedIn: 'root' })
export class CourseApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/courses`;

  list(): Observable<Course[]> {
    return this.http.get<Course[]>(this.base);
  }

  get(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.base}/${id}`);
  }

  create(body: CourseCreate): Observable<Course> {
    return this.http.post<Course>(this.base, body);
  }
}