import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificationApiService } from '../../services/notification-api.service';
import { UserRole } from '../../models/auth.models';

interface NavLink {
  label: string;
  path: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notificationApi = inject(NotificationApiService);

  readonly profile = this.auth.getProfile();
  readonly role = this.auth.getRole();

  sidebarOpen = false;
  unread = 0;
  searchQuery = '';

  ngOnInit(): void {
    if (this.notificationsLink()) {
      this.notificationApi.list().subscribe({
        next: (items) => (this.unread = items.filter((n) => !n.read).length),
      });
    }
  }

  navLinks(): NavLink[] {
    const role = this.role;
    if (role === 'STUDENT') {
      return [
        { label: 'Dashboard', path: '/student/dashboard' },
        { label: 'My courses', path: '/student/courses' },
        { label: 'Search', path: '/student/search' },
        { label: 'Notifications', path: '/student/notifications' },
        { label: 'Profile', path: '/student/profile' },
      ];
    }
    if (role === 'TEACHER') {
      return [
        { label: 'Dashboard', path: '/teacher/dashboard' },
        { label: 'My courses', path: '/teacher/courses' },
        { label: 'AI setup', path: '/teacher/ai' },
        { label: 'Notifications', path: '/teacher/notifications' },
        { label: 'Profile', path: '/teacher/profile' },
      ];
    }
    if (role === 'ADMIN') {
      return [
        { label: 'Dashboard', path: '/admin/dashboard' },
        { label: 'Users', path: '/admin/users' },
        { label: 'Courses', path: '/admin/courses' },
        { label: 'Batches', path: '/admin/batches' },
        { label: 'Enrollments', path: '/admin/enrollments' },
        { label: 'Profile', path: '/admin/profile' },
      ];
    }
    return [];
  }

  rolePill(): string {
    return (this.role ?? 'user').toLowerCase();
  }

  initials(): string {
    const first = this.profile?.firstName?.charAt(0) ?? '';
    const last = this.profile?.lastName?.charAt(0) ?? '';
    return (first + last).toUpperCase() || 'U';
  }

  displayName(): string {
    if (!this.profile) {
      return 'User';
    }
    return `${this.profile.firstName} ${this.profile.lastName}`.trim();
  }

  profileLink(): string {
    const role = this.role?.toLowerCase();
    return role ? `/${role}/profile` : '/login';
  }

  homeLink(): string {
    return this.auth.getHomeRoute();
  }

  notificationsLink(): string | null {
    switch (this.role) {
      case 'STUDENT':
        return '/student/notifications';
      case 'TEACHER':
        return '/teacher/notifications';
      default:
        return null;
    }
  }

  chipLabel(): string {
    if (this.role === 'TEACHER') {
      return 'Faculty · AY 2024–25';
    }
    return 'Nexus Institute · AY 2024–25';
  }

  searchEnabled(): boolean {
    return this.role === 'STUDENT';
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  onSearchSubmit(ev: Event): void {
    ev.preventDefault();
    if (!this.searchEnabled()) {
      return;
    }
    const q = this.searchQuery.trim();
    void this.router.navigate(['/student/search'], q ? { queryParams: { q } } : {});
    this.closeSidebar();
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}