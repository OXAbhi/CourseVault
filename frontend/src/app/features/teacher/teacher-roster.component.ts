import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BatchApiService } from '../../services/batch-api.service';
import { Enrollment } from '../../models/portal.models';
import { formatInstant } from '../../shared/utils/date.util';

@Component({
  selector: 'app-teacher-roster',
  standalone: true,
  template: `
    <div class="page-head">
      <div>
        <p class="page-kicker">Roster</p>
        <h1 class="page-title">Batch roster</h1>
      </div>
    </div>
    @if (roster.length === 0) {
      <p class="page-lead">No students in this batch.</p>
    } @else {
      <div class="card" style="overflow: hidden">
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Student</th>
                <th>Status</th>
                <th>Enrolled</th>
              </tr>
            </thead>
            <tbody>
              @for (e of roster; track e.id) {
                <tr>
                  <td>{{ e.studentUsername }}</td>
                  <td>{{ e.status }}</td>
                  <td>{{ formatInstant(e.enrolledAt) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    }
  `,
})
export class TeacherRosterComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly batchApi = inject(BatchApiService);
  roster: Enrollment[] = [];
  readonly formatInstant = formatInstant;

  ngOnInit(): void {
    const batchId = Number(this.route.snapshot.paramMap.get('batchId'));
    this.batchApi.enrollments(batchId).subscribe({ next: (e) => (this.roster = e) });
  }
}