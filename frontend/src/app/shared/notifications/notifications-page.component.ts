import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NotificationApiService } from '../../services/notification-api.service';
import { NotificationItem } from '../../models/portal.models';
import { formatInstant } from '../utils/date.util';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './notifications-page.component.html',
})
export class NotificationsPageComponent implements OnInit {
  private readonly api = inject(NotificationApiService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  items: NotificationItem[] = [];
  loading = true;
  readonly formatInstant = formatInstant;

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.api.list().subscribe({
      next: (list) => {
        this.items = list;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  open(item: NotificationItem): void {
    if (!item.read) {
      this.api.markRead(item.id).subscribe({
        next: () => (item.read = true),
      });
    }
    const path = this.resolveLink(item.link);
    if (path) {
      void this.router.navigate(path);
    }
  }

  private resolveLink(link: string | null): string[] | null {
    if (!link) {
      return null;
    }
    const role = this.auth.getRole();
    const prefix =
      role === 'STUDENT' ? '/student' : role === 'TEACHER' ? '/teacher' : '/admin';
    if (link.startsWith('/resources/')) {
      const id = link.replace('/resources/', '');
      if (role === 'STUDENT') {
        return [`${prefix}/resources`, id];
      }
      return null;
    }
    if (link.startsWith('/assignments/')) {
      const id = link.replace('/assignments/', '');
      if (role === 'STUDENT') {
        return [`${prefix}/assignments`, id];
      }
      if (role === 'TEACHER') {
        return [`${prefix}/assignments`, id, 'grade'];
      }
    }
    if (link.startsWith('/student/quizzes/')) {
      const id = link.replace('/student/quizzes/', '');
      if (role === 'STUDENT') {
        return [`${prefix}/quizzes`, id];
      }
    }
    return null;
  }
}