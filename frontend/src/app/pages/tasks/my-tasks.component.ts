import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { PHASES, PROJECT_STATUS, TASK_STATUS, todayIso } from '../../core/format';
import { MyTask, TaskStatus } from '../../core/models';
import { ProjectsService } from '../../core/projects.service';
import { ToastService } from '../../core/toast.service';

/** Everything assigned to me across projects. Handing work in and reviewing happens on the project page. */
@Component({
  selector: 'app-my-tasks',
  imports: [DatePipe, RouterLink, FormsModule],
  template: `
    <div class="top">
      <div>
        <h1>My tasks</h1>
        <p class="sub">Your work across all projects. Move a task forward or log your hours.</p>
      </div>
    </div>
    <section class="card">
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (items().length === 0 && !error()) {
        <p class="muted empty">No tasks are assigned to you yet.</p>
      } @else {
        <div class="tblwrap cardtable">
          <table>
            <thead><tr><th>Task</th><th>Project</th><th>Status</th><th>Due</th><th>Hours</th><th><span class="sr-only">Actions</span></th></tr></thead>
            <tbody>
              @for (m of items(); track m.task.id) {
                <tr>
                  <td class="first"><div class="l1">{{ m.task.title }}</div><div class="l2">{{ phase(m.task.phase) }}</div></td>
                  <td data-label="Project"><a [routerLink]="['/projects', m.projectId]">{{ m.projectName }}</a><div class="l2">{{ projectLabel(m) }}</div></td>
                  <td data-label="Status"><span class="pill" [class]="badge(m.task.status).cls">{{ badge(m.task.status).label }}</span></td>
                  <td class="num" data-label="Due">@if (m.task.dueDate) { {{ m.task.dueDate | date: 'd MMM y' }} } @if (m.task.late) { <span class="pill p-bad">Late</span> }</td>
                  <td class="num" data-label="Hours">{{ m.task.loggedHours ?? 0 }}/{{ m.task.plannedHours ?? 0 }}</td>
                  <td class="nolabel">
                    <div class="tools">
                      @if (m.projectStatus !== 'PLANNING') {
                        @if (m.task.status === 'TODO') { <button class="btn sm" type="button" (click)="move(m, 'DOING')">Start</button> }
                        @if (m.task.status === 'REVIEW') { <button class="btn sm" type="button" (click)="move(m, 'DOING')">Take back</button> }
                        @if (m.task.status === 'TODO' || m.task.status === 'DOING') { <button class="btn sm" type="button" (click)="openTime(m)">Log time</button> }
                        @if (m.task.status === 'DOING') { <a class="btn sm primary" [routerLink]="['/projects', m.projectId]">Submit work</a> }
                      } @else {
                        <span class="muted small">Not started</span>
                      }
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    @if (logging(); as m) {
      <div class="overlay" (click)="logging.set(null)"></div>
      <form class="modal" role="dialog" aria-modal="true" aria-labelledby="lt" (ngSubmit)="saveTime()" novalidate>
        <h2 id="lt">Log time: {{ m.task.title }}</h2>
        <div class="formgrid">
          <div class="field"><label for="td">Day</label><input id="td" name="td" type="date" [max]="today" [class.invalid]="fields()['date']" [(ngModel)]="fDate" />@if (fields()['date']) { <span class="fielderr">{{ fields()['date'] }}</span> }</div>
          <div class="field"><label for="th">Hours</label><input id="th" name="th" type="number" min="0.25" max="24" step="0.25" [class.invalid]="fields()['hours']" [(ngModel)]="fHours" />@if (fields()['hours']) { <span class="fielderr">{{ fields()['hours'] }}</span> }</div>
          <div class="field wide"><label for="tn">What did you do? (optional)</label><input id="tn" name="tn" maxlength="500" [(ngModel)]="fNote" /></div>
        </div>
        @if (formError()) { <p class="err" role="alert">{{ formError() }}</p> }
        <div class="tools">
          <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save' }}</button>
          <button class="btn" type="button" (click)="logging.set(null)">Cancel</button>
        </div>
      </form>
    }
  `,
})
export class MyTasksComponent implements OnInit {
  private readonly api = inject(ProjectsService);
  private readonly toast = inject(ToastService);

  readonly items = signal<MyTask[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly today = todayIso();

  readonly logging = signal<MyTask | null>(null);
  readonly busy = signal(false);
  readonly fields = signal<Record<string, string>>({});
  readonly formError = signal('');
  fDate = this.today;
  fHours: number | null = null;
  fNote = '';

  ngOnInit(): void {
    this.load();
  }

  badge(s: TaskStatus) {
    return TASK_STATUS[s];
  }

  phase(i: number): string {
    return PHASES[i] ?? '';
  }

  projectLabel(m: MyTask): string {
    return PROJECT_STATUS[m.projectStatus].label;
  }

  load(): void {
    this.api.mine().subscribe({
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

  move(m: MyTask, status: TaskStatus): void {
    this.error.set('');
    this.api.setStatus(m.projectId, m.task.id, status).subscribe({
      next: () => {
        this.toast.show('Task moved');
        this.load();
      },
      error: (e) => this.error.set(toApiError(e).message),
    });
  }

  openTime(m: MyTask): void {
    this.fDate = this.today;
    this.fHours = null;
    this.fNote = '';
    this.fields.set({});
    this.formError.set('');
    this.logging.set(m);
  }

  saveTime(): void {
    const m = this.logging();
    if (!m) return;
    this.fields.set({});
    this.formError.set('');
    this.busy.set(true);
    this.api.logTime(m.projectId, m.task.id, { date: this.fDate, hours: Number(this.fHours), ...(this.fNote.trim() ? { note: this.fNote.trim() } : {}) }).subscribe({
      next: () => {
        this.busy.set(false);
        this.logging.set(null);
        this.toast.show('Time logged');
        this.load();
      },
      error: (e) => {
        const err = toApiError(e);
        this.busy.set(false);
        this.fields.set(err.fields);
        this.formError.set(err.code === 'VALIDATION_FAILED' ? 'Please correct the fields marked below.' : err.message);
      },
    });
  }
}
