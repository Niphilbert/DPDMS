import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { INVOICE_STATUS, rwf } from '../../core/format';
import { InvoicesService } from '../../core/invoices.service';
import { Invoice, InvoiceSummary, Project, ProjectMoney } from '../../core/models';
import { canManageMoney } from '../../core/permissions';
import { SessionService } from '../../core/session.service';
import { ToastService } from '../../core/toast.service';
import { InvoiceFormComponent } from '../invoices/invoice-form.component';

/** The Money tab of a project: budget, invoiced, paid and balance, and the project's invoices. Not for developers. */
@Component({
  selector: 'app-project-money',
  imports: [DatePipe, RouterLink, InvoiceFormComponent],
  template: `
    <section class="card" aria-labelledby="mn">
      <header>
        <h2 id="mn">Money</h2>
        @if (canManage()) { <button class="btn primary" type="button" (click)="creating.set(true)">New invoice</button> }
      </header>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (money(); as m) {
        <div class="moneygrid" role="list" aria-label="Project money">
          <div role="listitem"><span class="caps">Budget</span><span class="v">{{ fmt(m.budget) }}</span></div>
          <div role="listitem"><span class="caps">Invoiced</span><span class="v">{{ fmt(m.invoiced) }}</span></div>
          <div role="listitem"><span class="caps">Paid</span><span class="v">{{ fmt(m.paid) }}</span></div>
          <div role="listitem"><span class="caps">Balance due</span><span class="v">{{ fmt(m.balanceDue) }}</span></div>
          <div role="listitem"><span class="caps">Not invoiced yet</span><span class="v">{{ fmt(m.unbilled) }}</span></div>
          @if (m.overdueAmount > 0) { <div role="listitem"><span class="caps">Overdue</span><span class="v" style="color:var(--bad)">{{ fmt(m.overdueAmount) }}</span></div> }
        </div>
      }
    </section>

    <section class="card" aria-labelledby="pi">
      <header><h2 id="pi">Invoices of this project</h2></header>
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (invoices().length === 0) {
        <p class="muted">{{ canManage() ? 'No invoices yet. Select "New invoice" to bill hours or the remaining budget.' : 'No invoices have been sent for this project yet.' }}</p>
      } @else {
        <div class="tblwrap cardtable">
          <table>
            <thead><tr><th>Invoice</th><th>Due</th><th class="right">Total</th><th class="right">Balance</th><th>Status</th></tr></thead>
            <tbody>
              @for (i of invoices(); track i.id) {
                <tr class="row" [class.row-bad]="i.overdue" tabindex="0" (click)="open(i)" (keydown.enter)="open(i)">
                  <td class="first"><div class="l1"><a [routerLink]="['/invoices', i.id]" (click)="$event.stopPropagation()">{{ i.number }}</a></div><div class="l2">{{ i.note || 'Issued ' + (i.issueDate | date: 'd MMM y') }}</div></td>
                  <td class="num" data-label="Due">{{ i.dueDate | date: 'd MMM y' }}</td>
                  <td class="num right" data-label="Total">{{ fmt(i.total) }}</td>
                  <td class="num right" data-label="Balance">{{ i.status === 'DRAFT' ? '—' : fmt(i.balance) }}</td>
                  <td data-label="Status"><span class="pill" [class]="badge(i).cls">{{ badge(i).label }}</span></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    @if (creating()) {
      <app-invoice-form [presetProjectId]="project().id" (saved)="created($event)" (cancelled)="creating.set(false)" />
    }
  `,
})
export class ProjectMoneyComponent implements OnInit {
  readonly project = input.required<Project>();

  private readonly api = inject(InvoicesService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly money = signal<ProjectMoney | null>(null);
  readonly invoices = signal<InvoiceSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly creating = signal(false);
  readonly canManage = computed(() => canManageMoney(this.session.user()?.role));

  ngOnInit(): void {
    const id = this.project().id;
    this.api.projectMoney(id).subscribe({ next: (m) => this.money.set(m), error: (e) => this.error.set(toApiError(e).message) });
    this.api.forProject(id).subscribe({
      next: (list) => {
        this.invoices.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(toApiError(e).message);
      },
    });
  }

  fmt(n: number): string {
    return rwf(n) || '0 RWF';
  }

  badge(i: InvoiceSummary) {
    return INVOICE_STATUS[i.displayStatus];
  }

  open(i: InvoiceSummary): void {
    void this.router.navigate(['/invoices', i.id]);
  }

  created(inv: Invoice): void {
    this.creating.set(false);
    this.toast.show(`Draft ${inv.number} saved`);
    void this.router.navigate(['/invoices', inv.id]);
  }
}
