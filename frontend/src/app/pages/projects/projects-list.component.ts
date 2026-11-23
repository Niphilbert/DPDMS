import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { PROJECT_STATUS, rwf } from '../../core/format';
import { Project } from '../../core/models';
import { ProjectsService } from '../../core/projects.service';
import { SessionService } from '../../core/session.service';

@Component({
  selector: 'app-projects-list',
  imports: [DatePipe, RouterLink],
  template: `
    <div class="top">
      <div>
        <h1>{{ isClient() ? 'My projects' : 'Projects' }}</h1>
        <p class="sub">{{ isClient() ? 'Progress, approvals and invoices for your projects.' : 'Every project from planning to launch.' }}</p>
      </div>
    </div>
    @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
    @if (loading()) {
      <p class="muted loading">Loading…</p>
    } @else if (items().length === 0 && !error()) {
      <section class="card"><p class="muted">{{ emptyText() }}</p></section>
    } @else {
      <div class="cards">
        @for (p of items(); track p.id) {
          <a class="pcard" [routerLink]="['/projects', p.id]">
            <div style="display:flex; justify-content:space-between; gap:8px; align-items:flex-start">
              <h3>{{ p.name }}</h3>
              <span class="pill" [class]="badge(p).cls">{{ badge(p).label }}</span>
            </div>
            <div class="muted small">{{ isClient() ? 'Managed by ' + p.managerName : p.companyName + ' · managed by ' + p.managerName }}</div>
            <div>
              <div style="display:flex; justify-content:space-between" class="small"><span>Progress</span><b class="num">{{ p.progress }}%</b></div>
              <div class="meter" [class.ok]="p.progress === 100" role="progressbar" [attr.aria-valuenow]="p.progress" aria-valuemin="0" aria-valuemax="100" [attr.aria-label]="p.name + ' progress'"><i [style.width.%]="p.progress"></i></div>
            </div>
            <div class="small muted" style="display:flex; gap:12px; flex-wrap:wrap">
              @if (p.dueDate) { <span>Due {{ p.dueDate | date: 'd MMM y' }}</span> }
              @if (p.budget !== undefined) { <span>Budget {{ money(p.budget) }}</span> }
              @if (p.lateTasks > 0) { <span class="pill p-bad">{{ p.lateTasks }} late</span> }
              @if (p.openApprovals) { <span class="pill p-warn">{{ p.openApprovals }} to approve</span> }
            </div>
          </a>
        }
      </div>
    }
  `,
})
export class ProjectsListComponent implements OnInit {
  private readonly api = inject(ProjectsService);
  private readonly session = inject(SessionService);

  readonly items = signal<Project[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly isClient = computed(() => this.session.user()?.role === 'CLIENT');
  readonly emptyText = computed(() => {
    switch (this.session.user()?.role) {
      case 'CLIENT':
        return 'Your project appears here as soon as you accept a quote.';
      case 'DEVELOPER':
        return 'You will see a project here once a task is assigned to you, or an administrator gives you access.';
      default:
        return 'Projects appear here when a client accepts a quote.';
    }
  });

  ngOnInit(): void {
    this.api.list().subscribe({
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

  badge(p: Project) {
    return PROJECT_STATUS[p.status];
  }

  money(n: number): string {
    return rwf(n);
  }
}
