import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ResourceAccess, ResourceItem, ResourceType } from '../models/portal.models';

@Injectable({ providedIn: 'root' })
export class ResourceApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api`;

  listForCourse(courseId: number): Observable<ResourceItem[]> {
    return this.http.get<ResourceItem[]>(`${this.base}/courses/${courseId}/resources`);
  }

  get(id: number): Observable<ResourceItem> {
    return this.http.get<ResourceItem>(`${this.base}/resources/${id}`);
  }

  access(id: number): Observable<ResourceAccess> {
    return this.http.get<ResourceAccess>(`${this.base}/resources/${id}/access`);
  }

  upload(
    courseId: number,
    file: File,
    title: string,
    resourceType: ResourceType,
    tags: string,
    published: boolean
  ): Observable<ResourceItem> {
    const form = new FormData();
    form.append('file', file);
    form.append('title', title);
    form.append('resourceType', resourceType);
    if (tags) {
      form.append('tags', tags);
    }
    form.append('published', String(published));
    return this.http.post<ResourceItem>(`${this.base}/courses/${courseId}/resources`, form);
  }

  uploadWithProgress(
    courseId: number,
    file: File,
    title: string,
    resourceType: ResourceType,
    tags: string,
    published: boolean
  ): Observable<HttpEvent<ResourceItem>> {
    const form = new FormData();
    form.append('file', file);
    form.append('title', title);
    form.append('resourceType', resourceType);
    if (tags) {
      form.append('tags', tags);
    }
    form.append('published', String(published));
    return this.http.post<ResourceItem>(`${this.base}/courses/${courseId}/resources`, form, {
      reportProgress: true,
      observe: 'events',
    });
  }
}