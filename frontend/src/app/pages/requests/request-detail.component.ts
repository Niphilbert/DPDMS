import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { REQUEST_STATUS, rwf, todayIso } from '../../core/format';
import { ProjectRequest } from '../../core/models';
import { RequestsService } from '../../core/requests.service';
import { SessionService } from '../../core/session.service';
import { ToastService } from '../../core/toast.service';

interface Line {
  description: string;
  amount: number | null;
}

@Component({
  selector: 'app-request-detail',
  imports: [FormsModule, DatePipe, RouterLink],
  template: `
    <div class="top">
      <div>
        <p class="small"><a routerLink="/requests">← Requests and quotes</a></p>
        <h1>{{ req()?.title ?? 'Request' }}</h1>
        @if (req(); as r) {
          <p class="sub"><span class="pill" [class]="badge(r).cls">{{ badge(r).label }}</span></p>
        }
      </div>
    </div>

    @if (loading()) {
      <p class="muted loading">Loading…</p>
    } @else if (loadError()) {
      <div class="callout bad" role="alert">{{ loadError() }}</div>
    } @else if (req(); as r) {
      @if (message()) { <div class="callout ok" role="status">{{ message() }}</div> }
      @if (actionError()) { <div class="callout bad" role="alert">{{ actionError() }}</div> }

      <div class="grid2">
        <div class="stack">
          <section class="card" aria-labelledby="rq">
            <header><h2 id="rq">The request</h2></header>
            <dl>
              @if (!isClient()) { <dt>Client</dt><dd>{{ r.companyName }}</dd> }
              <dt>Sent by</dt><dd>{{ r.createdByName }} on {{ r.createdAt | date: 'd MMM y' }}</dd>
              <dt>Wanted by</dt><dd>{{ r.deadline | date: 'd MMM y' }}</dd>
              <dt>Budget idea</dt><dd>{{ r.budget || 'Not given' }}</dd>
              <dt>Description</dt><dd class="note">{{ r.description }}</dd>
            </dl>
            @if (r.changeNote) {
              <div class="callout warn" style="margin-top:12px">
                <b>{{ r.status === 'DECLINED' ? 'Reason for declining' : 'Changes asked for' }}:</b> {{ r.changeNote }}
              </div>
            }
          </section>

          @if (r.quote; as q) {
            <section class="card" aria-labelledby="qt">
              <header>
                <h2 id="qt">Quote</h2>
                <span class="muted small">Sent by {{ q.quotedByName }} on {{ q.sentAt | date: 'd MMM y' }}</span>
              </header>
              <div class="tblwrap">
                <table style="min-width:360px">
                  <thead><tr><th>Work</th><th style="text-align:right">Amount</th></tr></thead>
                  <tbody>
                    @for (it of q.items; track $index) {
                      <tr><td>{{ it.description }}</td><td class="num" style="text-align:right">{{ money(it.amount) }}</td></tr>
                    }
                  </tbody>
                </table>
              </div>
              <p style="margin-top:12px"><span class="muted">Total</span> <span class="total">{{ money(q.total) }}</span></p>
              <p class="muted small">About {{ q.weeks }} weeks of work. Valid until {{ q.validUntil | date: 'd MMM y' }}.</p>
            </section>
          } @else if (isClient()) {
            <section class="card"><p class="muted">A project manager is preparing your quote. We will tell you here when it is ready.</p></section>
          }
        </div>

        <div class="stack">
          @if (isClient()) {
            @if (r.status === 'QUOTED') {
              <section class="card" aria-labelledby="ans">
                <header><h2 id="ans">Your answer</h2></header>
                <div class="stack" style="gap:12px">
                  <p class="muted small">Accepting creates your project right away.</p>
                  <div><button class="btn primary" type="button" (click)="accept()" [disabled]="busy()">Accept the quote</button></div>
                  @if (mode() === 'changes') {
                    <div class="box">
                      <div class="field">
                        <label for="cn">What should change?</label>
                        <textarea id="cn" [ngModel]="note()" (ngModelChange)="note.set($event)"></textarea>
                        @if (fields()['note']) { <span class="fielderr">{{ fields()['note'] }}</span> }
                      </div>
                      <div class="tools">
                        <button class="btn" type="button" (click)="sendChanges()" [disabled]="busy()">Send</button>
                        <button class="btn" type="button" (click)="mode.set('')">Cancel</button>
                      </div>
                    </div>
                  } @else if (mode() === 'decline') {
                    <div class="box">
                      <div class="field">
                        <label for="dr">Reason (optional)</label>
                        <textarea id="dr" [ngModel]="note()" (ngModelChange)="note.set($event)"></textarea>
                      </div>
                      <div class="tools">
                        <button class="btn danger" type="button" (click)="decline()" [disabled]="busy()">Decline the quote</button>
                        <button class="btn" type="button" (click)="mode.set('')">Cancel</button>
                      </div>
                    </div>
                  } @else {
                    <div class="tools">
                      <button class="btn" type="button" (click)="openMode('changes')">Ask for changes</button>
                      <button class="btn danger" type="button" (click)="openMode('decline')">Decline</button>
                    </div>
                  }
                </div>
              </section>
            } @else if (r.status === 'CHANGES_REQUESTED') {
              <section class="card"><p>You asked for changes. We will send a revised quote.</p></section>
            }
          }

          @if (r.status === 'ACCEPTED' && r.projectId) {
            <section class="card">
              <h2>Project created</h2>
              <p class="muted" style="margin:6px 0 12px">The quote was accepted and the project is set up.</p>
              <a class="btn primary" [routerLink]="['/projects', r.projectId]">Open the project</a>
            </section>
          }

          @if (canQuote()) {
            <section class="card" aria-labelledby="wq">
              <header><h2 id="wq">{{ r.quote ? 'Revise the quote' : 'Write the quote' }}</h2></header>
              <form (ngSubmit)="sendQuote()" novalidate>
                @for (l of lines(); track $index) {
                  <div class="qrow">
                    <input [attr.aria-label]="'Work ' + ($index + 1)" placeholder="What is included" [ngModel]="l.description" (ngModelChange)="setLine($index, 'description', $event)" [ngModelOptions]="{ standalone: true }" />
                    <input [attr.aria-label]="'Amount ' + ($index + 1)" type="number" min="1" step="1000" placeholder="Amount (RWF)" [ngModel]="l.amount" (ngModelChange)="setLine($index, 'amount', $event)" [ngModelOptions]="{ standalone: true }" />
                    <button class="btn sm" type="button" (click)="removeLine($index)" [disabled]="lines().length === 1" [attr.aria-label]="'Remove line ' + ($index + 1)">Remove</button>
                  </div>
                }
                <p><button class="link" type="button" (click)="addLine()" [disabled]="lines().length >= 30">Add a line</button></p>
                <p style="margin:12px 0"><span class="muted">Total</span> <span class="total">{{ money(total()) }}</span></p>
                <div class="formgrid">
                  <div class="field">
                    <label for="qw">Weeks of work</label>
                    <input id="qw" name="qw" type="number" min="1" max="104" [class.invalid]="fields()['weeks']" [(ngModel)]="weeks" />
                    @if (fields()['weeks']) { <span class="fielderr">{{ fields()['weeks'] }}</span> }
                  </div>
                  <div class="field">
                    <label for="qv">Valid until</label>
                    <input id="qv" name="qv" type="date" [min]="today" [class.invalid]="fields()['validUntil']" [(ngModel)]="validUntil" />
                    @if (fields()['validUntil']) { <span class="fielderr">{{ fields()['validUntil'] }}</span> }
                  </div>
                </div>
                @if (quoteError()) { <p class="err" role="alert">{{ quoteError() }}</p> }
                <div style="margin-top:12px"><button class="btn primary" type="submit" [disabled]="busy()">{{ r.quote ? 'Send revised quote' : 'Send quote to client' }}</button></div>
              </form>
            </section>
          }
        </div>
      </div>
    }
  `,
})
export class RequestDetailComponent implements OnInit {
  /** The route parameter `:id`, bound through withComponentInputBinding. */
  readonly id = input.required<string>();

