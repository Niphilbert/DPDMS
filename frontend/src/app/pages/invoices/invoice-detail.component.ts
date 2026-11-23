import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { INVOICE_STATUS, PAYMENT_METHOD, plainNumber, rwf, todayIso } from '../../core/format';
import { InvoicesService } from '../../core/invoices.service';
import { Invoice, Payment, PaymentMethod } from '../../core/models';
import { invoiceActions } from '../../core/permissions';
import { SessionService } from '../../core/session.service';
import { ToastService } from '../../core/toast.service';
import { InvoiceFormComponent } from './invoice-form.component';

type Dialog = 'pay' | 'reverse' | 'void' | 'send' | 'edit' | null;

interface HistoryEvent {
  at: string;
  text: string;
}

/** Builds the status history from the timestamps the invoice and its payments carry (the API has no separate log). */
export function invoiceHistory(inv: Invoice, forClient: boolean): HistoryEvent[] {
  const ev: HistoryEvent[] = [];
  if (!forClient) ev.push({ at: inv.createdAt, text: `Draft created by ${inv.createdByName}` });
  if (inv.sentAt) ev.push({ at: inv.sentAt, text: 'Sent to the client' });
  for (const p of inv.payments) {
    ev.push({ at: p.recordedAt, text: `Payment of ${rwf(p.amount)} recorded by ${p.recordedByName}` });
    if (p.reversed && p.reversedAt) {
      const by = !forClient && p.reversedByName ? ` by ${p.reversedByName}` : '';
      const why = !forClient && p.reverseReason ? `: ${p.reverseReason}` : '';
      ev.push({ at: p.reversedAt, text: `Payment of ${rwf(p.amount)} reversed${by}${why}` });
    }
  }
  if (inv.voidedAt) ev.push({ at: inv.voidedAt, text: `Voided${!forClient && inv.voidReason ? ': ' + inv.voidReason : ''}` });
  return ev.sort((a, b) => a.at.localeCompare(b.at));
}

