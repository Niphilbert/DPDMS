import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../core/admin.service';
import { toApiError } from '../../core/api-error';
import { Access } from '../../core/models';
import { Person, StaffService } from '../../core/staff.service';
import { ToastService } from '../../core/toast.service';

/** Administrator only: let a developer see a project without having a task on it, or take that access away. */
@Component({
  selector: 'app-project-access',
  imports: [FormsModule, DatePipe],
  template: `
    <section class="card" aria-labelledby="xh">
      <header><h2 id="xh">Developer access</h2></header>
      <p class="muted small" style="margin-bottom:12px">Developers see a project when they have a task on it. Give extra access here so a developer can read the project, its updates and its time without a task. A developer who still has a task keeps seeing the project after you remove access.</p>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      <div class="filters">
        <div class="field" style="min-width:240px">
          <label for="gd">Developer</label>
          <select id="gd" [ngModel]="choice()" (ngModelChange)="choice.set($event)">
            <option value="">Choose a developer…</option>
            @for (p of available(); track p.id) { <option [value]="p.id">{{ p.name }}</option> }
          </select>
        </div>
        <button class="btn primary" type="button" (click)="grant()" [disabled]="!choice() || busy()">Give access</button>
      </div>
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (granted().length === 0) {
        <p class="muted empty">No extra access given.</p>
      } @else {
        <div class="list">
          @for (a of granted(); track a.userId) {
            <div class="item">
              <div class="grow"><div class="l1">{{ a.fullName }}</div><div class="l2 muted small">{{ a.email }} · since {{ a.grantedAt | date: 'd MMM y' }}</div></div>
              <button class="btn sm danger" type="button" (click)="revoke(a)" [disabled]="busy()" [attr.aria-label]="'Remove access for ' + a.fullName">Remove access</button>
            </div>
          }
        </div>
      }
    </section>
  `,
})
export class ProjectAccessComponent implements OnInit {
  readonly projectId = input.required<string>();

  private readonly admin = inject(AdminService);
  private readonly staff = inject(StaffService);
  private readonly toast = inject(ToastService);

  readonly granted = signal<Access[]>([]);
  readonly developers = signal<Person[]>([]);
  readonly available = computed(() => this.developers().filter((d) => !this.granted().some((g) => g.userId === d.id)));
  readonly choice = signal('');
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');

  ngOnInit(): void {
    this.admin.projectAccess(this.projectId()).subscribe({
      next: (list) => {
        this.granted.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(toApiError(e).message);
      },
    });
    this.staff.developers().subscribe({ next: (list) => this.developers.set(list), error: (e) => this.error.set(toApiError(e).message) });
  }

  grant(): void {
    this.act(this.admin.grantAccess(this.projectId(), this.choice()), 'Access given');
    this.choice.set('');
  }

  revoke(a: Access): void {
    this.act(this.admin.revokeAccess(this.projectId(), a.userId), 'Access removed');
  }

  private act(call: ReturnType<AdminService['projectAccess']>, done: string): void {
    this.error.set('');
    this.busy.set(true);
    call.subscribe({
      next: (list) => {
        this.busy.set(false);
        this.granted.set(list);
        this.toast.show(done);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(toApiError(e).message);
      },
    });
  }
}
