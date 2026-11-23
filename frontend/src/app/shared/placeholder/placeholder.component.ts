import { Component } from '@angular/core';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  template: `
    <div class="page-head">
      <h1 class="page-title">Coming soon</h1>
      <p class="page-lead">This screen is not available yet.</p>
    </div>
  `,
})
export class PlaceholderComponent {}