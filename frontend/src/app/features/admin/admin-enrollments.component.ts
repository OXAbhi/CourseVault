import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EnrollmentApiService } from '../../services/enrollment-api.service';
import { UserApiService } from '../../services/user-api.service';
import { CourseApiService } from '../../services/course-api.service';
import { BatchApiService } from '../../services/batch-api.service';
import { Enrollment, UserAccount, Batch } from '../../models/portal.models';
import { formatInstant } from '../../shared/utils/date.util';

@Component({
  selector: 'app-admin-enrollments',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './admin-enrollments.component.html',
})
export class AdminEnrollmentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly enrollmentApi = inject(EnrollmentApiService);
  private readonly userApi = inject(UserApiService);
  private readonly courseApi = inject(CourseApiService);
  private readonly batchApi = inject(BatchApiService);

  enrollments: Enrollment[] = [];
  students: UserAccount[] = [];
  batches: Batch[] = [];
  readonly formatInstant = formatInstant;

  readonly form = this.fb.nonNullable.group({
    studentId: [0, Validators.required],
    batchId: [0, Validators.required],
  });

  ngOnInit(): void {
    this.reload();
    this.userApi.list().subscribe({
      next: (u) => {
        this.students = u.filter((x) => x.role === 'STUDENT' && x.active);
        if (this.students[0]) {
          this.form.patchValue({ studentId: this.students[0].id });
        }
      },
    });
    this.courseApi.list().subscribe({
      next: async (courses) => {
        const all: Batch[] = [];
        for (const c of courses) {
          const list = await new Promise<Batch[]>((resolve) =>
            this.batchApi.listForCourse(c.id).subscribe({ next: resolve })
          );
          all.push(...list);
        }
        this.batches = all;
        if (all[0]) {
          this.form.patchValue({ batchId: all[0].id });
        }
      },
    });
  }

  reload(): void {
    this.enrollmentApi.list().subscribe({ next: (e) => (this.enrollments = e) });
  }

  create(): void {
    const { studentId, batchId } = this.form.getRawValue();
    this.enrollmentApi.create(studentId, batchId).subscribe({ next: () => this.reload() });
  }
}