  private readonly api = inject(RequestsService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);

  readonly req = signal<ProjectRequest | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly busy = signal(false);
  readonly message = signal('');
  readonly actionError = signal('');
  readonly mode = signal<'' | 'changes' | 'decline'>('');
  readonly note = signal('');
  readonly fields = signal<Record<string, string>>({});
  readonly quoteError = signal('');
  readonly today = todayIso();

  readonly lines = signal<Line[]>([{ description: '', amount: null }]);
  readonly total = computed(() => this.lines().reduce((sum, l) => sum + (Number(l.amount) > 0 ? Number(l.amount) : 0), 0));
  weeks: number | null = 8;
  validUntil = todayIso(14);

  readonly isClient = computed(() => this.session.user()?.role === 'CLIENT');
  readonly canQuote = computed(() => {
    const r = this.req();
    return !this.isClient() && !!r && (r.status === 'NEW' || r.status === 'QUOTED' || r.status === 'CHANGES_REQUESTED');
  });

  ngOnInit(): void {
    this.api.get(this.id()).subscribe({
      next: (r) => {
        this.show(r);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.loadError.set(toApiError(e).message);
      },
    });
  }

  badge(r: ProjectRequest) {
    return REQUEST_STATUS[r.status];
  }

  money(n: number): string {
    return rwf(n) || '0 RWF';
  }

