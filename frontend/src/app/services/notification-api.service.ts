import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { NotificationItem } from '../models/portal.models';

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/notifications`;

  list(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(this.base);
  }

  markRead(id: number): Observable<NotificationItem> {
    return this.http.patch<NotificationItem>(`${this.base}/${id}/read`, {});
  }
}