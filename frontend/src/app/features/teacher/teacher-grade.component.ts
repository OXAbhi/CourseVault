import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AssignmentApiService } from '../../services/assignment-api.service';
import { Assignment, Submission } from '../../models/portal.models';
import { formatInstant } from '../../shared/utils/date.util';

@Component({
  selector: 'app-teacher-grade',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './teacher-grade.component.html',
})
export class TeacherGradeComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly assignmentApi = inject(AssignmentApiService);

  assignment: Assignment | null = null;
  submissions: Submission[] = [];
  selectedId: number | null = null;
  message: string | null = null;
  readonly formatInstant = formatInstant;

  readonly gradeForm = this.fb.nonNullable.group({
    marks: [0, [Validators.required, Validators.min(0)]],
    feedback: [''],
  });

  ngOnInit(): void {
    const assignmentId = Number(this.route.snapshot.paramMap.get('id'));
    this.assignmentApi.get(assignmentId).subscribe({ next: (a) => (this.assignment = a) });
    this.assignmentApi.listSubmissions(assignmentId).subscribe({
      next: (s) => (this.submissions = s),
    });
  }

  select(s: Submission): void {
    this.selectedId = s.id;
    this.gradeForm.patchValue({
      marks: s.marks ?? 0,
      feedback: s.feedback ?? '',
    });
  }

  submitGrade(): void {
    if (this.selectedId == null || this.gradeForm.invalid) {
      return;
    }
    const { marks, feedback } = this.gradeForm.getRawValue();
    this.assignmentApi.grade(this.selectedId, marks, feedback).subscribe({
      next: () => {
        this.message = 'Grade saved.';
        const assignmentId = Number(this.route.snapshot.paramMap.get('id'));
        this.assignmentApi.listSubmissions(assignmentId).subscribe({
          next: (s) => (this.submissions = s),
        });
      },
    });
  }
}