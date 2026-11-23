import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardCounts, DashboardService } from '../../core/dashboard.service';
import { Stage3Dashboard } from '../../core/models';
import { NotificationsService } from '../../core/notifications.service';
import { rwf } from '../../core/format';
import { ICON } from '../../core/nav-icons';
import { SessionService } from '../../core/session.service';

/** A greeting for the role, the numbers that need attention, and the latest notifications. */
@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe],
  template: `
    <div class="top">
      <div>
        <h1>{{ title() }}</h1>
        <p class="sub">{{ subtitle() }}</p>
      </div>
    </div>

    @if (counts(); as c) {
      <div class="band" role="list" aria-label="At a glance">
        @if (c.openRequests !== undefined) {
          <a class="stat tone-blue" role="listitem" routerLink="/requests">
            <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.requests" /></svg></span>
            <span class="caps">Open requests</span><span class="v num">{{ c.openRequests }}</span>
            <span class="n">{{ isClient() ? 'Waiting for a quote or your answer' : c.needQuote + ' need a quote' }}</span>
          </a>
        }
        <a class="stat tone-teal" role="listitem" routerLink="/projects">
          <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.projects" /></svg></span>
          <span class="caps">Active projects</span><span class="v num">{{ c.activeProjects }}</span><span class="n">Not launched yet</span>
        </a>
        @if (c.myTasks !== undefined) {
          <a class="stat tone-violet" role="listitem" routerLink="/tasks">
            <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.tasks" /></svg></span>
            <span class="caps">My tasks</span><span class="v num">{{ c.myTasks }}</span><span class="n">{{ c.lateTasks ? c.lateTasks + ' late' : 'Not done yet' }}</span>
          </a>
        }
        @if (c.pendingApprovals !== undefined) {
          <a class="stat tone-amber" role="listitem" routerLink="/projects">
            <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.check" /></svg></span>
            <span class="caps">Pending approvals</span><span class="v num">{{ c.pendingApprovals }}</span><span class="n">{{ isClient() ? 'Waiting for your answer' : 'Waiting for the client' }}</span>
          </a>
        }
      </div>
    } @else if (countsFailed()) {
      <p class="err" role="alert">Could not load the numbers. Please reload the page.</p>
    }

    @if (care(); as s) {
      <div class="band" role="list" aria-label="Money and care">
        @if (s.invoices; as inv) {
          <a class="stat tone-green" role="listitem" routerLink="/invoices">
            <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.invoices" /></svg></span>
            <span class="caps">{{ isClient() ? 'Balance due' : 'Unpaid invoices' }}</span><span class="v num">{{ inv.unpaidCount }}</span>
            <span class="n">{{ money(inv.unpaidAmount) }} to be paid</span>
          </a>
          <a class="stat" [class.tone-red]="inv.overdueCount > 0" [class.tone-green]="inv.overdueCount === 0" role="listitem" routerLink="/invoices">
            <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.alert" /></svg></span>
            <span class="caps">Overdue invoices</span><span class="v num" [style.color]="inv.overdueCount ? 'var(--bad)' : ''">{{ inv.overdueCount }}</span>
            <span class="n">{{ inv.overdueCount ? money(inv.overdueAmount) + ' overdue' : 'None overdue' }}@if (inv.draftCount) { · {{ inv.draftCount }} {{ inv.draftCount === 1 ? 'draft' : 'drafts' }} }</span>
          </a>
        }
        @if (s.renewals; as rn) {
          <a class="stat tone-amber" role="listitem" routerLink="/renewals">
            <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.renewals" /></svg></span>
            <span class="caps">Expiring renewals</span><span class="v num" [style.color]="rn.expiringCount ? 'var(--warn)' : ''">{{ rn.expiringCount }}</span>
            <span class="n">{{ rn.expiredCount ? rn.expiredCount + ' already expired' : 'Within 30 days' }}</span>
          </a>
        }
        @if (s.tickets; as tk) {
          <a class="stat tone-blue" role="listitem" routerLink="/tickets">
            <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.tickets" /></svg></span>
            <span class="caps">Open tickets</span><span class="v num">{{ tk.openCount }}</span>
            <span class="n">
              @if (tk.breachedCount !== undefined) { {{ tk.breachedCount ? tk.breachedCount + ' past deadline' : 'All within deadline' }}@if (tk.unassignedCount) { · {{ tk.unassignedCount }} unassigned } }
              @else { {{ tk.waitingOnClientCount ? tk.waitingOnClientCount + ' waiting for you' : 'Being looked after' }} }
            </span>
          </a>
          @if (tk.breachedCount !== undefined) {
            <a class="stat" [class.tone-red]="tk.breachedCount > 0" [class.tone-green]="tk.breachedCount === 0" role="listitem" routerLink="/tickets">
              <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.clock" /></svg></span>
              <span class="caps">Tickets past deadline</span><span class="v num" [style.color]="tk.breachedCount ? 'var(--bad)' : ''">{{ tk.breachedCount }}</span>
              <span class="n">{{ tk.atRiskCount ? tk.atRiskCount + ' more at risk' : 'None at risk' }}</span>
            </a>
          }
        }
      </div>
    }

    <div class="grid2">
      <section class="card" aria-labelledby="n">
        <header>
          <h2 id="n">Notifications</h2>
          <span class="chip">{{ notes.unread() }} unread</span>
        </header>
        @if (!notes.loaded()) {
          <p class="muted loading">Loading…</p>
        } @else if (notes.failed()) {
          <p class="err">Could not load notifications.</p>
        } @else if (notes.items().length === 0) {
          <p class="muted empty">Nothing new. We will tell you here when something needs you.</p>
        } @else {
          <div class="list nlist">
            @for (n of latest(); track n.id) {
              <div class="item" [class.unread]="!n.read">
                <button class="itembtn" type="button" (click)="notes.open(n)">
                  <span class="dot" [style.visibility]="n.read ? 'hidden' : 'visible'" aria-hidden="true"></span>
                  <div class="grow"><div class="l1">{{ n.message }}</div><div class="l2 muted small">{{ n.createdAt | date: 'd MMM y, HH:mm' }}</div></div>
                  @if (!n.read) { <span class="pill p-info">New</span> }
                </button>
              </div>
            }
          </div>
          <p style="margin-top:14px"><a routerLink="/notifications">See all notifications</a></p>
        }
      </section>

      <div class="stack">
        @if (health().length > 0) {
          <section class="card" aria-labelledby="h">
            <header><h2 id="h">Needs attention</h2></header>
            <div class="health">
              @for (r of health(); track r.label) {
                <div class="row" [class]="r.tone">
                  <div><span>{{ r.label }}</span><b class="num">{{ r.value }} of {{ r.total }}</b></div>
                  <div class="meter" role="img" [attr.aria-label]="r.label + ': ' + r.value + ' of ' + r.total"><i [style.width.%]="r.pct"></i></div>
                </div>
              }
            </div>
          </section>
        }

        <section class="card" aria-labelledby="w">
          <header><h2 id="w">{{ isAdmin() ? 'Administration' : 'Where to start' }}</h2></header>
          <div class="tiles">
            @if (isAdmin()) {
              @for (t of adminTiles; track t.link) {
                <a class="tile" [class]="t.tone" [routerLink]="t.link">
                  <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons[t.icon]" /></svg></span>
                  <span><b>{{ t.title }}</b><span class="d">{{ t.text }}</span></span>
                </a>
              }
            }
            @for (t of tiles(); track t.link + t.title) {
              <a class="tile" [class]="t.tone" [routerLink]="t.link">
                <span class="ic" aria-hidden="true"><svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons[t.icon]" /></svg></span>
                <span><b>{{ t.title }}</b><span class="d">{{ t.text }}</span></span>
              </a>
            }
          </div>
        </section>
      </div>
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  private readonly session = inject(SessionService);
  private readonly dashboard = inject(DashboardService);
  readonly notes = inject(NotificationsService);

  readonly icons = ICON;
  readonly counts = signal<DashboardCounts | null>(null);
  readonly countsFailed = signal(false);
  readonly care = signal<Stage3Dashboard | null>(null);

  readonly role = computed(() => this.session.user()?.role);
  readonly firstName = computed(() => (this.session.user()?.fullName ?? '').split(' ')[0]);
  readonly isAdmin = computed(() => this.role() === 'ADMIN');
  readonly isClient = computed(() => this.role() === 'CLIENT');
  readonly latest = computed(() => this.notes.items().slice(0, 5));

  readonly adminTiles: Tile[] = [
    { link: '/admin/users', tone: 'tone-violet', icon: 'users', title: 'Users and access', text: 'Add staff, change roles, deactivate or reset a password.' },
    { link: '/admin/audit', tone: 'tone-teal', icon: 'shield', title: 'Audit trail', text: 'Every important action, who did it and when.' },
  ];

  /** The quick-action tiles for the signed-in role. */
  readonly tiles = computed<Tile[]>(() => {
    switch (this.role()) {
      case 'CLIENT':
        return [
          { link: '/requests', tone: 'tone-blue', icon: 'requests', title: 'Requests and quotes', text: 'Ask for new work and answer quotes.' },
          { link: '/projects', tone: 'tone-teal', icon: 'projects', title: 'My projects', text: 'Progress, approvals and change requests.' },
          { link: '/invoices', tone: 'tone-green', icon: 'invoices', title: 'Invoices', text: 'What you owe and what you have paid.' },
          { link: '/tickets', tone: 'tone-amber', icon: 'tickets', title: 'Support', text: 'Report a problem and follow the answer.' },
        ];
      case 'DEVELOPER':
        return [
          { link: '/tasks', tone: 'tone-violet', icon: 'tasks', title: 'My tasks', text: 'Start work, hand it in and log your hours.' },
          { link: '/projects', tone: 'tone-teal', icon: 'projects', title: 'Projects', text: 'The projects you work on.' },
          { link: '/tickets', tone: 'tone-amber', icon: 'tickets', title: 'My tickets', text: 'Support tickets assigned to you.' },
        ];
      default:
        return [
          { link: '/requests', tone: 'tone-blue', icon: 'requests', title: 'Requests and quotes', text: 'Write and revise quotes.' },
          { link: '/projects', tone: 'tone-teal', icon: 'projects', title: 'Projects', text: 'Start projects, assign tasks and review work.' },
          { link: '/invoices', tone: 'tone-green', icon: 'invoices', title: 'Invoices', text: 'Bill clients and record payments.' },
          { link: '/renewals', tone: 'tone-amber', icon: 'renewals', title: 'Renewals and support tickets', text: 'Keep websites running.' },
        ];
    }
  });

  /** Progress bars built from numbers already on the page: how much of the work is late, overdue or past its deadline. */
  readonly health = computed(() => {
    const rows: { label: string; value: number; total: number; pct: number; tone: string }[] = [];
    const add = (label: string, value: number | undefined, total: number | undefined, tone: string) => {
      if (value === undefined || total === undefined || total <= 0) return;
      rows.push({ label, value, total, pct: Math.min(100, Math.round((value / total) * 100)), tone });
    };
    const c = this.counts();
    const s = this.care();
    add('Late tasks', c?.lateTasks, c?.myTasks, 'tone-red');
    add('Overdue invoices', s?.invoices?.overdueCount, s?.invoices?.unpaidCount, 'tone-red');
    add('Tickets past deadline', s?.tickets?.breachedCount, s?.tickets?.openCount, 'tone-red');
    const renewals = s?.renewals ? s.renewals.expiringCount + s.renewals.expiredCount : 0;
    add('Renewals expired', s?.renewals?.expiredCount, renewals, 'tone-amber');
    return rows;
  });

  readonly title = computed(() => {
    switch (this.role()) {
      case 'CLIENT':
        return `Welcome, ${this.firstName()}`;
      case 'DEVELOPER':
        return `Hello, ${this.firstName()}`;
      default:
        return 'Delivery and care overview';
    }
  });

  readonly subtitle = computed(() => {
    switch (this.role()) {
      case 'CLIENT':
        return 'Your requests, projects and approvals in one place.';
      case 'DEVELOPER':
        return 'Your tasks and hours.';
      default:
        return 'Requests, projects and tasks that need action.';
    }
  });

  money(n: number): string {
    return rwf(n) || '0 RWF';
  }

  ngOnInit(): void {
    const role = this.role();
    if (!role) return;
    this.dashboard.stage3().subscribe({ next: (c) => this.care.set(c), error: () => undefined });
    this.dashboard.counts(role).subscribe({
      next: (c) => this.counts.set(c),
      error: () => this.countsFailed.set(true),
    });
  }
}

interface Tile {
  link: string;
  tone: string;
  icon: keyof typeof ICON;
  title: string;
  text: string;
}
