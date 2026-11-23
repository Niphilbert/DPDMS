import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { PROJECT_STATUS, rwf } from '../../core/format';
import { Project } from '../../core/models';
import { ProjectsService } from '../../core/projects.service';
import { SessionService } from '../../core/session.service';
import { ToastService } from '../../core/toast.service';
import { canSeeMoney } from '../../core/permissions';
import { ProjectAccessComponent } from './project-access.component';
import { ProjectApprovalsComponent } from './project-approvals.component';
import { ProjectChangesComponent } from './project-changes.component';
import { ProjectMoneyComponent } from './project-money.component';
import { ProjectTasksComponent } from './project-tasks.component';
import { ProjectTimeComponent } from './project-time.component';
import { ProjectUpdatesComponent } from './project-updates.component';

type Tab = 'tasks' | 'approvals' | 'changes' | 'time' | 'money' | 'updates' | 'access';

@Component({
  selector: 'app-project-detail',
  imports: [
    DatePipe, RouterLink, ProjectTasksComponent, ProjectApprovalsComponent, ProjectChangesComponent,
    ProjectTimeComponent, ProjectUpdatesComponent, ProjectAccessComponent, ProjectMoneyComponent,
  ],
  template: `
    <div class="top">
      <div>
        <p class="small"><a routerLink="/projects">← Projects</a></p>
        <h1>{{ project()?.name ?? 'Project' }}</h1>
        @if (project(); as p) {
          <p class="sub">
            <span class="pill" [class]="badge().cls">{{ badge().label }}</span>
            {{ isClient() ? '' : p.companyName + ' · ' }}Managed by {{ p.managerName }}
          </p>
        }
      </div>
      @if (project(); as p) {
        @if (isManagement() && p.status === 'PLANNING') {
          <div class="tools"><button class="btn primary" type="button" (click)="start()" [disabled]="busy()">Start the project</button></div>
        }
      }
    </div>

    @if (loading()) {
      <p class="muted loading">Loading…</p>
    } @else if (loadError()) {
      <div class="callout bad" role="alert">{{ loadError() }}</div>
    } @else if (project(); as p) {
      @if (actionError()) { <div class="callout bad" role="alert">{{ actionError() }}</div> }
      @if (p.status === 'PLANNING') {
        <div class="callout info">
          {{ isManagement() ? 'This project is in planning. Assign the tasks, then start it. Developers can only move tasks and log time after it starts.' : 'Work has not started yet. We will tell you when it does.' }}
        </div>
      }
      @if (isClient() && (p.openApprovals ?? 0) > 0) {
        <div class="callout warn">
          {{ p.openApprovals }} {{ p.openApprovals === 1 ? 'item is' : 'items are' }} waiting for your answer.
          <button class="link" type="button" (click)="tab.set('approvals')">Review now</button>
        </div>
      }

      <section class="card" aria-label="Progress">
        <div class="stack" style="gap:14px">
          <div>
            <div style="display:flex; justify-content:space-between" class="small"><b>Overall progress</b><b class="num">{{ p.progress }}%</b></div>
            <div class="meter" [class.ok]="p.progress === 100" role="progressbar" [attr.aria-valuenow]="p.progress" aria-valuemin="0" aria-valuemax="100" aria-label="Overall progress"><i [style.width.%]="p.progress"></i></div>
          </div>
          <div class="phases">
            @for (ph of p.phases; track ph.index) {
              <div>
                <span class="caps">{{ ph.name }}</span>
                @if (ph.progress === null) {
                  <span class="muted">No tasks</span>
                } @else {
                  <div class="meter" [class.ok]="ph.progress === 100" role="progressbar" [attr.aria-valuenow]="ph.progress" aria-valuemin="0" aria-valuemax="100" [attr.aria-label]="ph.name + ' progress'"><i [style.width.%]="ph.progress"></i></div>
                  <span class="num">{{ ph.progress }}% · {{ ph.taskCount }} {{ ph.taskCount === 1 ? 'task' : 'tasks' }}</span>
                }
              </div>
            }
          </div>
          <dl>
            @if (p.startDate) { <dt>Start date</dt><dd>{{ p.startDate | date: 'd MMM y' }}</dd> }
            @if (p.dueDate) { <dt>Due</dt><dd>{{ p.dueDate | date: 'd MMM y' }}</dd> }
            @if (p.budget !== undefined) { <dt>Budget</dt><dd>{{ money(p.budget) }}</dd> }
            @if (p.plannedHours !== undefined) { <dt>Hours</dt><dd class="num">{{ p.loggedHours ?? 0 }} logged of {{ p.plannedHours }} planned</dd> }
            @if (p.lateTasks > 0) { <dt>Late tasks</dt><dd><span class="pill p-bad">{{ p.lateTasks }}</span></dd> }
          </dl>
        </div>
      </section>

      <div class="tabs bar" role="group" aria-label="Project sections">
        @for (t of tabs(); track t.id) {
          <button type="button" [attr.aria-pressed]="tab() === t.id" (click)="tab.set(t.id)">{{ t.label }}</button>
        }
      </div>

      @switch (tab()) {
        @case ('tasks') { <app-project-tasks [project]="p" (changed)="reload()" /> }
        @case ('approvals') { <app-project-approvals [project]="p" (changed)="reload()" /> }
        @case ('changes') { <app-project-changes [project]="p" (changed)="reload()" /> }
        @case ('time') { <app-project-time [project]="p" /> }
        @case ('money') { <app-project-money [project]="p" /> }
        @case ('updates') { <app-project-updates [projectId]="p.id" /> }
        @case ('access') { <app-project-access [projectId]="p.id" /> }
      }
    }
  `,
})
export class ProjectDetailComponent implements OnInit {
  /** The route parameter `:id`, bound through withComponentInputBinding. */
  readonly id = input.required<string>();

