import { Routes } from '@angular/router';
import { ShellComponent } from '../../shared/shell/shell.component';
import { ProfilePageComponent } from '../../shared/profile/profile-page.component';
import { NotificationsPageComponent } from '../../shared/notifications/notifications-page.component';
import { TeacherDashboardComponent } from './teacher-dashboard.component';
import { TeacherCoursesComponent } from './teacher-courses.component';
import { TeacherCourseHubComponent } from './teacher-course-hub.component';
import { TeacherGradeComponent } from './teacher-grade.component';
import { TeacherRosterComponent } from './teacher-roster.component';

export const TEACHER_ROUTES: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: TeacherDashboardComponent },
      { path: 'courses', component: TeacherCoursesComponent },
      { path: 'courses/:id', component: TeacherCourseHubComponent },
      { path: 'courses/:id/roster/:batchId', component: TeacherRosterComponent },
      { path: 'assignments/:id/grade', component: TeacherGradeComponent },
      { path: 'notifications', component: NotificationsPageComponent },
      { path: 'profile', component: ProfilePageComponent },
    ],
  },
];