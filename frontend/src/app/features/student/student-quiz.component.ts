import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { QuizApiService } from '../../services/quiz-api.service';
import { Quiz, QuizAttempt } from '../../models/quiz.models';

@Component({
  selector: 'app-student-quiz',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    @if (error && !quiz && !result) {
      <div class="setu-alert setu-alert-danger">{{ error }}</div>
      <a class="btn btn-soft" [routerLink]="['/student/courses', courseId]">Back to course</a>
    }

    @if (quiz && !result) {
      <div class="page-head">
        <div>
          <p class="page-kicker">Quiz</p>
          <h1 class="page-title">{{ quiz.title }}</h1>
          <p class="page-lead">{{ quiz.questionCount }} questions · pick one answer each.</p>
        </div>
      </div>
      @if (error) {
        <div class="setu-alert setu-alert-danger">{{ error }}</div>
      }
      <form class="stack" [formGroup]="answersForm" (ngSubmit)="submit()">
        @for (q of quiz.questions; track q.id; let qi = $index) {
          <div class="card card-pad">
            <p style="font-weight: 600; margin: 0 0 12px">Q{{ qi + 1 }}. {{ q.questionText }}</p>
            @for (opt of q.options; track opt; let oi = $index) {
              <label class="row" style="margin: 8px 0; font-weight: 500">
                <input
                  type="radio"
                  [attr.name]="'q' + q.id"
                  [value]="oi"
                  [formControlName]="'' + q.id"
                  style="width: 16px; height: 16px; accent-color: var(--color-primary)"
                />
                {{ optionLabel(oi) }}. {{ opt }}
              </label>
            }
          </div>
        }
        <div class="row" style="gap: 12px">
          <button class="btn btn-primary" type="submit" [disabled]="submitting">
            {{ submitting ? 'Submitting…' : 'Submit answers' }}
          </button>
          <a class="btn btn-ghost" [routerLink]="['/student/courses', quiz.courseId]">Cancel</a>
        </div>
      </form>
    }

    @if (result) {
      <div class="page-head">
        <div>
          <p class="page-kicker">Quiz result</p>
          <h1 class="page-title">{{ result.quizTitle }}</h1>
          <p class="page-lead">
            Score: <strong>{{ result.score }}/{{ result.totalQuestions }}</strong> ({{ percentScore() }}%)
          </p>
        </div>
      </div>
      <div class="stack" style="gap: 16px">
        @for (r of result.results; track r.questionId; let ri = $index) {
          <div class="card card-pad">
            <p style="font-weight: 600; margin: 0 0 8px">
              Q{{ ri + 1 }}. {{ r.questionText }}
              @if (r.correct) {
                <span class="badge badge-success" style="margin-left: 8px">Correct</span>
              } @else {
                <span class="badge badge-warning" style="margin-left: 8px">Wrong</span>
              }
            </p>
            <p class="field hint" style="margin: 0">
              Your answer: {{ optionLabel(r.selectedIndex) }}. {{ r.options[r.selectedIndex] }}
            </p>
            @if (!r.correct) {
              <p class="field hint" style="margin: 4px 0 0">
                Correct: {{ optionLabel(r.correctIndex) }}. {{ r.options[r.correctIndex] }}
              </p>
            }
          </div>
        }
      </div>
      <a class="btn btn-primary" style="margin-top: 16px" [routerLink]="['/student/courses', courseId]">Back to course</a>
    }
  `,
})
export class StudentQuizComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly quizApi = inject(QuizApiService);

  quiz: Quiz | null = null;
  result: QuizAttempt | null = null;
  error: string | null = null;
  submitting = false;

  readonly answersForm = this.fb.group({});

  courseId = 0;

  ngOnInit(): void {
    const quizId = Number(this.route.snapshot.paramMap.get('id'));
    this.quizApi.get(quizId).subscribe({
      next: (q) => {
        this.quiz = q;
        this.courseId = q.courseId;
        for (const question of q.questions) {
          this.answersForm.addControl(String(question.id), this.fb.control<number | null>(null));
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error = extractApiMessage(err) ?? 'Quiz is not available.';
      },
    });
  }

  submit(): void {
    if (!this.quiz || this.submitting) {
      return;
    }
    this.error = null;
    const answers = this.quiz.questions.map((q) => {
      const selected = this.answersForm.get(String(q.id))?.value;
      if (selected == null) {
        return null;
      }
      return { questionId: q.id, selectedIndex: Number(selected) };
    });
    if (answers.some((a) => a == null)) {
      this.error = 'Answer every question before submitting.';
      return;
    }
    this.submitting = true;
    this.quizApi.submitAttempt(this.quiz.id, answers as { questionId: number; selectedIndex: number }[]).subscribe({
      next: (attempt) => {
        this.submitting = false;
        this.result = attempt;
      },
      error: (err: HttpErrorResponse) => {
        this.submitting = false;
        this.error = extractApiMessage(err) ?? 'Could not submit quiz.';
      },
    });
  }

  optionLabel(index: number): string {
    return String.fromCharCode(65 + index);
  }

  percentScore(): number {
    if (!this.result || this.result.totalQuestions === 0) {
      return 0;
    }
    return Math.round((100 * this.result.score) / this.result.totalQuestions);
  }
}

function extractApiMessage(err: HttpErrorResponse): string | null {
  const body = err.error as { message?: string } | null;
  return body?.message ?? null;
}