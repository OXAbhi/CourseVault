import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SearchApiService } from '../../services/search-api.service';
import { ResourceType, SearchHit } from '../../models/portal.models';

@Component({
  selector: 'app-student-search',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './search-page.component.html',
})
export class StudentSearchPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly searchApi = inject(SearchApiService);

  results: SearchHit[] = [];
  searched = false;

  readonly form = this.fb.nonNullable.group({
    q: [''],
    type: ['' as '' | ResourceType],
    tag: [''],
  });

  run(): void {
    const { q, type, tag } = this.form.getRawValue();
    this.searchApi.search(q, type || undefined, tag).subscribe({
      next: (hits) => {
        this.results = hits;
        this.searched = true;
      },
    });
  }
}