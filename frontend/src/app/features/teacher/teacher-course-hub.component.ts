import { Component, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { CourseApiService } from '../../services/course-api.service';
import { ResourceApiService } from '../../services/resource-api.service';
import { AssignmentApiService } from '../../services/assignment-api.service';
import { BatchApiService } from '../../services/batch-api.service';
import { QuizApiService } from '../../services/quiz-api.service';
import { Assignment, Batch, Course, ResourceItem, ResourceType } from '../../models/portal.models';
import { Quiz, QuizAttemptSummary, QuizQuestion } from '../../models/quiz.models';
import { datetimeLocalToIso, formatInstant } from '../../shared/utils/date.util';

type QuizDifficulty = 'easy' | 'medium' | 'hard';
type QuizCount = 5 | 10 | 15;

@Component({
  selector: 'app-teacher-course-hub',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './teacher-course-hub.component.html',
  styles: [
    `
      .spinner {
        width: 22px;
        height: 22px;
        border: 2px solid rgba(0, 0, 0, 0.12);
        border-top-color: var(--color-primary, #2563eb);
        border-radius: 50%;
        animation: hub-spin 0.7s linear infinite;
        flex-shrink: 0;
      }
      @keyframes hub-spin {
        to {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class TeacherCourseHubComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly courseApi = inject(CourseApiService);
  private readonly resourceApi = inject(ResourceApiService);
  private readonly assignmentApi = inject(AssignmentApiService);
  private readonly batchApi = inject(BatchApiService);
  private readonly quizApi = inject(QuizApiService);

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
  uploadProgress = 0;

  assignMsg: string | null = null;
  assignError: string | null = null;
  assigning = false;

  quizzes: Quiz[] = [];
  quizPreview: Quiz | null = null;
  quizModalOpen = false;
  quizAttempts: QuizAttemptSummary[] = [];
  attemptsLoading = false;

  generateModalOpen = false;
  generateResource: ResourceItem | null = null;
  generating = false;

  editingQuestionId: number | null = null;
  questionSavingId: number | null = null;
  questionDeletingId: number | null = null;
  quizDeleting = false;

  quizError: string | null = null;
  quizMsg: string | null = null;

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

  readonly generateForm = this.fb.nonNullable.group({
    instructions: ['', Validators.maxLength(1000)],
    pages: [''],
    questionCount: [5 as QuizCount],
    difficulty: ['medium' as QuizDifficulty],
  });

  readonly editQuestionForm = this.fb.nonNullable.group({
    questionText: ['', Validators.required],
    option0: ['', Validators.required],
    option1: ['', Validators.required],
    option2: ['', Validators.required],
    option3: ['', Validators.required],
    correctIndex: [0, Validators.required],
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
    this.reloadQuizzes();
  }

  reloadQuizzes(): void {
    this.quizApi.listForCourse(this.courseId).subscribe({
      next: (q) => (this.quizzes = q),
    });
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

  canGenerateQuiz(resource: ResourceItem): boolean {
    return resource.resourceType !== 'LECTURE';
  }

  openGenerateModal(resource: ResourceItem): void {
    if (this.generating) {
      return;
    }
    this.quizError = null;
    this.generateResource = resource;
    this.generateForm.reset({
      instructions: '',
      pages: '',
      questionCount: 5,
      difficulty: 'medium',
    });
    this.generateModalOpen = true;
  }

  closeGenerateModal(): void {
    if (this.generating) {
      return;
    }
    this.generateModalOpen = false;
    this.generateResource = null;
  }

  submitGenerate(): void {
    if (!this.generateResource || this.generating) {
      return;
    }
    this.quizError = null;
    this.quizMsg = null;
    const raw = this.generateForm.getRawValue();
    const pages = raw.pages.trim();
    if (pages && !/^\d{1,4}(-\d{1,4})?$/.test(pages)) {
      this.quizError = 'Pages must look like 12 or 12-20.';
      return;
    }
    if (this.generateForm.controls.instructions.invalid) {
      this.generateForm.markAllAsTouched();
      this.quizError = 'Instructions must be at most 1000 characters.';
      return;
    }
    this.generating = true;
    const resource = this.generateResource;
    this.quizApi
      .generateFromResource(resource.id, {
        questionCount: raw.questionCount,
        difficulty: raw.difficulty,
        instructions: raw.instructions.trim() || undefined,
        pages: pages || undefined,
      })
      .subscribe({
        next: (quiz) => {
          this.generating = false;
          this.generateModalOpen = false;
          this.generateResource = null;
          this.quizMsg = `Generated ${quiz.questionCount} questions from “${resource.title}”.`;
          this.quizPreview = quiz;
          this.quizAttempts = [];
          this.quizModalOpen = true;
          this.editingQuestionId = null;
          this.reloadQuizzes();
        },
        error: (err: HttpErrorResponse) => {
          this.generating = false;
          this.quizError = extractApiMessage(err) ?? 'Quiz generation failed.';
        },
      });
  }

  openQuiz(quizId: number): void {
    this.quizError = null;
    this.editingQuestionId = null;
    this.quizApi.get(quizId).subscribe({
      next: (q) => {
        this.quizPreview = q;
        this.quizModalOpen = true;
        this.quizAttempts = [];
        if (q.published) {
          this.loadAttempts(q.id);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.quizError = extractApiMessage(err) ?? 'Could not load quiz.';
      },
    });
  }

  loadAttempts(quizId: number): void {
    this.attemptsLoading = true;
    this.quizApi.listAttempts(quizId).subscribe({
      next: (list) => {
        this.quizAttempts = list;
        this.attemptsLoading = false;
      },
      error: () => {
        this.attemptsLoading = false;
      },
    });
  }

  closeQuizModal(): void {
    this.quizModalOpen = false;
    this.quizPreview = null;
    this.quizAttempts = [];
    this.editingQuestionId = null;
  }

  publishQuiz(published: boolean): void {
    if (!this.quizPreview) {
      return;
    }
    this.quizApi.publish(this.quizPreview.id, published).subscribe({
      next: (q) => {
        this.quizPreview = q;
        this.reloadQuizzes();
        this.editingQuestionId = null;
        if (published) {
          this.quizMsg = 'Quiz published — enrolled students were notified.';
          this.loadAttempts(q.id);
        } else {
          this.quizAttempts = [];
          this.quizMsg = 'Quiz unpublished.';
        }
      },
      error: (err: HttpErrorResponse) => {
        this.quizError = extractApiMessage(err) ?? 'Could not update quiz.';
      },
    });
  }

  deleteDraftQuiz(): void {
    if (!this.quizPreview || this.quizPreview.published || this.quizDeleting) {
      return;
    }
    if (!confirm('Delete this draft quiz? This cannot be undone.')) {
      return;
    }
    this.quizDeleting = true;
    this.quizApi.deleteQuiz(this.quizPreview.id).subscribe({
      next: () => {
        this.quizDeleting = false;
        this.closeQuizModal();
        this.reloadQuizzes();
        this.quizMsg = 'Draft quiz deleted.';
      },
      error: (err: HttpErrorResponse) => {
        this.quizDeleting = false;
        this.quizError = extractApiMessage(err) ?? 'Could not delete quiz.';
      },
    });
  }

  startEditQuestion(q: QuizQuestion): void {
    if (!this.quizPreview || this.quizPreview.published) {
      return;
    }
    this.editingQuestionId = q.id;
    this.editQuestionForm.setValue({
      questionText: q.questionText,
      option0: q.options[0] ?? '',
      option1: q.options[1] ?? '',
      option2: q.options[2] ?? '',
      option3: q.options[3] ?? '',
      correctIndex: q.correctIndex,
    });
  }

  cancelEditQuestion(): void {
    this.editingQuestionId = null;
  }

  saveQuestion(): void {
    if (!this.quizPreview || this.editingQuestionId == null) {
      return;
    }
    if (this.editQuestionForm.invalid) {
      this.editQuestionForm.markAllAsTouched();
      return;
    }
    const raw = this.editQuestionForm.getRawValue();
    const qid = this.editingQuestionId;
    this.questionSavingId = qid;
    this.quizApi
      .updateQuestion(this.quizPreview.id, qid, {
        questionText: raw.questionText.trim(),
        options: [raw.option0.trim(), raw.option1.trim(), raw.option2.trim(), raw.option3.trim()],
        correctIndex: raw.correctIndex,
      })
      .subscribe({
        next: (quiz) => {
          this.questionSavingId = null;
          this.editingQuestionId = null;
          this.quizPreview = quiz;
          this.reloadQuizzes();
        },
        error: (err: HttpErrorResponse) => {
          this.questionSavingId = null;
          this.quizError = extractApiMessage(err) ?? 'Could not save question.';
        },
      });
  }

  deleteQuestion(q: QuizQuestion): void {
    if (!this.quizPreview || this.quizPreview.published) {
      return;
    }
    if (!confirm('Remove this question from the draft quiz?')) {
      return;
    }
    this.questionDeletingId = q.id;
    this.quizApi.deleteQuestion(this.quizPreview.id, q.id).subscribe({
      next: (quiz) => {
        this.questionDeletingId = null;
        if (this.editingQuestionId === q.id) {
          this.editingQuestionId = null;
        }
        this.quizPreview = quiz;
        this.reloadQuizzes();
      },
      error: (err: HttpErrorResponse) => {
        this.questionDeletingId = null;
        this.quizError = extractApiMessage(err) ?? 'Could not delete question.';
      },
    });
  }

  optionLabel(index: number): string {
    return String.fromCharCode(65 + index);
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