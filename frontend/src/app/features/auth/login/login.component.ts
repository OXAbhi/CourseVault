import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  errorMessage: string | null = null;
  submitting = false;

  readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage = null;
    this.submitting = true;
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.submitting = false;
        this.auth.navigateHome();
      },
      error: () => {
        this.submitting = false;
        this.errorMessage = 'Invalid username or password.';
      },
    });
  }
}