  // ---- client answers
  openMode(m: 'changes' | 'decline'): void {
    this.note.set('');
    this.fields.set({});
    this.actionError.set('');
    this.mode.set(m);
  }

  accept(): void {
    this.run(this.api.accept(this.id()), 'Quote accepted. Your project has been set up.');
  }

  sendChanges(): void {
    this.run(this.api.requestChanges(this.id(), this.note().trim()), 'Your request for changes was sent.');
  }

  decline(): void {
    this.run(this.api.decline(this.id(), this.note()), 'The quote was declined.');
  }

  // ---- manager quote
  setLine(i: number, key: keyof Line, value: string | number | null): void {
    this.lines.update((ls) => ls.map((l, idx) => (idx === i ? { ...l, [key]: key === 'amount' ? (value === null || value === '' ? null : Number(value)) : value } : l)));
  }

  addLine(): void {
    this.lines.update((ls) => [...ls, { description: '', amount: null }]);
  }

  removeLine(i: number): void {
    this.lines.update((ls) => ls.filter((_, idx) => idx !== i));
  }

  sendQuote(): void {
    this.fields.set({});
    this.quoteError.set('');
    this.busy.set(true);
    const items = this.lines().map((l) => ({ description: l.description.trim(), amount: Number(l.amount) }));
    this.api.quote(this.id(), { items, weeks: Number(this.weeks), validUntil: this.validUntil }).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.show(r);
        this.toast.show('Quote sent to the client');
      },
      error: (e) => {
        const err = toApiError(e);
        this.busy.set(false);
        this.fields.set(err.fields);
        const others = Object.entries(err.fields).filter(([k]) => k !== 'weeks' && k !== 'validUntil').map(([, v]) => v);
        this.quoteError.set(err.code === 'VALIDATION_FAILED' ? ['Please check the quote.', ...others].join(' ') : err.message);
      },
    });
  }

  private run(call: ReturnType<RequestsService['accept']>, done: string): void {
    this.actionError.set('');
    this.message.set('');
    this.fields.set({});
    this.busy.set(true);
    call.subscribe({
      next: (r) => {
        this.busy.set(false);
        this.mode.set('');
        this.show(r);
        this.message.set(done);
      },
      error: (e) => {
        const err = toApiError(e);
        this.busy.set(false);
        this.fields.set(err.fields);
        this.actionError.set(err.code === 'VALIDATION_FAILED' ? '' : err.message);
      },
    });
  }

  /** Shows a request and prepares the quote form from its current quote, so a revision starts from what was sent. */
  private show(r: ProjectRequest): void {
    this.req.set(r);
    if (r.quote) {
      this.lines.set(r.quote.items.map((i) => ({ description: i.description, amount: i.amount })));
      this.weeks = r.quote.weeks;
    }
  }
}
