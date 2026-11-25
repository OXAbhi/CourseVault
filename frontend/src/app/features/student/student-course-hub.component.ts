import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CourseApiService } from '../../services/course-api.service';
import { ResourceApiService } from '../../services/resource-api.service';
import { AssignmentApiService } from '../../services/assignment-api.service';
import { QuizApiService } from '../../services/quiz-api.service';
import { Course, ResourceItem, Assignment } from '../../models/portal.models';
import { Quiz } from '../../models/quiz.models';
import { formatInstant } from '../../shared/utils/date.util';

type HubTab = 'notes' | 'lectures' | 'assignments' | 'quizzes';

@Component({
  selector: 'app-student-course-hub',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './student-course-hub.component.html',
})
export class StudentCourseHubComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly courseApi = inject(CourseApiService);
  private readonly resourceApi = inject(ResourceApiService);
  private readonly assignmentApi = inject(AssignmentApiService);
  private readonly quizApi = inject(QuizApiService);

  courseId = 0;
  course: Course | null = null;
  resources: ResourceItem[] = [];
  assignments: Assignment[] = [];
  quizzes: Quiz[] = [];
  activeTab: HubTab = 'notes';
  readonly formatInstant = formatInstant;

  ngOnInit(): void {
    this.courseId = Number(this.route.snapshot.paramMap.get('id'));
    this.courseApi.get(this.courseId).subscribe({ next: (c) => (this.course = c) });
    this.resourceApi.listForCourse(this.courseId).subscribe({ next: (r) => (this.resources = r) });
    this.assignmentApi.listForCourse(this.courseId).subscribe({ next: (a) => (this.assignments = a) });
    this.quizApi.listForCourse(this.courseId).subscribe({ next: (q) => (this.quizzes = q) });
  }

  setTab(tab: HubTab): void {
    this.activeTab = tab;
  }

  notesResources(): ResourceItem[] {
    return this.resources.filter((r) => r.resourceType === 'NOTE' || r.resourceType === 'OTHER');
  }

  lectureResources(): ResourceItem[] {
    return this.resources.filter((r) => r.resourceType === 'LECTURE');
  }

  fileIcon(resource: ResourceItem): string {
    return resource.resourceType === 'LECTURE' ? 'MP4' : 'PDF';
  }
}