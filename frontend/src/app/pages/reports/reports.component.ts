import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toApiError } from '../../core/api-error';
import { PROJECT_STATUS, RENEWAL_TYPE, TICKET_PRIORITY, monthLabel, plainNumber, rwf } from '../../core/format';
import { saveBlob } from '../../core/download';
import { MonthRevenue, ReportSummary } from '../../core/models';
import { ReportsService } from '../../core/reports.service';

export interface BarGeometry {
  month: string;
  label: string;
  invoiced: number;
  paid: number;
  x: number;
  width: number;
  invoicedY: number;
  invoicedH: number;
  paidY: number;
  paidH: number;
}

/** Pure layout for the revenue chart, so it can be tested. Bars share one scale; zero months get no bar. */
export function barLayout(rows: MonthRevenue[], width: number, height: number, pad: { left: number; right: number; top: number; bottom: number }): { bars: BarGeometry[]; max: number } {
  const innerW = width - pad.left - pad.right;
  const innerH = height - pad.top - pad.bottom;
  const max = niceMax(Math.max(0, ...rows.flatMap((r) => [r.invoiced, r.paid])));
  const slot = rows.length ? innerW / rows.length : innerW;
  const barW = Math.max(4, Math.min(22, (slot - 8) / 2));
  const bars = rows.map((r, i) => {
    const cx = pad.left + slot * i + slot / 2;
    const ih = max ? (r.invoiced / max) * innerH : 0;
    const ph = max ? (r.paid / max) * innerH : 0;
    return {
      month: r.month,
      label: monthLabel(r.month),
      invoiced: r.invoiced,
      paid: r.paid,
      x: cx - barW - 1,
      width: barW,
      invoicedY: pad.top + innerH - ih,
      invoicedH: ih,
      paidY: pad.top + innerH - ph,
      paidH: ph,
    };
  });
  return { bars, max };
}

/** Rounds a maximum up to 1, 2, 2.5 or 5 times a power of ten, so the axis has tidy numbers. */
export function niceMax(v: number): number {
  if (v <= 0) return 0;
  const pow = Math.pow(10, Math.floor(Math.log10(v)));
  for (const m of [1, 2, 2.5, 5, 10]) if (v <= m * pow) return m * pow;
  return 10 * pow;
}

