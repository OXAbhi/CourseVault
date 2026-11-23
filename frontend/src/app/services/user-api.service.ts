import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserAccount, UserCreate } from '../models/portal.models';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/users`;

  list(): Observable<UserAccount[]> {
    return this.http.get<UserAccount[]>(this.base);
  }

  create(body: UserCreate): Observable<UserAccount> {
    return this.http.post<UserAccount>(this.base, body);
  }

  setActive(id: number, active: boolean): Observable<UserAccount> {
    return this.http.patch<UserAccount>(`${this.base}/${id}/status`, { active });
  }
}