import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ResourceApiService } from '../../services/resource-api.service';
import { ResourceItem } from '../../models/portal.models';

@Component({
  selector: 'app-student-resource-view',
  standalone: true,
  templateUrl: './student-resource-view.component.html',
})
export class StudentResourceViewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly resourceApi = inject(ResourceApiService);

  resource: ResourceItem | null = null;
  videoUrl: string | null = null;
  error: string | null = null;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.resourceApi.get(id).subscribe({
      next: (r) => {
        this.resource = r;
        if (r.resourceType === 'LECTURE') {
          this.resourceApi.access(id).subscribe({
            next: (a) => (this.videoUrl = a.secureUrl),
            error: () => (this.error = 'Cannot play lecture'),
          });
        }
      },
      error: () => (this.error = 'Resource not found or access denied'),
    });
  }

  openDocument(): void {
    if (!this.resource) {
      return;
    }
    this.resourceApi.access(this.resource.id).subscribe({
      next: (access) => {
        window.open(access.secureUrl, '_blank', 'noopener,noreferrer');
      },
      error: () => (this.error = 'Cannot open file'),
    });
  }
}