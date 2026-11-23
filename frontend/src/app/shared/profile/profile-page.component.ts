import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProfileApiService } from '../../services/profile-api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './profile-page.component.html',
})
export class ProfilePageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly profileApi = inject(ProfileApiService);
  private readonly auth = inject(AuthService);

  profileMessage: string | null = null;
  passwordMessage: string | null = null;
  profileError: string | null = null;
  passwordError: string | null = null;

  readonly profileForm = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
  });

  readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    this.profileApi.get().subscribe({
      next: (p) => {
        this.profileForm.patchValue({
          firstName: p.firstName,
          lastName: p.lastName,
          email: p.email,
        });
      },
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      return;
    }
    this.profileError = null;
    this.profileMessage = null;
    this.profileApi.update(this.profileForm.getRawValue()).subscribe({
      next: (p) => {
        sessionStorage.setItem('learning_portal_profile', JSON.stringify(p));
        this.profileMessage = 'Profile saved.';
      },
      error: () => (this.profileError = 'Could not update profile.'),
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      return;
    }
    this.passwordError = null;
    this.passwordMessage = null;
    this.profileApi.changePassword(this.passwordForm.getRawValue()).subscribe({
      next: () => {
        this.passwordMessage = 'Password updated.';
        this.passwordForm.reset();
      },
      error: () => (this.passwordError = 'Current password incorrect or policy failed.'),
    });
  }
}