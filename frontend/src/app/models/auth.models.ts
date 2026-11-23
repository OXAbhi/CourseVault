export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT';

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  firstName: string;
  lastName: string;
  active: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  role: UserRole;
  profile: UserProfile;
}