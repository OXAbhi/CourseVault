import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CourseApiService } from '../../services/course-api.service';
import { UserApiService } from '../../services/user-api.service';
import { Course } from '../../models/portal.models';
import { UserAccount } from '../../models/portal.models';

@Component({
  selector: 'app-admin-courses',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './admin-courses.component.html',
})
export class AdminCoursesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly coursesApi = inject(CourseApiService);
  private readonly usersApi = inject(UserApiService);

  courses: Course[] = [];
  teachers: UserAccount[] = [];

  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    title: ['', Validators.required],
    description: [''],
    teacherId: [0, Validators.required],
  });

  ngOnInit(): void {
    this.reload();
    this.usersApi.list().subscribe({
      next: (users) => {
        this.teachers = users.filter((u) => u.role === 'TEACHER' && u.active);
        if (this.teachers[0]) {
          this.form.patchValue({ teacherId: this.teachers[0].id });
        }
      },
    });
  }

  reload(): void {
    this.coursesApi.list().subscribe({ next: (c) => (this.courses = c) });
  }

  create(): void {
    if (this.form.invalid) {
      return;
    }
    const raw = this.form.getRawValue();
    this.coursesApi
      .create({
        code: raw.code,
        title: raw.title,
        description: raw.description || undefined,
        teacherId: raw.teacherId,
        active: true,
      })
      .subscribe({
        next: () => {
          this.form.patchValue({ code: '', title: '', description: '' });
          this.reload();
        },
      });
  }
}