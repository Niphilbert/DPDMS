import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { toApiError } from '../../core/api-error';
import { ProjectUpdate } from '../../core/models';
import { ProjectsService } from '../../core/projects.service';

/** Plain-language lines about the project, newest first. Everyone on the project can read them. */
@Component({
  selector: 'app-project-updates',
  imports: [DatePipe],
  template: `
    <section class="card" aria-labelledby="uh">
      <header><h2 id="uh">Updates</h2></header>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (items().length === 0 && !error()) {
        <p class="muted empty">No updates yet.</p>
      } @else {
        <div class="feed">
          @for (u of items(); track u.id) {
            <div class="it"><span class="muted small num">{{ u.createdAt | date: 'd MMM, HH:mm' }}</span><span>{{ u.message }}</span></div>
          }
        </div>
      }
    </section>
  `,
})
export class ProjectUpdatesComponent implements OnInit {
  readonly projectId = input.required<string>();
  private readonly api = inject(ProjectsService);

  readonly items = signal<ProjectUpdate[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    this.api.updates(this.projectId()).subscribe({
      next: (list) => {
        this.items.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(toApiError(e).message);
      },
    });
  }
}
