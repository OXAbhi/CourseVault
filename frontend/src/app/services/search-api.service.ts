import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ResourceType, SearchHit } from '../models/portal.models';

@Injectable({ providedIn: 'root' })
export class SearchApiService {
  private readonly http = inject(HttpClient);

  search(q?: string, type?: ResourceType, tag?: string): Observable<SearchHit[]> {
    let params = new HttpParams();
    if (q?.trim()) {
      params = params.set('q', q.trim());
    }
    if (type) {
      params = params.set('type', type);
    }
    if (tag?.trim()) {
      params = params.set('tag', tag.trim());
    }
    return this.http.get<SearchHit[]>(`${environment.apiUrl}/api/search`, { params });
  }
}