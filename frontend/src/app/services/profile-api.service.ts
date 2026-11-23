import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserProfile } from '../models/auth.models';
import { ChangePassword, ProfileUpdate } from '../models/portal.models';

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly http = inject(HttpClient);
  private readonly profileUrl = `${environment.apiUrl}/api/profile`;
  private readonly passwordUrl = `${environment.apiUrl}/api/auth/password`;

  get(): Observable<UserProfile> {
    return this.http.get<UserProfile>(this.profileUrl);
  }

  update(body: ProfileUpdate): Observable<UserProfile> {
    return this.http.put<UserProfile>(this.profileUrl, body);
  }

  changePassword(body: ChangePassword): Observable<void> {
    return this.http.put<void>(this.passwordUrl, body);
  }
}