import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AiStatusResponse, AiTestResponse } from '../models/ai.models';

@Injectable({ providedIn: 'root' })
export class AiApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/ai`;

  getStatus(): Observable<AiStatusResponse> {
    return this.http.get<AiStatusResponse>(`${this.base}/status`);
  }

  testConnection(): Observable<AiTestResponse> {
    return this.http.post<AiTestResponse>(`${this.base}/test`, {});
  }
}