@Component({
  selector: 'app-invoice-detail',
  imports: [FormsModule, DatePipe, RouterLink, InvoiceFormComponent],
  template: `
    <div class="top">
      <div>
        <p class="small"><a routerLink="/invoices">← Invoices</a></p>
        <h1>{{ invoice()?.number ?? 'Invoice' }}</h1>
        @if (invoice(); as i) {
          <p class="sub">
            <span class="pill" [class]="badge().cls">{{ badge().label }}</span>
            {{ isClient() ? '' : i.companyName + ' · ' }}{{ i.projectName ?? 'Company invoice' }}
          </p>
        }
      </div>
      @if (invoice(); as i) {
        @if (acts(); as a) {
          <div class="tools">
            @if (a.edit) { <button class="btn" type="button" (click)="dialog.set('edit')">Edit draft</button> }
            @if (a.send) { <button class="btn primary" type="button" (click)="dialog.set('send')">Send to client</button> }
            @if (a.pay) { <button class="btn primary" type="button" (click)="openPay()">Record payment</button> }
            @if (a.remind) { <button class="btn" type="button" (click)="remind()" [disabled]="busy()">Send reminder</button> }
            @if (a.void) { <button class="btn danger" type="button" (click)="openVoid()">Void</button> }
          </div>
        }
      }
    </div>

    @if (loading()) {
      <p class="muted loading">Loading…</p>
    } @else if (loadError()) {
      <div class="callout bad" role="alert">{{ loadError() }}</div>
    } @else if (invoice(); as i) {
      @if (actionError()) { <div class="callout bad" role="alert">{{ actionError() }}</div> }
      @if (i.status === 'DRAFT') {
        <div class="callout info">This is a draft. The client cannot see it until you send it, and it can still be edited.</div>
      }
      @if (i.overdue) {
        <div class="callout bad" role="status">This invoice is {{ i.daysOverdue }} {{ i.daysOverdue === 1 ? 'day' : 'days' }} overdue. It was due on {{ i.dueDate | date: 'd MMM y' }} and {{ money(i.balance) }} is still unpaid.</div>
      }
      @if (i.status === 'VOID') {
        <div class="callout warn">This invoice was voided{{ i.voidedAt ? ' on ' + (i.voidedAt | date: 'd MMM y') : '' }}@if (i.voidReason) { : {{ i.voidReason }} }. Nothing is owed on it.</div>
      }
      @if (isClient() && (i.status === 'SENT' || i.status === 'PARTIALLY_PAID')) {
        <div class="callout info">
          <b>How to pay.</b> Pay {{ money(i.balance) }} by mobile money, bank transfer or cash and quote <b>{{ i.number }}</b>. ITZETech records your payment here when it arrives, and you get a notification.
        </div>
      }

      @if (i.status !== 'VOID') {
        <section class="balance" [class.paid]="i.status === 'PAID'" [class.overdue]="i.overdue" [class.partial]="i.status === 'PARTIALLY_PAID' && !i.overdue" aria-label="Balance summary">
          <div><div class="k">Invoice total</div><div class="big num">{{ money(i.total) }}</div></div>
          <div><div class="k">Paid so far</div><div class="big num">{{ money(i.paidAmount) }}</div></div>
          <div><div class="k">Balance due</div><div class="big due num">{{ money(i.balance) }}</div></div>
          <div><div class="k">Due date</div><div class="big num">{{ i.dueDate | date: 'd MMM y' }}</div></div>
          @if (i.status !== 'DRAFT' && i.total > 0) {
            <div class="prog">
              <div class="meter" role="img" [attr.aria-label]="paidPct(i) + '% paid'"><i [style.width.%]="paidPct(i)"></i></div>
              <span class="num">{{ paidPct(i) }}% paid{{ i.balance > 0 ? ', ' + money(i.balance) + ' still to pay' : '' }}</span>
            </div>
          }
        </section>
      }

      <div class="grid2">
        <div class="stack">
          <section class="card" aria-labelledby="ln">
            <header><h2 id="ln">Lines</h2><span class="num muted small">{{ i.lines.length }} {{ i.lines.length === 1 ? 'line' : 'lines' }}</span></header>
            <div class="tblwrap cardtable">
              <table style="min-width:0">
                <thead><tr><th>Description</th><th class="right">Quantity</th><th class="right">Unit price</th><th class="right">Total</th></tr></thead>
                <tbody>
                  @for (l of i.lines; track l.id) {
                    <tr>
                      <td class="first l1">{{ l.description }}</td>
                      <td class="num right" data-label="Quantity">{{ plain(l.quantity) }}</td>
                      <td class="num right" data-label="Unit price">{{ money(l.unitPrice) }}</td>
                      <td class="num right" data-label="Total">{{ money(l.lineTotal) }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <dl class="sumbox" style="margin-top:14px" aria-label="Totals">
              <dt>Subtotal</dt><dd class="num right">{{ money(i.subtotal) }}</dd>
              <dt>Tax ({{ plain(i.taxRate) }}%)</dt><dd class="num right">{{ money(i.taxAmount) }}</dd>
              <dt class="grand">Total</dt><dd class="grand num right">{{ money(i.total) }}</dd>
              @if (i.status !== 'DRAFT') {
                <dt>Paid</dt><dd class="num right">{{ money(i.paidAmount) }}</dd>
                <dt><b>Balance due</b></dt><dd class="num right"><b>{{ money(i.balance) }}</b></dd>
              }
            </dl>
          </section>

          @if (i.status !== 'DRAFT') {
            <section class="card" aria-labelledby="pm">
              <header><h2 id="pm">Payments</h2></header>
              @if (i.payments.length === 0) {
                <p class="muted empty">No payment has been recorded yet.</p>
              } @else {
                <div class="tblwrap cardtable">
                  <table>
                    <thead><tr><th>Date</th><th>Method</th><th>Reference</th><th class="right">Amount</th><th>Recorded by</th><th></th></tr></thead>
                    <tbody>
                      @for (p of i.payments; track p.id) {
                        <tr [class.reversed]="p.reversed">
                          <td class="first num">{{ p.paidOn | date: 'd MMM y' }}@if (p.reversed) { <span class="pill p-neutral" style="margin-left:8px">Reversed</span> }</td>
                          <td data-label="Method">{{ method(p) }}</td>
                          <td data-label="Reference">{{ p.reference || '—' }}</td>
                          <td class="num right amt" data-label="Amount">{{ money(p.amount) }}</td>
                          <td data-label="Recorded by">{{ p.recordedByName }}
                            @if (p.reversed && !isClient()) { <div class="l2">Reversed by {{ p.reversedByName }}: {{ p.reverseReason }}</div> }
                          </td>
                          <td class="right nolabel">
                            @if (acts().reversePayments && !p.reversed) { <button class="btn sm" type="button" (click)="openReverse(p)">Reverse</button> }
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            </section>
          }
        </div>

        <div class="stack">
          <section class="card" aria-labelledby="dt">
            <header><h2 id="dt">Details</h2></header>
            <dl>
              @if (!isClient()) { <dt>Client</dt><dd>{{ i.companyName }}</dd> }
              @if (i.projectId) { <dt>Project</dt><dd><a [routerLink]="['/projects', i.projectId]">{{ i.projectName }}</a></dd> }
              <dt>Issued</dt><dd>{{ i.issueDate | date: 'd MMM y' }}</dd>
              <dt>Due</dt><dd>{{ i.dueDate | date: 'd MMM y' }}</dd>
              <dt>Prepared by</dt><dd>{{ i.createdByName }}</dd>
              @if (i.note) { <dt>Note</dt><dd class="note">{{ i.note }}</dd> }
            </dl>
          </section>
          <section class="card" aria-labelledby="hs">
            <header><h2 id="hs">History</h2></header>
            <ol class="timeline">
              @for (h of history(); track $index) {
                <li><span class="muted num">{{ h.at | date: 'd MMM y, HH:mm' }}</span><span>{{ h.text }}</span></li>
              }
            </ol>
          </section>
        </div>
      </div>

      @switch (dialog()) {
        @case ('edit') {
          <app-invoice-form [invoice]="i" (saved)="edited($event)" (cancelled)="dialog.set(null)" />
        }
        @case ('send') {
          <div class="overlay" (click)="dialog.set(null)"></div>
          <div class="modal" role="dialog" aria-modal="true" aria-labelledby="sdt">
            <h2 id="sdt">Send {{ i.number }} to the client?</h2>
            <p>{{ i.companyName }} will be notified and e-mailed. After sending, the invoice can no longer be edited. Total: <b>{{ money(i.total) }}</b>, due {{ i.dueDate | date: 'd MMM y' }}.</p>
            @if (dialogError()) { <p class="err" role="alert">{{ dialogError() }}</p> }
            <div class="tools">
              <button class="btn primary" type="button" (click)="send()" [disabled]="busy()">{{ busy() ? 'Sending…' : 'Send invoice' }}</button>
              <button class="btn" type="button" (click)="dialog.set(null)">Cancel</button>
            </div>
          </div>
        }
        @case ('pay') {
          <div class="overlay" (click)="dialog.set(null)"></div>
          <form class="modal" role="dialog" aria-modal="true" aria-labelledby="pdt" (ngSubmit)="pay()" novalidate>
            <h2 id="pdt">Record a payment</h2>
            <p class="muted small">{{ i.number }} · balance due <b>{{ money(i.balance) }}</b>. Record what the client actually paid; you can record several payments.</p>
            <div class="formgrid">
              <div class="field">
                <label for="pa">Amount (RWF)</label>
                <input id="pa" name="pa" type="number" min="1" step="1" inputmode="numeric" [class.invalid]="payFieldError('amount')" [ngModel]="payAmount()" (ngModelChange)="payAmount.set($event)" />
                @if (payFieldError('amount'); as m) { <span class="fielderr" role="alert">{{ m }}</span> }
              </div>
              <div class="field">
                <label for="pd">Paid on</label>
                <input id="pd" name="pd" type="date" [max]="today" [class.invalid]="fields()['paidOn']" [(ngModel)]="payDate" />
                @if (fields()['paidOn']) { <span class="fielderr">{{ fields()['paidOn'] }}</span> }
              </div>
              <div class="field">
                <label for="pmeth">Method</label>
                <select id="pmeth" name="pmeth" [(ngModel)]="payMethod">
                  <option value="MOBILE_MONEY">Mobile money</option>
                  <option value="BANK_TRANSFER">Bank transfer</option>
                  <option value="CASH">Cash</option>
                </select>
              </div>
              <div class="field">
                <label for="pr">Reference (optional)</label>
                <input id="pr" name="pr" maxlength="100" placeholder="For example: MoMo 123456" [class.invalid]="fields()['reference']" [(ngModel)]="payRef" />
                @if (fields()['reference']) { <span class="fielderr">{{ fields()['reference'] }}</span> }
              </div>
            </div>
            @if (dialogError()) { <p class="err" role="alert">{{ dialogError() }}</p> }
            <div class="tools">
              <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Record payment' }}</button>
              <button class="btn" type="button" (click)="dialog.set(null)">Cancel</button>
            </div>
          </form>
        }
        @case ('reverse') {
          <div class="overlay" (click)="dialog.set(null)"></div>
          <form class="modal" role="dialog" aria-modal="true" aria-labelledby="rdt" (ngSubmit)="reverse()" novalidate>
            <h2 id="rdt">Reverse this payment?</h2>
            @if (target(); as p) {
              <p>The payment of <b>{{ money(p.amount) }}</b> on {{ p.paidOn | date: 'd MMM y' }} stays in the list marked as reversed, and the balance and status are worked out again.</p>
            }
            <div class="field">
              <label for="rr">Reason (required)</label>
              <textarea id="rr" name="rr" maxlength="300" [class.invalid]="fields()['reason']" [(ngModel)]="reason" placeholder="For example: cheque bounced"></textarea>
              @if (fields()['reason']) { <span class="fielderr">{{ fields()['reason'] }}</span> }
            </div>
            @if (dialogError()) { <p class="err" role="alert">{{ dialogError() }}</p> }
            <div class="tools">
              <button class="btn danger" type="submit" [disabled]="busy()">{{ busy() ? 'Reversing…' : 'Reverse payment' }}</button>
              <button class="btn" type="button" (click)="dialog.set(null)">Cancel</button>
            </div>
          </form>
        }
        @case ('void') {
          <div class="overlay" (click)="dialog.set(null)"></div>
          <form class="modal" role="dialog" aria-modal="true" aria-labelledby="vdt" (ngSubmit)="voidIt()" novalidate>
            <h2 id="vdt">Void {{ i.number }}?</h2>
            <p>A voided invoice cannot be sent, paid or edited, and its number is not reused.@if (i.sentAt) { The client is told it was voided. }</p>
            <div class="field">
              <label for="vr">Reason (optional)</label>
              <textarea id="vr" name="vr" maxlength="300" [class.invalid]="fields()['reason']" [(ngModel)]="reason" placeholder="For example: wrong client"></textarea>
              @if (fields()['reason']) { <span class="fielderr">{{ fields()['reason'] }}</span> }
            </div>
            @if (dialogError()) { <p class="err" role="alert">{{ dialogError() }}</p> }
            <div class="tools">
              <button class="btn danger" type="submit" [disabled]="busy()">{{ busy() ? 'Voiding…' : 'Void invoice' }}</button>
              <button class="btn" type="button" (click)="dialog.set(null)">Cancel</button>
            </div>
          </form>
        }
      }
    }
  `,
})
export class InvoiceDetailComponent implements OnInit {
  /** The route parameter `:id`. */
  readonly id = input.required<string>();

