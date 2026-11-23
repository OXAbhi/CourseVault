import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { AssignmentApiService } from '../../services/assignment-api.service';
import { Assignment } from '../../models/portal.models';
import { formatInstant } from '../../shared/utils/date.util';

@Component({
  selector: 'app-student-assignment-detail',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './student-assignment-detail.component.html',
})
export class StudentAssignmentDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly assignmentApi = inject(AssignmentApiService);
  private readonly fb = inject(FormBuilder);

  assignment: Assignment | null = null;
  message: string | null = null;
  error: string | null = null;
  readonly formatInstant = formatInstant;

  readonly fileControl = this.fb.control<File | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.assignmentApi.get(id).subscribe({
      next: (a) => (this.assignment = a),
      error: () => (this.error = 'Assignment not available'),
    });
  }

  onFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    this.fileControl.setValue(input.files?.[0] ?? null);
  }

  submit(): void {
    const file = this.fileControl.value;
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!file) {
      this.error = 'Choose a file';
      return;
    }
    this.error = null;
    this.assignmentApi.submit(id, file).subscribe({
      next: () => {
        this.message = 'Submission saved.';
        this.load();
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Submit failed (check deadline).';
      },
    });
  }
}