@Component({
  selector: 'app-reports',
  imports: [FormsModule, DatePipe],
  template: `
    <div class="top">
      <div>
        <h1>Reports</h1>
        <p class="sub">Money, projects, tickets, renewals and hours for the period you choose.</p>
      </div>
      <div class="tools">
        <button class="btn" type="button" (click)="exportCsv()" [disabled]="exporting()">{{ exporting() ? 'Preparing…' : 'Export CSV' }}</button>
      </div>
    </div>

    <section class="card" aria-label="Date range">
      <form class="filters" style="margin-bottom:0" (ngSubmit)="load()">
        <div class="field narrow">
          <label for="rf">From</label>
          <input id="rf" name="rf" type="date" [(ngModel)]="from" />
        </div>
        <div class="field narrow">
          <label for="rt">To</label>
          <input id="rt" name="rt" type="date" [(ngModel)]="to" />
        </div>
        <button class="btn primary" type="submit" [disabled]="loading()">Apply</button>
        <button class="btn" type="button" (click)="clear()" [disabled]="loading() || (!from && !to)">All time</button>
      </form>
      <p class="muted small" style="margin-top:8px">
        The period limits invoices (by issue date), projects (by start date), tickets (by creation date) and hours (by work date). Renewals and project profit always show today.
      </p>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
    </section>

    @if (loading() && !report()) {
      <p class="muted loading">Loading…</p>
    }
    @if (report(); as r) {
      <div class="band four" role="list" aria-label="Money">
        <div class="stat" role="listitem"><span class="caps">Invoiced</span><span class="v sm num">{{ money(r.revenue.invoiced) }}</span><span class="n">{{ r.revenue.invoiceCount }} invoices</span></div>
        <div class="stat" role="listitem"><span class="caps">Collected</span><span class="v sm ok num">{{ money(r.revenue.paid) }}</span><span class="n">{{ pct(r.revenue.paid, r.revenue.invoiced) }}% of invoiced</span></div>
        <div class="stat" role="listitem"><span class="caps">Outstanding</span><span class="v sm num">{{ money(r.revenue.outstanding) }}</span><span class="n">Still to be paid</span></div>
        <div class="stat" role="listitem"><span class="caps">Overdue</span><span class="v sm num" [class.bad]="r.revenue.overdueCount > 0">{{ money(r.revenue.overdue) }}</span><span class="n">{{ r.revenue.overdueCount }} past due</span></div>
      </div>
      <div class="band four" role="list" aria-label="Delivery and care">
        <div class="stat" role="listitem"><span class="caps">Projects</span><span class="v sm num">{{ r.projects.total }}</span><span class="n">{{ r.projects.averageProgress === null ? 'No projects' : 'Average progress ' + r.projects.averageProgress + '%' }}</span></div>
        <div class="stat" role="listitem"><span class="caps">Tickets open</span><span class="v sm num">{{ r.tickets.open }}</span><span class="n">{{ r.tickets.breached }} past deadline, {{ r.tickets.total }} in total</span></div>
        <div class="stat" role="listitem"><span class="caps">Resolved on time</span><span class="v sm num">{{ r.tickets.resolvedWithinSlaPercent === null ? '—' : r.tickets.resolvedWithinSlaPercent + '%' }}</span><span class="n">{{ r.tickets.averageResolutionHours === null ? 'Nothing resolved yet' : 'Average ' + plain(r.tickets.averageResolutionHours) + ' h to resolve' }}</span></div>
        <div class="stat" role="listitem"><span class="caps">Renewals needing action</span><span class="v sm num" [class.warn]="r.renewals.expiring + r.renewals.expired > 0">{{ r.renewals.expiring + r.renewals.expired }}</span><span class="n">{{ r.renewals.expired }} expired, {{ r.renewals.expiring }} within 30 days</span></div>
      </div>

      <section class="card" aria-labelledby="rm">
        <header>
          <h2 id="rm">Revenue by month</h2>
          <div class="tabs" role="group" aria-label="Number of months">
            @for (n of monthChoices; track n) { <button type="button" [attr.aria-pressed]="months() === n" (click)="setMonths(n)">{{ n }} months</button> }
          </div>
        </header>
        @if (monthsError()) { <p class="err" role="alert">{{ monthsError() }}</p> }
        @if (layout(); as l) {
          <svg class="chart" [attr.viewBox]="'0 0 ' + W + ' ' + H" role="img" [attr.aria-label]="chartLabel()">
            <defs>
              <pattern id="hatch" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
                <rect width="6" height="6" fill="var(--surface)" />
                <line x1="0" y1="0" x2="0" y2="6" stroke="var(--ok)" stroke-width="3" />
              </pattern>
            </defs>
            @for (t of ticks(); track t.v) {
              <line class="grid" [attr.x1]="PAD.left" [attr.x2]="W - PAD.right" [attr.y1]="t.y" [attr.y2]="t.y" />
              <text [attr.x]="PAD.left - 6" [attr.y]="t.y + 4" text-anchor="end">{{ short(t.v) }}</text>
            }
            <line class="axis" [attr.x1]="PAD.left" [attr.x2]="W - PAD.right" [attr.y1]="H - PAD.bottom" [attr.y2]="H - PAD.bottom" />
            @for (b of l.bars; track b.month) {
              <rect class="b-inv" [attr.x]="b.x" [attr.y]="b.invoicedY" [attr.width]="b.width" [attr.height]="b.invoicedH" rx="2"><title>{{ b.label }}: invoiced {{ money(b.invoiced) }}</title></rect>
              <rect class="b-paid" [attr.x]="b.x + b.width + 2" [attr.y]="b.paidY" [attr.width]="b.width" [attr.height]="b.paidH" rx="2"><title>{{ b.label }}: paid {{ money(b.paid) }}</title></rect>
              <text [attr.x]="b.x + b.width + 1" [attr.y]="H - PAD.bottom + 16" text-anchor="middle">{{ shortMonth(b.month) }}</text>
            }
          </svg>
          <div class="legend"><span><i class="inv"></i>Invoiced (solid)</span><span><i class="paid"></i>Paid (striped)</span></div>
        }
        <details class="alt" [open]="showTable()" (toggle)="showTable.set($any($event.target).open)">
          <summary>Show the same numbers as a table</summary>
          <div class="tblwrap cardtable" style="margin-top:8px">
            <table style="min-width:0">
              <thead><tr><th>Month</th><th class="right">Invoiced</th><th class="right">Paid</th></tr></thead>
              <tbody>
                @for (m of byMonth(); track m.month) {
                  <tr><td class="first">{{ label(m.month) }}</td><td class="num right" data-label="Invoiced">{{ money(m.invoiced) }}</td><td class="num right" data-label="Paid">{{ money(m.paid) }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </details>
      </section>

      <div class="grid2">
        <section class="card" aria-labelledby="rc">
          <header><h2 id="rc">Revenue by client</h2></header>
          @if (r.revenue.byClient.length === 0) {
            <p class="muted empty">No invoices in this period.</p>
          } @else {
            <div class="tblwrap cardtable">
              <table style="min-width:0">
                <thead><tr><th>Client</th><th class="right">Invoiced</th><th class="right">Paid</th></tr></thead>
                <tbody>
                  @for (c of r.revenue.byClient; track c.companyId) {
                    <tr><td class="first l1">{{ c.companyName }}</td><td class="num right" data-label="Invoiced">{{ money(c.invoiced) }}</td><td class="num right" data-label="Paid">{{ money(c.paid) }}</td></tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>

        <section class="card" aria-labelledby="rh">
          <header><h2 id="rh">Hours by developer</h2><span class="muted small num">{{ plain(r.hours.total) }} h in total</span></header>
          @if (r.hours.byDeveloper.length === 0) {
            <p class="muted empty">No hours logged in this period.</p>
          } @else {
            <div class="hbars">
              @for (d of r.hours.byDeveloper; track d.userId) {
                <div class="hbar">
                  <span class="name" [title]="d.fullName">{{ d.fullName }}</span>
                  <div class="track" aria-hidden="true"><i [style.width.%]="(d.hours / maxHours()) * 100"></i></div>
                  <span class="num">{{ plain(d.hours) }} h</span>
                </div>
              }
            </div>
          }
        </section>

        <section class="card" aria-labelledby="rtk">
          <header><h2 id="rtk">Tickets by priority</h2></header>
          <div class="tblwrap cardtable">
            <table style="min-width:0">
              <thead><tr><th>Priority</th><th class="right">Total</th><th class="right">Open</th><th class="right">Past deadline</th></tr></thead>
              <tbody>
                @for (p of r.tickets.byPriority; track p.priority) {
                  <tr><td class="first"><span class="pill" [class]="prio(p.priority).cls">{{ prio(p.priority).label }}</span></td><td class="num right" data-label="Total">{{ p.total }}</td><td class="num right" data-label="Open">{{ p.open }}</td><td class="num right" data-label="Past deadline">{{ p.breached }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>

        <section class="card" aria-labelledby="rr">
          <header><h2 id="rr">Renewals by type</h2><span class="muted small">Today, not limited by the period</span></header>
          <div class="tblwrap cardtable">
            <table style="min-width:0">
              <thead><tr><th>Type</th><th class="right">Active</th><th class="right">Expiring</th><th class="right">Expired</th></tr></thead>
              <tbody>
                @for (t of r.renewals.byType; track t.type) {
                  <tr><td class="first">{{ renewalType(t.type) }}</td><td class="num right" data-label="Active">{{ t.active }}</td><td class="num right" data-label="Expiring">{{ t.expiring }}</td><td class="num right" data-label="Expired">{{ t.expired }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <section class="card" aria-labelledby="rp">
        <header><h2 id="rp">Project profit</h2><span class="muted small">Cost is logged or planned hours at {{ money(r.hourlyRate) }} per hour. Today, not limited by the period.</span></header>
        @if (r.projectProfit.length === 0) {
          <p class="muted empty">No projects yet.</p>
        } @else {
          <div class="tblwrap cardtable">
            <table>
              <thead><tr><th>Project</th><th>Status</th><th class="right">Budget</th><th class="right">Hours logged</th><th class="right">Cost so far</th><th class="right">Forecast cost</th><th class="right">Forecast margin</th></tr></thead>
              <tbody>
                @for (p of r.projectProfit; track p.projectId) {
                  <tr>
                    <td class="first"><div class="l1">{{ p.projectName }}</div><div class="l2">{{ p.companyName }}</div></td>
                    <td data-label="Status"><span class="pill" [class]="projStatus(p.status).cls">{{ projStatus(p.status).label }}</span></td>
                    <td class="num right" data-label="Budget">{{ money(p.budget) }}</td>
                    <td class="num right" data-label="Hours logged">{{ plain(p.loggedHours) }} of {{ p.plannedHours }} h</td>
                    <td class="num right" data-label="Cost so far">{{ money(p.costSoFar) }}</td>
                    <td class="num right" data-label="Forecast cost">{{ money(p.forecastCost) }}</td>
                    <td class="num right" data-label="Forecast margin">
                      @if (p.forecastMarginPercent === null) { — } @else {
                        <b [style.color]="p.forecastMarginPercent < 0 ? 'var(--bad)' : ''">{{ p.forecastMarginPercent }}%</b>
                        @if (p.forecastMarginPercent < 0) { <span class="small"> (loss)</span> }
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
        <p class="muted small" style="margin-top:10px">Generated {{ r.generatedAt | date: 'd MMM y, HH:mm' }}.</p>
      </section>
    }
  `,
})
export class ReportsComponent implements OnInit {
  private readonly api = inject(ReportsService);

