import { Component, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { CourseApiService } from '../../services/course-api.service';
import { ResourceApiService } from '../../services/resource-api.service';
import { AssignmentApiService } from '../../services/assignment-api.service';
import { BatchApiService } from '../../services/batch-api.service';
import { Assignment, Batch, Course, ResourceItem, ResourceType } from '../../models/portal.models';
import { datetimeLocalToIso, formatInstant } from '../../shared/utils/date.util';

@Component({
  selector: 'app-teacher-course-hub',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './teacher-course-hub.component.html',
})
export class TeacherCourseHubComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly courseApi = inject(CourseApiService);
  private readonly resourceApi = inject(ResourceApiService);
  private readonly assignmentApi = inject(AssignmentApiService);
  private readonly batchApi = inject(BatchApiService);

  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  courseId = 0;
  course: Course | null = null;
  resources: ResourceItem[] = [];
  assignments: Assignment[] = [];
  batches: Batch[] = [];

  selectedFile: File | null = null;
  uploadMsg: string | null = null;
  uploadError: string | null = null;
  uploading = false;
  uploadModalOpen = false;
  /** 0–100 while bytes are sent to the API; 100 + uploading means server/Cloudinary is finishing. */
  uploadProgress = 0;

  assignMsg: string | null = null;
  assignError: string | null = null;
  assigning = false;

  readonly formatInstant = formatInstant;

  readonly uploadForm = this.fb.nonNullable.group({
    title: ['', Validators.required],
    resourceType: ['NOTE' as ResourceType],
    tags: [''],
    published: [true],
  });

  readonly assignForm = this.fb.nonNullable.group({
    title: ['', Validators.required],
    description: [''],
    dueAt: ['', Validators.required],
    maxMarks: [20],
    published: [true],
  });

  get uploadStatusLabel(): string {
    if (!this.uploading) {
      return '';
    }
    if (this.uploadProgress >= 100) {
      return 'Saving to course…';
    }
    return 'Sending file…';
  }

  ngOnInit(): void {
    this.courseId = Number(this.route.snapshot.paramMap.get('id'));
    this.courseApi.get(this.courseId).subscribe({ next: (c) => (this.course = c) });
    this.reloadResources();
    this.assignmentApi.listForCourse(this.courseId).subscribe({ next: (a) => (this.assignments = a) });
    this.batchApi.listForCourse(this.courseId).subscribe({ next: (b) => (this.batches = b) });
  }

  reloadResources(): void {
    this.resourceApi.listForCourse(this.courseId).subscribe({
      next: (r) => (this.resources = r),
    });
  }

  openUploadModal(): void {
    this.uploadError = null;
    this.uploadModalOpen = true;
  }

  closeUploadModal(): void {
    if (this.uploading) {
      return;
    }
    this.uploadModalOpen = false;
    this.uploadError = null;
  }

  openFilePicker(): void {
    this.fileInput?.nativeElement.click();
  }

  onFileDrop(ev: DragEvent): void {
    ev.preventDefault();
    if (this.uploading) {
      return;
    }
    const f = ev.dataTransfer?.files?.[0] ?? null;
    if (!f) {
      return;
    }
    this.selectedFile = f;
    this.uploadError = null;
    if (!this.uploadForm.controls.title.value.trim()) {
      this.uploadForm.patchValue({ title: titleFromFileName(f.name) });
    }
  }

  onUploadFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const f = input.files?.[0] ?? null;
    this.selectedFile = f;
    this.uploadError = null;
    if (f && !this.uploadForm.controls.title.value.trim()) {
      this.uploadForm.patchValue({ title: titleFromFileName(f.name) });
    }
  }

  clearSelectedFile(): void {
    this.selectedFile = null;
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  upload(): void {
    this.uploadError = null;
    if (this.uploadForm.invalid) {
      this.uploadForm.markAllAsTouched();
      this.uploadError = 'Enter a title for this material.';
      return;
    }
    if (!this.selectedFile) {
      this.uploadError = 'Choose a file before uploading.';
      return;
    }
    const raw = this.uploadForm.getRawValue();
    this.uploading = true;
    this.uploadProgress = 0;
    this.resourceApi
      .uploadWithProgress(
        this.courseId,
        this.selectedFile,
        raw.title.trim(),
        raw.resourceType,
        raw.tags,
        raw.published
      )
      .subscribe({
        next: (event) => {
          if (event.type === HttpEventType.UploadProgress) {
            const total = event.total ?? 0;
            this.uploadProgress = total > 0 ? Math.round((100 * event.loaded) / total) : this.uploadProgress;
            return;
          }
          if (event.type === HttpEventType.Response && event.body) {
            const created = event.body;
            this.uploading = false;
            this.uploadProgress = 100;
            this.uploadMsg = `Uploaded “${created.title}” successfully.`;
            this.uploadForm.patchValue({ title: '', tags: '', resourceType: 'NOTE', published: true });
            this.clearSelectedFile();
            this.uploadModalOpen = false;
            this.reloadResources();
          }
        },
        error: (err: HttpErrorResponse) => {
          this.uploading = false;
          this.uploadProgress = 0;
          this.uploadError =
            extractApiMessage(err) ?? 'Upload failed. Check file type (PDF, PPT, MP4) and API logs.';
        },
      });
  }

  createAssignment(): void {
    this.assignError = null;
    this.assignMsg = null;
    if (this.assignForm.invalid) {
      this.assignForm.markAllAsTouched();
      this.assignError = 'Title and due date are required.';
      return;
    }
    const raw = this.assignForm.getRawValue();
    this.assigning = true;
    this.assignmentApi
      .create(this.courseId, {
        title: raw.title,
        description: raw.description || undefined,
        dueAt: datetimeLocalToIso(raw.dueAt),
        maxMarks: raw.maxMarks,
        published: raw.published,
      })
      .subscribe({
        next: () => {
          this.assigning = false;
          this.assignMsg = 'Assignment created.';
          this.assignForm.patchValue({ title: '', description: '', dueAt: '' });
          this.assignmentApi.listForCourse(this.courseId).subscribe({ next: (a) => (this.assignments = a) });
        },
        error: (err: HttpErrorResponse) => {
          this.assigning = false;
          this.assignError = extractApiMessage(err) ?? 'Could not create assignment.';
        },
      });
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}

function titleFromFileName(name: string): string {
  const base = name.replace(/\.[^.]+$/, '').replace(/[-_]/g, ' ').trim();
  return base || name;
}

function extractApiMessage(err: HttpErrorResponse): string | null {
  const body = err.error as { message?: string } | null;
  return body?.message ?? null;
}