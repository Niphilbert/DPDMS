import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { REQUEST_STATUS, rwf, todayIso } from '../../core/format';
import { ProjectRequest, RequestStatus } from '../../core/models';
import { RequestsService } from '../../core/requests.service';
import { SessionService } from '../../core/session.service';

const FILTERS: { value: RequestStatus | ''; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'NEW', label: 'Waiting for a quote' },
  { value: 'QUOTED', label: 'Quote sent' },
  { value: 'CHANGES_REQUESTED', label: 'Changes asked' },
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'DECLINED', label: 'Declined' },
];

@Component({
  selector: 'app-requests-list',
  imports: [FormsModule, DatePipe, RouterLink],
  template: `
    <div class="top">
      <div>
        <h1>Requests and quotes</h1>
        <p class="sub">{{ isClient() ? 'Ask for new work and respond to quotes.' : 'Client requests, quotes and their answers.' }}</p>
      </div>
      @if (isClient()) {
        <div class="tools"><button class="btn primary" type="button" (click)="openNew()">New request</button></div>
      }
    </div>

    <section class="card">
      <div class="filters">
        <div class="tabs" role="group" aria-label="Filter by status">
          @for (f of filters; track f.value) {
            <button type="button" [attr.aria-pressed]="status() === f.value" (click)="setStatus(f.value)">{{ f.label }}</button>
          }
        </div>
      </div>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (items().length === 0 && !error()) {
        <p class="muted empty">{{ isClient() ? 'You have not sent any requests yet. Select "New request" to start.' : 'No requests match.' }}</p>
      } @else {
        <div class="tblwrap cardtable">
          <table>
            <thead><tr><th>Request</th>@if (!isClient()) { <th>Client</th> }<th>Status</th><th>Quote</th><th>Sent</th></tr></thead>
            <tbody>
              @for (r of items(); track r.id) {
                <tr class="row" tabindex="0" (click)="open(r)" (keydown.enter)="open(r)">
                  <td class="first"><div class="l1"><a [routerLink]="['/requests', r.id]" (click)="$event.stopPropagation()">{{ r.title }}</a></div><div class="l2">Wanted by {{ r.deadline | date: 'd MMM y' }}</div></td>
                  @if (!isClient()) { <td data-label="Client">{{ r.companyName }}</td> }
                  <td data-label="Status"><span class="pill" [class]="badge(r).cls">{{ badge(r).label }}</span></td>
                  <td class="num" data-label="Quote">{{ r.quote ? money(r.quote.total) : '—' }}</td>
                  <td class="num" data-label="Sent">{{ r.createdAt | date: 'd MMM y' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    @if (adding()) {
      <div class="overlay" (click)="adding.set(false)"></div>
      <form class="modal" role="dialog" aria-modal="true" aria-labelledby="nrt" (ngSubmit)="create()" novalidate>
        <h2 id="nrt">New request</h2>
        <p class="muted small">Tell us what you need. A project manager will send you a quote.</p>
        <div class="formgrid">
          <div class="field wide">
            <label for="rt">What do you need?</label>
            <input id="rt" name="rt" placeholder="For example: Booking website" [class.invalid]="fields()['title']" [(ngModel)]="title" />
            @if (fields()['title']) { <span class="fielderr">{{ fields()['title'] }}</span> }
          </div>
          <div class="field wide">
            <label for="rd">Describe the work</label>
            <textarea id="rd" name="rd" [class.invalid]="fields()['description']" [(ngModel)]="description"></textarea>
            @if (fields()['description']) { <span class="fielderr">{{ fields()['description'] }}</span> }
          </div>
          <div class="field">
            <label for="rb">Your budget (optional)</label>
            <input id="rb" name="rb" placeholder="RWF 3,000,000" [(ngModel)]="budget" />
          </div>
          <div class="field">
            <label for="rl">Wanted by</label>
            <input id="rl" name="rl" type="date" [min]="tomorrow" [class.invalid]="fields()['deadline']" [(ngModel)]="deadline" />
            @if (fields()['deadline']) { <span class="fielderr">{{ fields()['deadline'] }}</span> }
          </div>
        </div>
        @if (formError()) { <p class="err" role="alert">{{ formError() }}</p> }
        <div class="tools">
          <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Sending…' : 'Send request' }}</button>
          <button class="btn" type="button" (click)="adding.set(false)">Cancel</button>
        </div>
      </form>
    }
  `,
})
export class RequestsListComponent implements OnInit {
  private readonly api = inject(RequestsService);
  private readonly router = inject(Router);
  private readonly session = inject(SessionService);

  readonly filters = FILTERS;
  readonly isClient = computed(() => this.session.user()?.role === 'CLIENT');
  readonly items = signal<ProjectRequest[]>([]);
  readonly status = signal<RequestStatus | ''>('');
  readonly loading = signal(true);
  readonly error = signal('');

  readonly adding = signal(false);
  readonly busy = signal(false);
  readonly fields = signal<Record<string, string>>({});
  readonly formError = signal('');
  readonly tomorrow = todayIso(1);
  title = '';
  description = '';
  budget = '';
  deadline = '';

  ngOnInit(): void {
    this.load();
  }

  badge(r: ProjectRequest) {
    return REQUEST_STATUS[r.status];
  }

  money(n: number): string {
    return rwf(n);
  }

  setStatus(s: RequestStatus | ''): void {
    this.status.set(s);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.list(this.status()).subscribe({
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

  open(r: ProjectRequest): void {
    void this.router.navigate(['/requests', r.id]);
  }

  openNew(): void {
    this.title = this.description = this.budget = this.deadline = '';
    this.fields.set({});
    this.formError.set('');
    this.adding.set(true);
  }

  create(): void {
    this.fields.set({});
    this.formError.set('');
    this.busy.set(true);
    this.api.create({ title: this.title.trim(), description: this.description.trim(), budget: this.budget, deadline: this.deadline }).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.adding.set(false);
        void this.router.navigate(['/requests', r.id]);
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
