import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { RENEWAL_STATUS, RENEWAL_TYPE, daysText, rwf } from '../../core/format';
import { InvoicesService } from '../../core/invoices.service';
import { Company, ReminderRun, Renewal, RenewalStatus, RenewalType } from '../../core/models';
import { canManageRenewals, canRunJobs, canSeeMoney } from '../../core/permissions';
import { RenewalsService } from '../../core/renewals.service';
import { SessionService } from '../../core/session.service';
import { ToastService } from '../../core/toast.service';
import { RenewalFormComponent } from './renewal-form.component';

const STATUS_FILTERS: { value: RenewalStatus | ''; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'EXPIRING', label: 'Expiring soon' },
  { value: 'EXPIRED', label: 'Expired' },
  { value: 'ACTIVE', label: 'Active' },
];

@Component({
  selector: 'app-renewals-list',
  imports: [FormsModule, DatePipe, RouterLink, RenewalFormComponent],
  template: `
    <div class="top">
      <div>
        <h1>{{ isClient() ? 'My websites and renewals' : 'Renewals' }}</h1>
        <p class="sub">Domains, hosting, SSL certificates and maintenance plans, with reminders before they expire.</p>
      </div>
      @if (canManage()) {
        <div class="tools"><button class="btn primary" type="button" (click)="adding.set(true)">Add a renewal</button></div>
      }
    </div>

    @if (needAttention() > 0) {
      <div class="callout warn" role="status">
        <b>{{ counts().expired }} expired and {{ counts().expiring }} expiring within 30 days</b> in this list.
        {{ isClient() ? 'Select "Request renewal" and ITZETech will take care of it.' : 'Reminders are sent automatically; you can also renew an item from its page.' }}
      </div>
    }

    <div class="band four" role="list" aria-label="Renewal numbers">
      <div class="stat" role="listitem"><span class="caps">Expiring soon</span><span class="v sm num" [class.warn]="counts().expiring > 0">{{ counts().expiring }}</span><span class="n">Within 30 days</span></div>
      <div class="stat" role="listitem"><span class="caps">Expired</span><span class="v sm num" [class.bad]="counts().expired > 0">{{ counts().expired }}</span><span class="n">Need action now</span></div>
      <div class="stat" role="listitem"><span class="caps">Active</span><span class="v sm num">{{ counts().active }}</span><span class="n">More than 30 days left</span></div>
    </div>

    @if (canRun()) {
      <section class="card" aria-labelledby="rrh">
        <header><h2 id="rrh">Reminders</h2><span class="muted small">Sent automatically every day at 07:00. Administrators can run them now.</span></header>
        <div class="filters" style="margin-bottom:0">
          <div class="field narrow">
            <label for="rd">Pretend today is (optional)</label>
            <input id="rd" type="date" [ngModel]="runDate()" (ngModelChange)="runDate.set($event)" />
          </div>
          <button class="btn" type="button" (click)="runReminders()" [disabled]="running()">{{ running() ? 'Running…' : 'Run reminders now' }}</button>
        </div>
        @if (runError()) { <p class="err" role="alert">{{ runError() }}</p> }
        @if (run(); as r) {
          <div class="callout" [class.ok]="r.sent > 0" [class.info]="r.sent === 0" role="status" style="margin-top:12px">
            For {{ r.today | date: 'd MMM y' }}: checked {{ r.checked }}, sent <b>{{ r.sent }}</b> {{ r.sent === 1 ? 'reminder' : 'reminders' }}.
            @if (r.sent === 0) { Nothing new to send for this day (each reminder is sent once). }
            @if (r.reminders.length > 0) {
              <ul style="margin:8px 0 0; padding-left:18px">
                @for (m of r.reminders; track m.renewalId) {
                  <li>{{ typeLabel(m.type) }} <b>{{ m.label }}</b> ({{ m.projectName }}): {{ m.daysLeft < 0 ? 'expired ' + daysText(m.daysLeft) : m.daysLeft === 0 ? 'expires today' : m.daysLeft + ' days left' }}, expiry {{ m.expiresOn | date: 'd MMM y' }}</li>
                }
              </ul>
            }
          </div>
        }
      </section>
    }

    <section class="card">
      <div class="filters">
        <div class="field">
          <span class="caps" id="rst">Status</span>
          <div class="tabs" role="group" aria-labelledby="rst">
            @for (f of statusFilters; track f.value) {
              <button type="button" [attr.aria-pressed]="status() === f.value" (click)="status.set(f.value); load()">{{ f.label }}</button>
            }
          </div>
        </div>
      </div>
      <div class="filters">
        <div class="field grow">
          <label for="rt">Type</label>
          <select id="rt" [ngModel]="type()" (ngModelChange)="type.set($event); load()">
            <option value="">All types</option>
            @for (t of types; track t) { <option [value]="t">{{ typeLabel(t) }}</option> }
          </select>
        </div>
        @if (canManage()) {
          <div class="field grow">
            <label for="rc">Client</label>
            <select id="rc" [ngModel]="companyId()" (ngModelChange)="companyId.set($event); load()">
              <option value="">All clients</option>
              @for (c of companies(); track c.id) { <option [value]="c.id">{{ c.name }}</option> }
            </select>
          </div>
        }
        <div class="field narrow">
          <label for="rw">Expiring within (days)</label>
          <input id="rw" type="number" min="0" max="3650" inputmode="numeric" placeholder="Any" [ngModel]="within()" (ngModelChange)="within.set($event); load()" />
        </div>
      </div>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (items().length === 0 && !error()) {
        <p class="muted empty">No renewals match.</p>
      } @else {
        <div class="tblwrap cardtable">
          <table>
            <thead>
              <tr><th>Item</th>@if (!isClient()) { <th>Client</th> }<th>Project</th><th>Expires</th><th>Status</th>@if (showPrice()) { <th class="right">Price</th> }<th></th></tr>
            </thead>
            <tbody>
              @for (r of items(); track r.id) {
                <tr class="row" [class.row-emph]="r.status === 'EXPIRING'" [class.row-bad]="r.status === 'EXPIRED'" tabindex="0" (click)="open(r)" (keydown.enter)="open(r)">
                  <td class="first"><div class="l1"><a [routerLink]="['/renewals', r.id]" (click)="$event.stopPropagation()">{{ r.label }}</a></div><div class="l2">{{ typeLabel(r.type) }}@if (r.provider) { · {{ r.provider }} }</div></td>
                  @if (!isClient()) { <td data-label="Client">{{ r.companyName }}</td> }
                  <td data-label="Project">{{ r.projectName }}</td>
                  <td class="num" data-label="Expires">{{ r.expiresOn | date: 'd MMM y' }}<div class="l2" [style.color]="r.status === 'ACTIVE' ? '' : r.status === 'EXPIRED' ? 'var(--bad)' : 'var(--warn)'" [style.font-weight]="r.status === 'ACTIVE' ? '' : '700'">{{ daysText(r.daysLeft) }}</div></td>
                  <td data-label="Status"><span class="pill" [class]="badge(r).cls">{{ badge(r).label }}</span></td>
                  @if (showPrice()) { <td class="num right" data-label="Price">{{ money(r.price) }}</td> }
                  <td class="right nolabel">
                    @if (isClient() && r.status !== 'ACTIVE') { <button class="btn sm" type="button" (click)="request(r, $event)" [disabled]="requested().has(r.id)">{{ requested().has(r.id) ? 'Requested' : 'Request renewal' }}</button> }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    @if (adding()) {
      <app-renewal-form (saved)="created($event)" (cancelled)="adding.set(false)" />
    }
  `,
})
export class RenewalsListComponent implements OnInit {
  private readonly api = inject(RenewalsService);
  private readonly invoicesApi = inject(InvoicesService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly statusFilters = STATUS_FILTERS;
  readonly types: RenewalType[] = ['DOMAIN', 'HOSTING', 'SSL', 'MAINTENANCE'];
  readonly items = signal<Renewal[]>([]);
  readonly companies = signal<Company[]>([]);
  readonly status = signal<RenewalStatus | ''>('');
  readonly type = signal<RenewalType | ''>('');
  readonly companyId = signal('');
  readonly within = signal<number | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly adding = signal(false);
  readonly requested = signal<Set<string>>(new Set());

  readonly runDate = signal('');
  readonly running = signal(false);
  readonly run = signal<ReminderRun | null>(null);
  readonly runError = signal('');

  readonly role = computed(() => this.session.user()?.role);
  readonly isClient = computed(() => this.role() === 'CLIENT');
  readonly canManage = computed(() => canManageRenewals(this.role()));
  readonly canRun = computed(() => canRunJobs(this.role()));
  readonly showPrice = computed(() => canSeeMoney(this.role()));
  readonly counts = computed(() => {
    const list = this.items();
    return {
      expiring: list.filter((r) => r.status === 'EXPIRING').length,
      expired: list.filter((r) => r.status === 'EXPIRED').length,
      active: list.filter((r) => r.status === 'ACTIVE').length,
    };
  });
  readonly needAttention = computed(() => this.counts().expiring + this.counts().expired);

  ngOnInit(): void {
    if (this.canManage()) this.invoicesApi.companies().subscribe({ next: (c) => this.companies.set(c), error: () => undefined });
    this.load();
  }

  badge(r: Renewal) {
    return RENEWAL_STATUS[r.status];
  }

  typeLabel(t: RenewalType): string {
    return RENEWAL_TYPE[t];
  }

  daysText = daysText;

  money(n: number | undefined): string {
    return n === undefined ? '' : rwf(n) || '0 RWF';
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api
      .list({ status: this.status(), type: this.type(), companyId: this.companyId(), expiringWithinDays: this.within() === null || (this.within() as unknown) === '' ? null : Number(this.within()) })
      .subscribe({
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

  open(r: Renewal): void {
    void this.router.navigate(['/renewals', r.id]);
  }

  created(r: Renewal): void {
    this.adding.set(false);
    this.toast.show('Renewal added');
    void this.router.navigate(['/renewals', r.id]);
  }

  request(r: Renewal, ev: Event): void {
    ev.stopPropagation();
    this.api.requestRenewal(r.id).subscribe({
      next: () => {
        this.requested.update((s) => new Set(s).add(r.id));
        this.toast.show('We told ITZETech you want this renewed');
      },
      error: (e) => this.error.set(toApiError(e).message),
    });
  }

  runReminders(): void {
    this.running.set(true);
    this.runError.set('');
    this.api.runReminders(this.runDate() || undefined).subscribe({
      next: (r) => {
        this.running.set(false);
        this.run.set(r);
        this.load();
      },
      error: (e) => {
        this.running.set(false);
        this.runError.set(toApiError(e).message);
      },
    });
  }
}