  readonly W = 720;
  readonly H = 280;
  readonly PAD = { left: 64, right: 12, top: 12, bottom: 28 };
  readonly monthChoices = [6, 12, 24];

  readonly report = signal<ReportSummary | null>(null);
  readonly byMonth = signal<MonthRevenue[]>([]);
  readonly months = signal(12);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly monthsError = signal('');
  readonly exporting = signal(false);
  readonly showTable = signal(false);
  from = '';
  to = '';

  readonly layout = computed(() => (this.byMonth().length ? barLayout(this.byMonth(), this.W, this.H, this.PAD) : null));
  readonly ticks = computed(() => {
    const l = this.layout();
    if (!l || !l.max) return [];
    const innerH = this.H - this.PAD.top - this.PAD.bottom;
    return [0, 0.25, 0.5, 0.75, 1].map((f) => ({ v: l.max * f, y: this.PAD.top + innerH - f * innerH }));
  });
  readonly maxHours = computed(() => Math.max(1, ...(this.report()?.hours.byDeveloper.map((d) => d.hours) ?? [1])));
  readonly chartLabel = computed(() => {
    const rows = this.byMonth();
    const inv = rows.reduce((s, r) => s + r.invoiced, 0);
    const paid = rows.reduce((s, r) => s + r.paid, 0);
    return `Bar chart of invoiced and paid money for the last ${rows.length} months: ${rwf(inv) || '0 RWF'} invoiced and ${rwf(paid) || '0 RWF'} paid. The same numbers are in the table below.`;
  });

