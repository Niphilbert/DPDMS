import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { toApiError } from '../../core/api-error';
import { Project, TimeSummary } from '../../core/models';
import { ProjectsService } from '../../core/projects.service';

/** Planned against logged hours. Managers see every task; a developer sees only their own. Logging happens on the task card. */
@Component({
  selector: 'app-project-time',
  imports: [DatePipe],
  template: `
    <section class="card" aria-labelledby="th">
      <header><h2 id="th">Time</h2><span class="muted small">Log your hours from a task on the Tasks tab.</span></header>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (summary(); as s) {
        <p style="margin-bottom:12px"><b class="num">{{ s.loggedHours }}</b> hours logged of <b class="num">{{ s.plannedHours }}</b> planned</p>
        @if (s.tasks.length === 0) {
          <p class="muted empty">No tasks to show.</p>
        } @else {
          <div class="tblwrap cardtable">
            <table style="min-width:480px">
              <thead><tr><th>Task</th><th>Person</th><th>Planned</th><th>Logged</th></tr></thead>
              <tbody>
                @for (t of s.tasks; track t.taskId) {
                  <tr><td class="l1 first">{{ t.title }}</td><td data-label="Person">{{ t.assigneeName ?? 'Not assigned' }}</td><td class="num" data-label="Planned">{{ t.plannedHours }} h</td><td class="num" data-label="Logged">{{ t.loggedHours }} h</td></tr>
                }
              </tbody>
            </table>
          </div>
        }
        <h3 style="margin:18px 0 8px">Entries</h3>
        @if (s.entries.length === 0) {
          <p class="muted empty">No time has been logged yet.</p>
        } @else {
          <div class="tblwrap cardtable">
            <table style="min-width:560px">
              <thead><tr><th>Day</th><th>Task</th><th>Person</th><th>Hours</th><th>Note</th></tr></thead>
              <tbody>
                @for (e of s.entries; track e.id) {
                  <tr><td class="num first">{{ e.date | date: 'd MMM y' }}</td><td data-label="Task">{{ e.taskTitle }}</td><td data-label="Person">{{ e.userName }}</td><td class="num" data-label="Hours">{{ e.hours }}</td><td class="muted" data-label="Note">{{ e.note }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        }
      }
    </section>
  `,
})
export class ProjectTimeComponent implements OnInit {
  readonly project = input.required<Project>();
  private readonly api = inject(ProjectsService);

  readonly summary = signal<TimeSummary | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    this.api.timeSummary(this.project().id).subscribe({
      next: (s) => {
        this.summary.set(s);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(toApiError(e).message);
      },
    });
  }
}
