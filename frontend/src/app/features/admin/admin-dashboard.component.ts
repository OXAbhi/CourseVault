import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page-head">
      <div>
        <p class="page-kicker">Institute admin</p>
        <h1 class="page-title">Nexus Learning Portal</h1>
        <p class="page-lead">Issue accounts, assign teachers, and enroll students. No public sign-up in production.</p>
      </div>
    </div>
    <div class="grid-4" style="margin-bottom: 24px">
      <div class="card stat">
        <div class="label">Users</div>
        <div class="value">—</div>
        <a routerLink="/admin/users" class="btn btn-soft btn-sm" style="margin-top: 12px">Manage</a>
      </div>
      <div class="card stat">
        <div class="label">Courses</div>
        <div class="value">—</div>
        <a routerLink="/admin/courses" class="btn btn-soft btn-sm" style="margin-top: 12px">Manage</a>
      </div>
      <div class="card stat">
        <div class="label">Batches</div>
        <div class="value">—</div>
        <a routerLink="/admin/batches" class="btn btn-soft btn-sm" style="margin-top: 12px">Manage</a>
      </div>
      <div class="card stat">
        <div class="label">Enrollments</div>
        <div class="value">—</div>
        <a routerLink="/admin/enrollments" class="btn btn-soft btn-sm" style="margin-top: 12px">Manage</a>
      </div>
    </div>
    <div class="d-flex flex-wrap gap-2">
      <a routerLink="/admin/users" class="btn btn-outline-primary">Users</a>
      <a routerLink="/admin/courses" class="btn btn-outline-primary">Courses</a>
      <a routerLink="/admin/batches" class="btn btn-outline-primary">Batches</a>
      <a routerLink="/admin/enrollments" class="btn btn-outline-primary">Enrollments</a>
    </div>
  `,
})
export class AdminDashboardComponent {}