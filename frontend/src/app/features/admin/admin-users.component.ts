import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserApiService } from '../../services/user-api.service';
import { UserAccount, UserCreate } from '../../models/portal.models';
import { UserRole } from '../../models/auth.models';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './admin-users.component.html',
})
export class AdminUsersComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly usersApi = inject(UserApiService);

  users: UserAccount[] = [];
  error: string | null = null;

  readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['TEACHER' as UserRole, Validators.required],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.usersApi.list().subscribe({
      next: (u) => (this.users = u),
      error: () => (this.error = 'Failed to load users'),
    });
  }

  create(): void {
    if (this.form.invalid) {
      return;
    }
    this.error = null;
    this.usersApi.create(this.form.getRawValue() as UserCreate).subscribe({
      next: () => {
        this.form.reset({ role: 'TEACHER' });
        this.reload();
      },
      error: () => (this.error = 'Could not create user'),
    });
  }

  toggleActive(u: UserAccount): void {
    this.usersApi.setActive(u.id, !u.active).subscribe({ next: () => this.reload() });
  }
}