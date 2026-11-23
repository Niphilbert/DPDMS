import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toApiError } from '../../core/api-error';
import { CHANGE_STATUS, rwf } from '../../core/format';
import { ChangeRequest, Project } from '../../core/models';
import { ProjectsService } from '../../core/projects.service';
import { SessionService } from '../../core/session.service';
import { ToastService } from '../../core/toast.service';

/** Extra work with its own price. The team proposes it; the client accepts (adds to the budget) or declines. */
@Component({
  selector: 'app-project-changes',
  imports: [FormsModule, DatePipe],
  template: `
    <section class="card" aria-labelledby="ch">
      <header>
        <h2 id="ch">Change requests</h2>
        @if (isManagement()) { <button class="btn primary" type="button" (click)="openNew()">Propose a change</button> }
      </header>
      <p class="muted small" style="margin-bottom:12px">{{ isClient() ? 'Extra work after your quote was accepted. Accepting adds its cost to the project budget.' : 'Extra work with its own price. When the client accepts, the cost is added to the project budget.' }}</p>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (items().length === 0) {
        <p class="muted empty">No change requests.</p>
      } @else {
        <div class="list">
          @for (c of items(); track c.id) {
            <div class="item" style="align-items:flex-start">
              <div class="grow">
                <div class="l1">{{ c.title }} <span class="muted">· {{ money(c.cost) }}</span></div>
                <div class="muted small">Proposed by {{ c.proposedByName }} on {{ c.createdAt | date: 'd MMM y' }}@if (c.answeredAt) { · answered by {{ c.answeredByName }} on {{ c.answeredAt | date: 'd MMM y' }} }</div>
                @if (isClient() && c.status === 'PROPOSED') {
                  <div class="tools" style="margin-top:8px">
                    <button class="btn sm primary" type="button" (click)="answer(c, 'accept')" [disabled]="busy()">Accept ({{ money(c.cost) }})</button>
                    <button class="btn sm" type="button" (click)="answer(c, 'decline')" [disabled]="busy()">Decline</button>
                  </div>
                }
              </div>
              <span class="pill" [class]="badge(c).cls">{{ badge(c).label }}</span>
            </div>
          }
        </div>
      }
    </section>

    @if (adding()) {
      <div class="overlay" (click)="adding.set(false)"></div>
      <form class="modal" role="dialog" aria-modal="true" aria-labelledby="nc" (ngSubmit)="propose()" novalidate>
        <h2 id="nc">Propose a change</h2>
        <div class="formgrid">
          <div class="field wide"><label for="ct">What is the extra work?</label><input id="ct" name="ct" [class.invalid]="fields()['title']" [(ngModel)]="fTitle" />@if (fields()['title']) { <span class="fielderr">{{ fields()['title'] }}</span> }</div>
          <div class="field"><label for="cc">Cost (RWF)</label><input id="cc" name="cc" type="number" min="1" step="1000" [class.invalid]="fields()['cost']" [(ngModel)]="fCost" />@if (fields()['cost']) { <span class="fielderr">{{ fields()['cost'] }}</span> }</div>
        </div>
        @if (formError()) { <p class="err" role="alert">{{ formError() }}</p> }
        <div class="tools">
          <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Sending…' : 'Send to client' }}</button>
          <button class="btn" type="button" (click)="adding.set(false)">Cancel</button>
        </div>
      </form>
    }
  `,
})
export class ProjectChangesComponent implements OnInit {
  readonly project = input.required<Project>();
  readonly changed = output<void>();

  private readonly api = inject(ProjectsService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);

  readonly items = signal<ChangeRequest[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly busy = signal(false);
  readonly isClient = computed(() => this.session.user()?.role === 'CLIENT');
  readonly isManagement = computed(() => this.session.user()?.role !== 'CLIENT');

  readonly adding = signal(false);
  readonly fields = signal<Record<string, string>>({});
  readonly formError = signal('');
  fTitle = '';
  fCost: number | null = null;

  ngOnInit(): void {
    this.load();
  }

  badge(c: ChangeRequest) {
    return CHANGE_STATUS[c.status];
  }

  money(n: number): string {
    return rwf(n);
  }

  load(): void {
    this.api.changes(this.project().id).subscribe({
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

  answer(c: ChangeRequest, answer: 'accept' | 'decline'): void {
    this.error.set('');
    this.busy.set(true);
    this.api.answerChange(this.project().id, c.id, answer).subscribe({
      next: () => {
        this.busy.set(false);
        this.toast.show(answer === 'accept' ? 'Change accepted. The budget was updated.' : 'Change declined');
        this.load();
        this.changed.emit();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(toApiError(e).message);
        this.load();
      },
    });
  }

  openNew(): void {
    this.fTitle = '';
    this.fCost = null;
    this.fields.set({});
    this.formError.set('');
    this.adding.set(true);
  }

  propose(): void {
    this.fields.set({});
    this.formError.set('');
    this.busy.set(true);
    this.api.proposeChange(this.project().id, { title: this.fTitle.trim(), cost: Number(this.fCost) }).subscribe({
      next: () => {
        this.busy.set(false);
        this.adding.set(false);
        this.toast.show('Change request sent to the client');
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
