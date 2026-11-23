import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { PRIORITIES, TICKET_PRIORITY, TICKET_STATUS } from '../../core/format';
import { NewTicket, Project, SlaRun, TicketPriority, TicketStatus, TicketSummary } from '../../core/models';
import { canRaiseTicket, canRunJobs, isManagement } from '../../core/permissions';
import { ProjectsService } from '../../core/projects.service';
import { SessionService } from '../../core/session.service';
import { TicketsService } from '../../core/tickets.service';
import { ToastService } from '../../core/toast.service';
import { TicketSlaComponent } from './ticket-sla.component';

const STATUS_FILTERS: { value: TicketStatus | ''; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'OPEN', label: 'Open' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'WAITING_CLIENT', label: 'Waiting for the client' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'CLOSED', label: 'Closed' },
];

@Component({
  selector: 'app-tickets-list',
  imports: [FormsModule, RouterLink, TicketSlaComponent],
  template: `
    <div class="top">
      <div>
        <h1>{{ title() }}</h1>
        <p class="sub">{{ subtitle() }}</p>
      </div>
      @if (canRaise()) {
        <div class="tools"><button class="btn primary" type="button" (click)="openNew()">New ticket</button></div>
      }
    </div>

    @if (!loading() && breachedCount() > 0 && !isClient()) {
      <div class="callout bad" role="status"><b>{{ breachedCount() }} {{ breachedCount() === 1 ? 'ticket is' : 'tickets are' }} past the answer deadline.</b> Open {{ breachedCount() === 1 ? 'it' : 'them' }} first.</div>
    }

    <div class="band four" role="list" aria-label="Ticket numbers">
      <div class="stat" role="listitem"><span class="caps">Open</span><span class="v sm num">{{ stats().open }}</span><span class="n">Not resolved yet</span></div>
      <div class="stat" role="listitem"><span class="caps">Past deadline</span><span class="v sm num" [class.bad]="stats().breached > 0">{{ stats().breached }}</span><span class="n">{{ stats().breached ? 'Answer deadline missed' : 'All within deadline' }}</span></div>
      <div class="stat" role="listitem"><span class="caps">At risk</span><span class="v sm num" [class.warn]="stats().atRisk > 0">{{ stats().atRisk }}</span><span class="n">Deadline close</span></div>
      @if (isManager()) {
        <div class="stat" role="listitem"><span class="caps">Unassigned</span><span class="v sm num">{{ stats().unassigned }}</span><span class="n">Need an owner</span></div>
      } @else if (isClient()) {
        <div class="stat" role="listitem"><span class="caps">Waiting for you</span><span class="v sm num">{{ stats().waiting }}</span><span class="n">ITZETech asked a question</span></div>
      }
    </div>

    @if (canRun()) {
      <section class="card" aria-labelledby="sch">
        <header><h2 id="sch">Deadline alerts</h2><span class="muted small">Checked automatically every 15 minutes. Administrators can run the check now.</span></header>
        <div class="filters" style="margin-bottom:0">
          <div class="field" style="min-width:220px">
            <label for="sn">Pretend it is (optional)</label>
            <input id="sn" type="datetime-local" [ngModel]="runAt()" (ngModelChange)="runAt.set($event)" />
          </div>
          <button class="btn" type="button" (click)="runCheck()" [disabled]="running()">{{ running() ? 'Checking…' : 'Run SLA check now' }}</button>
        </div>
        @if (runError()) { <p class="err" role="alert">{{ runError() }}</p> }
        @if (run(); as r) {
          <div class="callout" [class.ok]="r.alerts.length > 0" [class.info]="r.alerts.length === 0" role="status" style="margin-top:12px">
            Checked {{ r.checked }} {{ r.checked === 1 ? 'ticket' : 'tickets' }}: <b>{{ r.atRisk }}</b> at risk, <b>{{ r.breached }}</b> past deadline (new alerts sent).
            @if (r.alerts.length === 0) { Nothing new to alert about (each alert is sent once per ticket). }
            @if (r.alerts.length > 0) {
              <ul style="margin:8px 0 0; padding-left:18px">
                @for (a of r.alerts; track a.ticketId) {
                  <li><a [routerLink]="['/tickets', a.ticketId]">{{ a.number }}</a>: {{ a.kind === 'BREACHED' ? 'deadline missed' : 'deadline at risk' }}, {{ a.assigneeName ? 'assigned to ' + a.assigneeName : 'not assigned' }}</li>
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
          <span class="caps" id="tst">Status</span>
          <div class="tabs" role="group" aria-labelledby="tst">
            @for (f of statusFilters; track f.value) {
              <button type="button" [attr.aria-pressed]="status() === f.value" (click)="status.set(f.value); load()">{{ f.label }}</button>
            }
          </div>
        </div>
      </div>
      <div class="filters">
        <div class="field narrow">
          <label for="tp">Priority</label>
          <select id="tp" [ngModel]="priority()" (ngModelChange)="priority.set($event); load()">
            <option value="">Any</option>
            @for (p of priorities; track p) { <option [value]="p">{{ prio(p).label }}</option> }
          </select>
        </div>
        <label class="sw"><input type="checkbox" [ngModel]="breachedOnly()" (ngModelChange)="breachedOnly.set($event); load()" /> Past deadline only</label>
        @if (isManager()) {
          <label class="sw"><input type="checkbox" [ngModel]="unassignedOnly()" (ngModelChange)="unassignedOnly.set($event); load()" /> Unassigned only</label>
          <label class="sw"><input type="checkbox" [ngModel]="mineOnly()" (ngModelChange)="mineOnly.set($event); load()" /> Assigned to me</label>
        }
      </div>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (items().length === 0 && !error()) {
        <p class="muted">{{ emptyText() }}</p>
      } @else {
        <div class="tblwrap cardtable">
          <table>
            <thead><tr><th>Ticket</th>@if (!isClient()) { <th>Client</th> }<th>Priority</th><th>Assigned to</th><th>Answer deadline</th><th>Status</th></tr></thead>
            <tbody>
              @for (t of items(); track t.id) {
                <tr class="row" [class.row-bad]="t.breached" tabindex="0" (click)="open(t)" (keydown.enter)="open(t)">
                  <td class="first"><div class="l1"><a [routerLink]="['/tickets', t.id]" (click)="$event.stopPropagation()">{{ t.subject }}</a></div><div class="l2">{{ t.number }}@if (t.projectName) { · {{ t.projectName }} }@if (t.commentCount) { · {{ t.commentCount }} {{ t.commentCount === 1 ? 'comment' : 'comments' }} }</div></td>
                  @if (!isClient()) { <td data-label="Client">{{ t.companyName }}</td> }
                  <td data-label="Priority"><span class="pill" [class]="prio(t.priority).cls">{{ prio(t.priority).label }}</span></td>
                  <td data-label="Assigned to">{{ t.assigneeName ?? 'Not assigned yet' }}</td>
                  <td data-label="Deadline"><app-ticket-sla [ticket]="t" /></td>
                  <td data-label="Status"><span class="pill" [class]="badge(t).cls">{{ badge(t).label }}</span></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    @if (adding()) {
      <div class="overlay" (click)="adding.set(false)"></div>
      <form class="modal" role="dialog" aria-modal="true" aria-labelledby="nt" (ngSubmit)="create()" novalidate>
        <h2 id="nt">New support ticket</h2>
        <p class="muted small">Tell us what is wrong or what you need changed. How fast we answer depends on the priority.</p>
        <div class="formgrid">
          <div class="field wide">
            <label for="ts">Subject</label>
            <input id="ts" name="ts" maxlength="200" placeholder="For example: The booking page shows an error" [class.invalid]="fields()['subject']" [(ngModel)]="subject" />
            @if (fields()['subject']) { <span class="fielderr">{{ fields()['subject'] }}</span> }
          </div>
          <div class="field wide">
            <label for="td">What happened?</label>
            <textarea id="td" name="td" maxlength="4000" [class.invalid]="fields()['description']" [(ngModel)]="description"></textarea>
            @if (fields()['description']) { <span class="fielderr">{{ fields()['description'] }}</span> }
          </div>
          <div class="field">
            <label for="tpr">Priority</label>
            <select id="tpr" name="tpr" [(ngModel)]="newPriority">
              @for (p of priorities; track p) { <option [value]="p">{{ prio(p).label }} (answer within {{ prio(p).sla }} h)</option> }
            </select>
          </div>
          <div class="field">
            <label for="tpj">Project (optional)</label>
            <select id="tpj" name="tpj" [(ngModel)]="projectId">
              <option value="">Not about one project</option>
              @for (p of projects(); track p.id) { <option [value]="p.id">{{ p.name }}</option> }
            </select>
          </div>
        </div>
        @if (formError()) { <p class="err" role="alert">{{ formError() }}</p> }
        <div class="tools">
          <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Sending…' : 'Send ticket' }}</button>
          <button class="btn" type="button" (click)="adding.set(false)">Cancel</button>
        </div>
      </form>
    }
  `,
})
export class TicketsListComponent implements OnInit {
  private readonly api = inject(TicketsService);
  private readonly projectsApi = inject(ProjectsService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly statusFilters = STATUS_FILTERS;
  readonly priorities = PRIORITIES;
  readonly items = signal<TicketSummary[]>([]);
  readonly projects = signal<Project[]>([]);
  readonly status = signal<TicketStatus | ''>('');
  readonly priority = signal<TicketPriority | ''>('');
  readonly breachedOnly = signal(false);
  readonly unassignedOnly = signal(false);
  readonly mineOnly = signal(false);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly adding = signal(false);
  readonly busy = signal(false);
  readonly fields = signal<Record<string, string>>({});
  readonly formError = signal('');
  subject = '';
  description = '';
  newPriority: TicketPriority = 'MEDIUM';
  projectId = '';

  readonly runAt = signal('');
  readonly running = signal(false);
  readonly run = signal<SlaRun | null>(null);
  readonly runError = signal('');

  readonly role = computed(() => this.session.user()?.role);
  readonly isClient = computed(() => this.role() === 'CLIENT');
  readonly isManager = computed(() => isManagement(this.role()));
  readonly canRaise = computed(() => canRaiseTicket(this.role()));
  readonly canRun = computed(() => canRunJobs(this.role()));
  readonly title = computed(() => (this.isClient() ? 'Support' : this.role() === 'DEVELOPER' ? 'My tickets' : 'Support tickets'));
  readonly subtitle = computed(() =>
    this.isClient()
      ? 'Report a problem or ask for a change. Each ticket has an answer deadline.'
      : this.role() === 'DEVELOPER'
        ? 'Tickets assigned to you, with their answer deadlines.'
        : 'Faults and change requests after launch, with answer deadlines.',
  );
  readonly stats = computed(() => {
    const live = this.items().filter((t) => t.status === 'OPEN' || t.status === 'IN_PROGRESS' || t.status === 'WAITING_CLIENT');
    return {
      open: live.length,
      breached: live.filter((t) => t.breached).length,
      atRisk: live.filter((t) => t.slaState === 'AT_RISK').length,
      unassigned: live.filter((t) => !t.assigneeId).length,
      waiting: live.filter((t) => t.status === 'WAITING_CLIENT').length,
    };
  });
  readonly breachedCount = computed(() => this.stats().breached);
  readonly emptyText = computed(() =>
    this.isClient() ? 'You have no tickets here. Select "New ticket" if something is wrong with your website.' : 'No tickets match.',
  );

  ngOnInit(): void {
    this.load();
  }

  prio(p: TicketPriority) {
    return TICKET_PRIORITY[p];
  }

  badge(t: TicketSummary) {
    return TICKET_STATUS[t.status];
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api
      .list({
        status: this.status(),
        priority: this.priority(),
        breached: this.breachedOnly(),
        unassigned: this.isManager() && this.unassignedOnly(),
        mine: this.isManager() && this.mineOnly(),
      })
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

  open(t: TicketSummary): void {
    void this.router.navigate(['/tickets', t.id]);
  }

  openNew(): void {
    this.subject = this.description = this.projectId = '';
    this.newPriority = 'MEDIUM';
    this.fields.set({});
    this.formError.set('');
    this.adding.set(true);
    if (this.projects().length === 0) this.projectsApi.list().subscribe({ next: (l) => this.projects.set(l), error: () => undefined });
  }

  create(): void {
    this.fields.set({});
    this.formError.set('');
    this.busy.set(true);
    const body: NewTicket = { subject: this.subject.trim(), description: this.description.trim(), priority: this.newPriority };
    if (this.projectId) body.projectId = this.projectId;
    this.api.create(body).subscribe({
      next: (t) => {
        this.busy.set(false);
        this.adding.set(false);
        this.toast.show(`Ticket ${t.number} sent`);
        void this.router.navigate(['/tickets', t.id]);
      },
      error: (e) => {
        const err = toApiError(e);
        this.busy.set(false);
        this.fields.set(err.fields);
        this.formError.set(err.code === 'VALIDATION_FAILED' ? 'Please correct the fields marked below.' : err.message);
      },
    });
  }

  runCheck(): void {
    this.running.set(true);
    this.runError.set('');
    const at = this.runAt() ? new Date(this.runAt()).toISOString() : undefined;
    this.api.runSlaCheck(at).subscribe({
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
