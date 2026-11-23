import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BatchApiService } from '../../services/batch-api.service';
import { CourseApiService } from '../../services/course-api.service';
import { Batch, Course } from '../../models/portal.models';

@Component({
  selector: 'app-admin-batches',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './admin-batches.component.html',
})
export class AdminBatchesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly batchApi = inject(BatchApiService);
  private readonly courseApi = inject(CourseApiService);

  courses: Course[] = [];
  batches: Batch[] = [];
  courseId = 0;

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    academicYear: ['2025-26', Validators.required],
  });

  ngOnInit(): void {
    this.courseApi.list().subscribe({
      next: (c) => {
        this.courses = c;
        if (c[0]) {
          this.selectCourse(c[0].id);
        }
      },
    });
  }

  selectCourse(id: number): void {
    this.courseId = id;
    this.batchApi.listForCourse(id).subscribe({ next: (b) => (this.batches = b) });
  }

  create(): void {
    if (!this.courseId || this.form.invalid) {
      return;
    }
    this.batchApi.create(this.courseId, this.form.getRawValue()).subscribe({
      next: () => {
        this.form.patchValue({ name: '' });
        this.selectCourse(this.courseId);
      },
    });
  }
}