  private readonly api = inject(ProjectsService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);

  readonly project = signal<Project | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly actionError = signal('');
  readonly busy = signal(false);
  readonly tab = signal<Tab>('tasks');

  readonly role = computed(() => this.session.user()?.role);
  readonly isClient = computed(() => this.role() === 'CLIENT');
  readonly isManagement = computed(() => this.role() === 'ADMIN' || this.role() === 'PROJECT_MANAGER');
  readonly badge = computed(() => PROJECT_STATUS[this.project()?.status ?? 'PLANNING']);
  readonly tabs = computed<{ id: Tab; label: string }[]>(() => {
    const role = this.role();
    const list: { id: Tab; label: string }[] = [{ id: 'tasks', label: 'Tasks' }];
    if (role !== 'DEVELOPER') {
      list.push({ id: 'approvals', label: 'Approvals' }, { id: 'changes', label: 'Change requests' });
    }
    if (role !== 'CLIENT') list.push({ id: 'time', label: 'Time' });
    if (canSeeMoney(role)) list.push({ id: 'money', label: 'Money' });
    list.push({ id: 'updates', label: 'Updates' });
    if (role === 'ADMIN') list.push({ id: 'access', label: 'Developer access' });
    return list;
  });

  ngOnInit(): void {
    this.fetch(true);
  }

  money(n: number): string {
    return rwf(n);
  }

  /** Reloads after a child changed something, so progress, status and counts stay true. */
  reload(): void {
    this.fetch(false);
  }

  start(): void {
    this.actionError.set('');
    this.busy.set(true);
    this.api.start(this.id()).subscribe({
      next: (p) => {
        this.busy.set(false);
        this.project.set(p);
        this.toast.show('Project started');
      },
      error: (e) => {
        this.busy.set(false);
        this.actionError.set(toApiError(e).message);
      },
    });
  }

  private fetch(first: boolean): void {
    this.api.get(this.id()).subscribe({
      next: (p) => {
        this.project.set(p);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        if (first) this.loadError.set(toApiError(e).message);
        else this.actionError.set(toApiError(e).message);
      },
    });
  }
}
