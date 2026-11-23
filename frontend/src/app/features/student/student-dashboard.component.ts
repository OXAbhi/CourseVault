import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationApiService } from '../../services/notification-api.service';
import { CourseApiService } from '../../services/course-api.service';
import { Course, NotificationItem } from '../../models/portal.models';

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './student-dashboard.component.html',
})
export class StudentDashboardComponent implements OnInit {
  private readonly notifications = inject(NotificationApiService);
  private readonly courseApi = inject(CourseApiService);

  unread = 0;
  courseCount = 0;
  courses: Course[] = [];
  recentNotifications: NotificationItem[] = [];
  greeting = 'Hello';

  ngOnInit(): void {
    const hour = new Date().getHours();
    if (hour < 12) {
      this.greeting = 'Good morning';
    } else if (hour < 17) {
      this.greeting = 'Good afternoon';
    } else {
      this.greeting = 'Good evening';
    }

    this.notifications.list().subscribe({
      next: (n) => {
        this.unread = n.filter((x) => !x.read).length;
        this.recentNotifications = n.slice(0, 3);
      },
    });
    this.courseApi.list().subscribe({
      next: (c) => {
        this.courses = c;
        this.courseCount = c.length;
      },
    });
  }
}