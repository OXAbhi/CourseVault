import { Component, inject, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AiApiService } from '../../services/ai-api.service';
import { AiStatusResponse, AiTestResponse } from '../../models/ai.models';

@Component({
  selector: 'app-teacher-ai',
  standalone: true,
  template: `
    <div class="page-head">
      <div>
        <p class="page-kicker">Faculty · AI</p>
        <h1 class="page-title">AI connection</h1>
        <p class="page-lead">
          Check that your API key and model are loaded locally before quiz generation.
        </p>
      </div>
    </div>

    <div class="card card-pad" style="max-width: 640px">
      <h2 class="card-title">Configuration</h2>
      @if (loadingStatus) {
        <p class="page-lead">Loading status…</p>
      } @else if (statusError) {
        <p class="form-error">{{ statusError }}</p>
        <button type="button" class="btn btn-soft btn-sm" (click)="loadStatus()">Retry</button>
      } @else if (status) {
        <dl class="ai-dl">
          <div>
            <dt>API key</dt>
            <dd>
              @if (status.configured) {
                <span class="badge badge-primary">Configured</span>
              } @else {
                <span class="badge">Not set</span>
              }
            </dd>
          </div>
          <div>
            <dt>Model</dt>
            <dd>{{ status.model || '—' }}</dd>
          </div>
          <div>
            <dt>API base URL</dt>
            <dd>{{ status.baseUrl || '—' }}</dd>
          </div>
          @if (status.keyPrefix) {
            <div>
              <dt>Key prefix (loaded in JVM)</dt>
              <dd>
                <code>{{ status.keyPrefix }}</code>
                @if (!status.keyPrefix.startsWith('sk-or-v1')) {
                  <span class="form-error" style="display: block; margin-top: 6px">
                    OpenRouter needs a key starting with <code>sk-or-v1</code>. If this shows your old key, remove
                    Windows / IDE <code>OPENAI_API_KEY</code> and restart the API.
                  </span>
                }
              </dd>
            </div>
          }
        </dl>
        @if (!status.configured) {
          <p class="page-lead" style="margin-top: 12px">
            Add <code>openai.api-key</code> to <code>application-local.properties</code> and restart the API.
          </p>
        }
        @if (status?.configured) {
          <p class="page-lead" style="margin-top: 12px; margin-bottom: 0">
            Model still wrong after editing the file? Windows/IDE env vars
            <code>OPENAI_MODEL</code> and <code>OPENAI_API_KEY</code> override
            <code>application-local.properties</code>. Remove them or match your local file, then restart the API.
          </p>
        }
      }

      <div style="margin-top: 20px; display: flex; flex-wrap: gap: 12px; align-items: center">
        <button
          type="button"
          class="btn btn-primary"
          [disabled]="testing || !status?.configured"
          (click)="runTest()"
        >
          {{ testing ? 'Testing…' : 'Run connection test' }}
        </button>
        <button type="button" class="btn btn-soft btn-sm" [disabled]="loadingStatus" (click)="loadStatus()">
          Refresh status
        </button>
      </div>

      @if (testError) {
        <p class="form-error" style="margin-top: 16px">{{ testError }}</p>
      }
      @if (testResult) {
        <div class="ai-reply" style="margin-top: 16px">
          <p class="form-label">Model reply</p>
          <p>{{ testResult.reply }}</p>
          <p class="page-lead" style="margin-top: 8px; margin-bottom: 0">
            A normal word (e.g. hello) means OpenRouter is working. If you only see
            <code>refusal</code>, restart the API after the latest code, or switch
            <code>openai.model</code> to <code>qwen/qwen-2.5-7b-instruct</code> on OpenRouter.
          </p>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .ai-dl {
        margin: 0;
        display: grid;
        gap: 12px;
      }
      .ai-dl dt {
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        opacity: 0.7;
      }
      .ai-dl dd {
        margin: 4px 0 0;
        font-weight: 500;
      }
      .ai-reply {
        padding: 12px 14px;
        border-radius: 8px;
        background: var(--surface-2, rgba(0, 0, 0, 0.04));
      }
      code {
        font-size: 0.9em;
      }
    `,
  ],
})
export class TeacherAiComponent implements OnInit {
  private readonly aiApi = inject(AiApiService);

  loadingStatus = false;
  status: AiStatusResponse | null = null;
  statusError: string | null = null;

  testing = false;
  testResult: AiTestResponse | null = null;
  testError: string | null = null;

  ngOnInit(): void {
    this.loadStatus();
  }

  loadStatus(): void {
    this.loadingStatus = true;
    this.statusError = null;
    this.aiApi.getStatus().subscribe({
      next: (s) => {
        this.status = s;
        this.loadingStatus = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loadingStatus = false;
        this.statusError = extractApiMessage(err) ?? `Could not load AI status (${err.status}).`;
      },
    });
  }

  runTest(): void {
    this.testing = true;
    this.testError = null;
    this.testResult = null;
    this.aiApi.testConnection().subscribe({
      next: (r) => {
        this.testResult = r;
        this.testing = false;
      },
      error: (err: HttpErrorResponse) => {
        this.testing = false;
        this.testError = extractApiMessage(err) ?? `Connection test failed (${err.status}).`;
      },
    });
  }
}

function extractApiMessage(err: HttpErrorResponse): string | null {
  const body = err.error as { message?: string } | null;
  return body?.message ?? null;
}