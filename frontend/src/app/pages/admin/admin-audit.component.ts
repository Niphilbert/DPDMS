import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../core/admin.service';
import { toApiError } from '../../core/api-error';
import { AuditEntry } from '../../core/models';

@Component({
  selector: 'app-admin-audit',
  imports: [FormsModule, DatePipe],
  template: `
    <div class="top">
      <div>
        <h1>Audit trail</h1>
        <p class="sub">Every action is recorded. Entries cannot be edited.</p>
      </div>
    </div>

    <section class="card">
      <div class="filters">
        <div class="field" style="flex:1; min-width:200px">
          <label for="q">Search</label>
          <input id="q" type="search" placeholder="Action, person or subject" [ngModel]="q()" (ngModelChange)="onSearch($event)" />
        </div>
      </div>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (entries().length === 0 && !error()) {
        <p class="muted empty">No entries match.</p>
      } @else {
        <div class="tblwrap cardtable">
          <table>
            <thead><tr><th>When</th><th>Who</th><th>Action</th><th>About</th><th>Reference</th></tr></thead>
            <tbody>
              @for (e of entries(); track e.id) {
                <tr>
                  <td class="num first">{{ e.occurredAt | date: 'd MMM y, HH:mm:ss' }}</td>
                  <td data-label="Who">{{ e.actorName }}</td>
                  <td data-label="Action">{{ e.action }}</td>
                  <td data-label="About">{{ e.subject }}</td>
                  <td class="mono" data-label="Reference">{{ e.ref }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <p class="muted small" style="margin-top:10px">Showing the latest {{ entries().length }} entries.</p>
      }
    </section>
  `,
})
export class AdminAuditComponent implements OnInit {
  private readonly admin = inject(AdminService);

  readonly entries = signal<AuditEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly q = signal('');
  private timer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.load();
  }

  onSearch(value: string): void {
    this.q.set(value);
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.load(), 250);
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.admin.audit(this.q()).subscribe({
      next: (list) => {
        this.entries.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(toApiError(e).message);
      },
    });
  }
}