  private readonly api = inject(InvoicesService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);

  readonly invoice = signal<Invoice | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly actionError = signal('');
  readonly dialogError = signal('');
  readonly fields = signal<Record<string, string>>({});
  readonly busy = signal(false);
  readonly dialog = signal<Dialog>(null);
  readonly target = signal<Payment | null>(null);
  readonly submitted = signal(false);

  readonly today = todayIso();
  readonly payAmount = signal<number | null>(null);
  payDate = todayIso();
  payMethod: PaymentMethod = 'MOBILE_MONEY';
  payRef = '';
  reason = '';

  readonly role = computed(() => this.session.user()?.role);
  readonly isClient = computed(() => this.role() === 'CLIENT');
  readonly badge = computed(() => INVOICE_STATUS[this.invoice()?.displayStatus ?? 'DRAFT']);
  readonly acts = computed(() => {
    const i = this.invoice();
    return i ? invoiceActions(i, this.role()) : invoiceActions({ status: 'VOID', balance: 0, paidAmount: 0 } as Invoice, this.role());
  });
  readonly history = computed(() => {
    const i = this.invoice();
    return i ? invoiceHistory(i, this.isClient()) : [];
  });

  ngOnInit(): void {
    this.api.get(this.id()).subscribe({
      next: (i) => {
        this.invoice.set(i);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.loadError.set(toApiError(e).message);
      },
    });
  }

