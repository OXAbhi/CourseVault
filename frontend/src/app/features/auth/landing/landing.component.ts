import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="gallery">
      <div class="gallery-hero">
        <img src="assets/logo.svg" alt="" width="48" height="48" style="margin-bottom: 16px" />
        <p class="page-kicker">Setu · Nexus Institute</p>
        <h1>CS Learning Portal</h1>
        <p class="page-lead" style="margin-top: 12px">
          Notes, lectures, and assignments for your enrolled batches — one place instead of scattered
          file shares.
        </p>
        <a routerLink="/login" class="btn btn-primary" style="margin-top: 24px">Sign in</a>
      </div>
    </div>
  `,
})
export class LandingComponent {}