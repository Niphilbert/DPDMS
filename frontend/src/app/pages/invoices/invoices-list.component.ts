import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';
import { toApiError } from '../../core/api-error';
import { saveBlob } from '../../core/download';
import { INVOICE_STATUS, PAYMENT_METHOD, rwf } from '../../core/format';
import { InvoicesService } from '../../core/invoices.service';
import { Company, Invoice, InvoiceDisplayStatus, InvoiceFilter, InvoiceSummary, InvoiceTotals, Payment, Project } from '../../core/models';
import { canManageMoney } from '../../core/permissions';
import { ProjectsService } from '../../core/projects.service';
import { SessionService } from '../../core/session.service';
import { ToastService } from '../../core/toast.service';
import { InvoiceFormComponent } from './invoice-form.component';

const STATUS_FILTERS: { value: InvoiceDisplayStatus | ''; label: string; managementOnly?: boolean }[] = [
  { value: '', label: 'All' },
  { value: 'DRAFT', label: 'Drafts', managementOnly: true },
  { value: 'SENT', label: 'Sent' },
  { value: 'PARTIALLY_PAID', label: 'Partly paid' },
  { value: 'OVERDUE', label: 'Overdue' },
  { value: 'PAID', label: 'Paid' },
  { value: 'VOID', label: 'Void' },
];

interface PaymentRow {
  payment: Payment;
  invoiceId: string;
  number: string;
}