  money(n: number): string {
    return rwf(n) || '0 RWF';
  }

  /** How much of the invoice total has been paid, as a whole percent (for the progress bar only). */
  paidPct(i: Invoice): number {
    return i.total > 0 ? Math.max(0, Math.min(100, Math.round((i.paidAmount / i.total) * 100))) : 0;
  }

  plain(n: number): string {
    return plainNumber(n);
  }

  method(p: Payment): string {
    return PAYMENT_METHOD[p.method];
  }

  /** The client-side checks for the payment amount; the server checks the same rules again. */
  payFieldError(field: 'amount'): string {
    const server = this.fields()[field];
    if (server) return server;
    const i = this.invoice();
    const a = this.payAmount();
    if (!i || a === null || a === undefined || !this.submitted()) return '';
    if (!Number.isInteger(Number(a)) || Number(a) <= 0) return 'Enter a whole number of francs above 0.';
    if (Number(a) > i.balance) return `That is more than the balance. Enter ${this.money(i.balance)} or less.`;
    return '';
  }

  private reset(): void {
    this.fields.set({});
    this.dialogError.set('');
    this.actionError.set('');
    this.submitted.set(false);
    this.reason = '';
  }

  openPay(): void {
    this.reset();
    this.payAmount.set(this.invoice()?.balance ?? null);
    this.payDate = todayIso();
    this.payMethod = 'MOBILE_MONEY';
    this.payRef = '';
    this.dialog.set('pay');
  }

