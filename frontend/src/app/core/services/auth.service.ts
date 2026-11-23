import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, UserProfile, UserRole } from '../../models/auth.models';

const TOKEN_KEY = 'learning_portal_token';
const PROFILE_KEY = 'learning_portal_profile';
const ROLE_KEY = 'learning_portal_role';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/api/auth/login`, credentials)
      .pipe(tap((response) => this.persistSession(response)));
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(PROFILE_KEY);
    sessionStorage.removeItem(ROLE_KEY);
  }

  getToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  getRole(): UserRole | null {
    const role = sessionStorage.getItem(ROLE_KEY);
    return role as UserRole | null;
  }

  getProfile(): UserProfile | null {
    const raw = sessionStorage.getItem(PROFILE_KEY);
    return raw ? (JSON.parse(raw) as UserProfile) : null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getHomeRoute(): string {
    switch (this.getRole()) {
      case 'ADMIN':
        return '/admin/dashboard';
      case 'TEACHER':
        return '/teacher/dashboard';
      case 'STUDENT':
        return '/student/dashboard';
      default:
        return '/login';
    }
  }

  navigateHome(): void {
    void this.router.navigateByUrl(this.getHomeRoute());
  }

  private persistSession(response: LoginResponse): void {
    sessionStorage.setItem(TOKEN_KEY, response.accessToken);
    sessionStorage.setItem(ROLE_KEY, response.role);
    sessionStorage.setItem(PROFILE_KEY, JSON.stringify(response.profile));
  }
}