  ngOnInit(): void {
    this.load();
    this.loadMonths();
  }

  money(n: number): string {
    return rwf(n) || '0 RWF';
  }

  plain = plainNumber;
  label = monthLabel;

  shortMonth(ym: string): string {
    return monthLabel(ym).split(' ')[0] + (ym.endsWith('-01') ? ' ' + ym.slice(2, 4) : '');
  }

  short(v: number): string {
    if (v >= 1_000_000) return plainNumber(v / 1_000_000) + 'M';
    if (v >= 1_000) return plainNumber(v / 1_000) + 'k';
    return String(v);
  }

  pct(part: number, whole: number): number {
    return whole > 0 ? Math.round((100 * part) / whole) : 0;
  }

  prio(p: keyof typeof TICKET_PRIORITY) {
    return TICKET_PRIORITY[p];
  }

  projStatus(s: keyof typeof PROJECT_STATUS) {
    return PROJECT_STATUS[s];
  }

  renewalType(t: keyof typeof RENEWAL_TYPE): string {
    return RENEWAL_TYPE[t];
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.summary(this.from || undefined, this.to || undefined).subscribe({
      next: (r) => {
        this.report.set(r);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(toApiError(e).message);
      },
    });
  }

  clear(): void {
    this.from = this.to = '';
    this.load();
  }

  setMonths(n: number): void {
    this.months.set(n);
    this.loadMonths();
  }

  private loadMonths(): void {
    this.monthsError.set('');
    this.api.revenueByMonth(this.months()).subscribe({
      next: (rows) => this.byMonth.set(rows),
      error: (e) => this.monthsError.set(toApiError(e).message),
    });
  }

  exportCsv(): void {
    this.exporting.set(true);
    this.error.set('');
    this.api.summaryCsv(this.from || undefined, this.to || undefined).subscribe({
      next: (blob) => {
        this.exporting.set(false);
        saveBlob(blob, 'reports-summary.csv');
      },
      error: (e) => {
        this.exporting.set(false);
        this.error.set(toApiError(e).message);
      },
    });
  }
}