  openReverse(p: Payment): void {
    this.reset();
    this.target.set(p);
    this.dialog.set('reverse');
  }

  openVoid(): void {
    this.reset();
    this.dialog.set('void');
  }

  private done(i: Invoice, message: string): void {
    this.busy.set(false);
    this.invoice.set(i);
    this.dialog.set(null);
    this.toast.show(message);
  }

  private failed(e: unknown, inDialog = true): void {
    const err = toApiError(e);
    this.busy.set(false);
    this.fields.set(err.fields);
    const text = err.code === 'VALIDATION_FAILED' ? 'Please correct the fields marked below.' : err.message;
    if (inDialog && this.dialog()) this.dialogError.set(text);
    else this.actionError.set(text);
  }

  edited(i: Invoice): void {
    this.invoice.set(i);
    this.dialog.set(null);
    this.toast.show(`Draft ${i.number} saved`);
  }

  send(): void {
    this.reset();
    this.busy.set(true);
    this.api.send(this.id()).subscribe({ next: (i) => this.done(i, 'Invoice sent to the client'), error: (e) => this.failed(e) });
  }

  pay(): void {
    const i = this.invoice();
    if (!i) return;
    this.fields.set({});
    this.dialogError.set('');
    this.submitted.set(true);
    if (this.payFieldError('amount')) return;
    this.busy.set(true);
    this.api
      .recordPayment(i.id, { amount: Number(this.payAmount()), paidOn: this.payDate, method: this.payMethod, reference: this.payRef.trim() || undefined })
      .subscribe({
        next: (inv) => this.done(inv, inv.status === 'PAID' ? 'Payment recorded. The invoice is now paid in full.' : 'Payment recorded'),
        error: (e) => this.failed(e),
      });
  }

  reverse(): void {
    const p = this.target();
    if (!p) return;
    this.fields.set({});
    this.dialogError.set('');
    this.busy.set(true);
    this.api.reversePayment(this.id(), p.id, this.reason.trim()).subscribe({
      next: (i) => this.done(i, 'Payment reversed'),
      error: (e) => this.failed(e),
    });
  }

  voidIt(): void {
    this.fields.set({});
    this.dialogError.set('');
    this.busy.set(true);
    this.api.void(this.id(), this.reason).subscribe({ next: (i) => this.done(i, 'Invoice voided'), error: (e) => this.failed(e) });
  }

  remind(): void {
    this.actionError.set('');
    this.busy.set(true);
    this.api.remind(this.id()).subscribe({
      next: (i) => {
        this.busy.set(false);
        this.invoice.set(i);
        this.toast.show('Reminder sent to the client');
      },
      error: (e) => this.failed(e, false),
    });
  }
}