@Component({
  selector: 'app-invoices-list',
  imports: [FormsModule, DatePipe, RouterLink, InvoiceFormComponent],
  template: `
    <div class="top">
      <div>
        <h1>Invoices</h1>
        <p class="sub">{{ isClient() ? 'What you owe and what you have paid.' : 'Invoices with payment status, balances and overdue amounts.' }}</p>
      </div>
      @if (canManage()) {
        <div class="tools">
          <button class="btn" type="button" (click)="exportCsv()" [disabled]="exporting()">{{ exporting() ? 'Preparing…' : 'Export CSV' }}</button>
          <button class="btn primary" type="button" (click)="creating.set(true)">New invoice</button>
        </div>
      }
    </div>

    @if (totals(); as t) {
      <div class="band four" role="list" aria-label="Invoice totals">
        <div class="stat" role="listitem"><span class="caps">Invoiced</span><span class="v sm num">{{ money(t.invoiced) }}</span><span class="n">{{ t.invoiceCount }} {{ t.invoiceCount === 1 ? 'invoice' : 'invoices' }}</span></div>
        <div class="stat" role="listitem"><span class="caps">Paid</span><span class="v sm ok num">{{ money(t.paid) }}</span><span class="n">{{ t.invoiced ? pct(t.paid, t.invoiced) + '% of invoiced' : 'Nothing invoiced yet' }}</span></div>
        <div class="stat" role="listitem"><span class="caps">{{ isClient() ? 'Balance due' : 'Outstanding' }}</span><span class="v sm num">{{ money(t.balanceDue) }}</span><span class="n">{{ t.openCount }} open {{ t.openCount === 1 ? 'invoice' : 'invoices' }}</span></div>
        <div class="stat" role="listitem"><span class="caps">Overdue</span><span class="v sm num" [class.bad]="t.overdueCount > 0">{{ money(t.overdueAmount) }}</span><span class="n">{{ t.overdueCount }} {{ t.overdueCount === 1 ? 'invoice' : 'invoices' }} past the due date</span></div>
      </div>
    }

    <section class="card">
      <div class="filters">
        <div class="field">
          <span class="caps" id="stl">Status</span>
          <div class="tabs" role="group" aria-labelledby="stl">
            @for (f of statusFilters(); track f.value) {
              <button type="button" [attr.aria-pressed]="status() === f.value" (click)="setStatus(f.value)">{{ f.label }}</button>
            }
          </div>
        </div>
      </div>
      @if (canManage()) {
        <div class="filters">
          <div class="field grow">
            <label for="fc">Client</label>
            <select id="fc" [ngModel]="companyId()" (ngModelChange)="companyId.set($event); load()">
              <option value="">All clients</option>
              @for (c of companies(); track c.id) { <option [value]="c.id">{{ c.name }}</option> }
            </select>
          </div>
          <div class="field grow">
            <label for="fp">Project</label>
            <select id="fp" [ngModel]="projectId()" (ngModelChange)="projectId.set($event); load()">
              <option value="">All projects</option>
              @for (p of projects(); track p.id) { <option [value]="p.id">{{ p.name }}</option> }
            </select>
          </div>
          <div class="field narrow">
            <label for="ff">Issued from</label>
            <input id="ff" type="date" [ngModel]="from()" (ngModelChange)="from.set($event); load()" />
          </div>
          <div class="field narrow">
            <label for="ft">Issued to</label>
            <input id="ft" type="date" [ngModel]="to()" (ngModelChange)="to.set($event); load()" />
          </div>
        </div>
      }
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (items().length === 0 && !error()) {
        <p class="muted">{{ isClient() ? 'No invoices to show. When ITZETech sends you an invoice it appears here.' : 'No invoices match.' }}</p>
      } @else {
        <div class="tblwrap cardtable">
          <table>
            <thead><tr><th>Invoice</th>@if (!isClient()) { <th>Client</th> }<th>Issued</th><th>Due</th><th class="right">Total</th><th class="right">Balance</th><th>Status</th></tr></thead>
            <tbody>
              @for (i of items(); track i.id) {
                <tr class="row" [class.row-bad]="i.overdue" tabindex="0" (click)="open(i)" (keydown.enter)="open(i)">
                  <td class="first"><div class="l1"><a [routerLink]="['/invoices', i.id]" (click)="$event.stopPropagation()">{{ i.number }}</a></div><div class="l2">{{ i.projectName ?? (i.note || 'Company invoice') }}</div></td>
                  @if (!isClient()) { <td data-label="Client">{{ i.companyName }}</td> }
                  <td class="num" data-label="Issued">{{ i.issueDate | date: 'd MMM y' }}</td>
                  <td class="num" data-label="Due">{{ i.dueDate | date: 'd MMM y' }}@if (i.overdue) { <div class="l2" style="color:var(--bad)">{{ i.daysOverdue }} {{ i.daysOverdue === 1 ? 'day' : 'days' }} late</div> }</td>
                  <td class="num right" data-label="Total">{{ money(i.total) }}</td>
                  <td class="num right" data-label="Balance">{{ i.status === 'DRAFT' ? '—' : money(i.balance) }}</td>
                  <td data-label="Status"><span class="pill" [class]="badge(i).cls">{{ badge(i).label }}</span></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <p class="muted small" style="margin-top:10px">{{ items().length }} {{ items().length === 1 ? 'invoice' : 'invoices' }} shown.</p>
      }
    </section>

    @if (isClient()) {
      <section class="card" aria-labelledby="ph">
        <header><h2 id="ph">Payment history</h2><span class="muted small">Every payment ITZETech recorded for your invoices.</span></header>
        @if (payments().length === 0) {
          <p class="muted">{{ historyLoaded() ? 'No payments yet.' : 'Loading…' }}</p>
        } @else {
          <div class="tblwrap cardtable">
            <table>
              <thead><tr><th>Date</th><th>Invoice</th><th>Method</th><th>Reference</th><th class="right">Amount</th></tr></thead>
              <tbody>
                @for (p of payments(); track p.payment.id) {
                  <tr [class.reversed]="p.payment.reversed">
                    <td class="first num">{{ p.payment.paidOn | date: 'd MMM y' }}@if (p.payment.reversed) { <span class="pill p-neutral" style="margin-left:8px">Reversed</span> }</td>
                    <td data-label="Invoice"><a [routerLink]="['/invoices', p.invoiceId]">{{ p.number }}</a></td>
                    <td data-label="Method">{{ method(p.payment) }}</td>
                    <td data-label="Reference">{{ p.payment.reference || '—' }}</td>
                    <td class="num right amt" data-label="Amount">{{ money(p.payment.amount) }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    }

    @if (creating()) {
      <app-invoice-form (saved)="created($event)" (cancelled)="creating.set(false)" />
    }
  `,
})
export class InvoicesListComponent implements OnInit {
  private readonly api = inject(InvoicesService);
  private readonly projectsApi = inject(ProjectsService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly isClient = computed(() => this.session.user()?.role === 'CLIENT');
  readonly canManage = computed(() => canManageMoney(this.session.user()?.role));
  readonly statusFilters = computed(() => STATUS_FILTERS.filter((f) => this.canManage() || !f.managementOnly));

  readonly items = signal<InvoiceSummary[]>([]);
  readonly totals = signal<InvoiceTotals | null>(null);
  readonly companies = signal<Company[]>([]);
  readonly projects = signal<Project[]>([]);
  readonly payments = signal<PaymentRow[]>([]);
  readonly historyLoaded = signal(false);
  readonly status = signal<InvoiceDisplayStatus | ''>('');
  readonly companyId = signal('');
  readonly projectId = signal('');
  readonly from = signal('');
  readonly to = signal('');
  readonly loading = signal(true);
  readonly error = signal('');
  readonly creating = signal(false);
  readonly exporting = signal(false);

  ngOnInit(): void {
    if (this.canManage()) {
      this.api.companies().subscribe({ next: (c) => this.companies.set(c), error: () => undefined });
      this.projectsApi.list().subscribe({ next: (p) => this.projects.set(p), error: () => undefined });
    }
    this.load();
  }

  badge(i: InvoiceSummary) {
    return INVOICE_STATUS[i.displayStatus];
  }

  money(n: number): string {
    return rwf(n) || '0 RWF';
  }

  pct(part: number, whole: number): number {
    return whole > 0 ? Math.round((100 * part) / whole) : 0;
  }

  method(p: Payment): string {
    return PAYMENT_METHOD[p.method];
  }

  setStatus(s: InvoiceDisplayStatus | ''): void {
    this.status.set(s);
    this.load();
  }

  private filter(): InvoiceFilter {
    return {
      status: this.status(),
      companyId: this.canManage() ? this.companyId() : '',
      projectId: this.canManage() ? this.projectId() : '',
      from: this.canManage() ? this.from() : '',
      to: this.canManage() ? this.to() : '',
    };
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    const f = this.filter();
    this.api.list(f).subscribe({
      next: (list) => {
        this.items.set(list);
        this.loading.set(false);
        if (this.isClient() && !this.historyLoaded()) this.loadHistory(list);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(toApiError(e).message);
      },
    });
    this.api.totals({ companyId: f.companyId, projectId: f.projectId }).subscribe({ next: (t) => this.totals.set(t), error: () => this.totals.set(null) });
  }

  /** A client's payment history is built from the payments on their invoices (the newest 30 sent invoices). */
  private loadHistory(list: InvoiceSummary[]): void {
    const sent = list.filter((i) => i.status !== 'DRAFT').slice(0, 30);
    if (sent.length === 0) {
      this.historyLoaded.set(true);
      return;
    }
    forkJoin(sent.map((i) => this.api.get(i.id).pipe(catchError(() => of(null as Invoice | null)))))
      .pipe(
        map((invoices) =>
          invoices
            .filter((i): i is Invoice => i !== null)
            .flatMap((i) => i.payments.map((payment) => ({ payment, invoiceId: i.id, number: i.number })))
            .sort((a, b) => b.payment.paidOn.localeCompare(a.payment.paidOn) || b.payment.recordedAt.localeCompare(a.payment.recordedAt)),
        ),
      )
      .subscribe((rows) => {
        this.payments.set(rows);
        this.historyLoaded.set(true);
      });
  }

  open(i: InvoiceSummary): void {
    void this.router.navigate(['/invoices', i.id]);
  }

  created(inv: Invoice): void {
    this.creating.set(false);
    this.toast.show(`Draft ${inv.number} saved`);
    void this.router.navigate(['/invoices', inv.id]);
  }

  exportCsv(): void {
    this.exporting.set(true);
    this.error.set('');
    this.api.exportCsv(this.filter()).subscribe({
      next: (blob) => {
        this.exporting.set(false);
        saveBlob(blob, 'invoices.csv');
      },
      error: (e) => {
        this.exporting.set(false);
        this.error.set(toApiError(e).message);
      },
    });
  }
}
