import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { LandingComponent } from './features/auth/landing/landing.component';
import { LoginComponent } from './features/auth/login/login.component';
import { ShellComponent } from './shared/shell/shell.component';
import { ProfilePageComponent } from './shared/profile/profile-page.component';
import { NotificationsPageComponent } from './shared/notifications/notifications-page.component';
import { AdminDashboardComponent } from './features/admin/admin-dashboard.component';
import { AdminUsersComponent } from './features/admin/admin-users.component';
import { AdminCoursesComponent } from './features/admin/admin-courses.component';
import { AdminBatchesComponent } from './features/admin/admin-batches.component';
import { AdminEnrollmentsComponent } from './features/admin/admin-enrollments.component';
import { StudentDashboardComponent } from './features/student/student-dashboard.component';
import { StudentCoursesComponent } from './features/student/student-courses.component';
import { StudentCourseHubComponent } from './features/student/student-course-hub.component';
import { StudentResourceViewComponent } from './features/student/student-resource-view.component';
import { StudentAssignmentDetailComponent } from './features/student/student-assignment-detail.component';
import { StudentSearchPageComponent } from './features/student/search-page.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  {
    path: 'student',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['STUDENT'] },
    children: [
      {
        path: '',
        component: ShellComponent,
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
          { path: 'dashboard', component: StudentDashboardComponent },
          { path: 'courses', component: StudentCoursesComponent },
          { path: 'courses/:id', component: StudentCourseHubComponent },
          { path: 'resources/:id', component: StudentResourceViewComponent },
          { path: 'assignments/:id', component: StudentAssignmentDetailComponent },
          { path: 'search', component: StudentSearchPageComponent },
          { path: 'notifications', component: NotificationsPageComponent },
          { path: 'profile', component: ProfilePageComponent },
        ],
      },
    ],
  },
  {
    path: 'teacher',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['TEACHER'] },
    loadChildren: () =>
      import('./features/teacher/teacher.routes').then((m) => m.TEACHER_ROUTES),
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    children: [
      {
        path: '',
        component: ShellComponent,
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
          { path: 'dashboard', component: AdminDashboardComponent },
          { path: 'users', component: AdminUsersComponent },
          { path: 'courses', component: AdminCoursesComponent },
          { path: 'batches', component: AdminBatchesComponent },
          { path: 'enrollments', component: AdminEnrollmentsComponent },
          { path: 'profile', component: ProfilePageComponent },
        ],
      },
    ],
  },
  { path: '**', redirectTo: '' },
];