import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CourseApiService } from '../../services/course-api.service';
import { Course } from '../../models/portal.models';

@Component({
  selector: 'app-teacher-courses',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page-head">
      <div>
        <p class="page-kicker">Teaching</p>
        <h1 class="page-title">My courses</h1>
      </div>
    </div>
    @if (courses.length === 0) {
      <p class="page-lead">No courses assigned yet.</p>
    } @else {
      <div class="grid-2">
        @for (c of courses; track c.id) {
          <a class="card course-card" [routerLink]="['/teacher/courses', c.id]">
            <span class="course-code">{{ c.code }}</span>
            <h3>{{ c.title }}</h3>
            <p class="course-meta">Manage materials & assignments</p>
            <div class="course-foot">
              <span>Open course hub</span>
              <span class="badge badge-primary">Faculty</span>
            </div>
          </a>
        }
      </div>
    }
  `,
})
export class TeacherCoursesComponent implements OnInit {
  private readonly courseApi = inject(CourseApiService);
  courses: Course[] = [];

  ngOnInit(): void {
    this.courseApi.list().subscribe({ next: (c) => (this.courses = c) });
  }
}