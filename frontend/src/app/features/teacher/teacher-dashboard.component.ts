import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-teacher-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page-head">
      <div>
        <p class="page-kicker">Faculty</p>
        <h1 class="page-title">Teacher dashboard</h1>
        <p class="page-lead">Upload materials, create assignments, and grade submissions.</p>
      </div>
      <a routerLink="/teacher/courses" class="btn btn-primary">My courses</a>
    </div>
    <div class="grid-2">
      <div class="card card-pad">
        <h2 class="card-title">Course hub</h2>
        <p class="page-lead">Open a course to upload PDFs or videos and manage assignments.</p>
        <a routerLink="/teacher/courses" class="btn btn-soft btn-sm" style="margin-top: 12px">Go to courses</a>
      </div>
      <div class="card card-pad">
        <h2 class="card-title">AI setup</h2>
        <p class="page-lead">Verify your model and API key before generating quizzes from PDFs.</p>
        <a routerLink="/teacher/ai" class="btn btn-soft btn-sm" style="margin-top: 12px">Test AI connection</a>
      </div>
      <div class="card card-pad">
        <h2 class="card-title">Notifications</h2>
        <p class="page-lead">Student submissions and portal updates.</p>
        <a routerLink="/teacher/notifications" class="btn btn-soft btn-sm" style="margin-top: 12px">Open inbox</a>
      </div>
    </div>
  `,
})
export class TeacherDashboardComponent {}