import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { PRIORITIES, TICKET_PRIORITY, TICKET_STATUS } from '../../core/format';
import { Ticket, TicketPriority, TicketStatus } from '../../core/models';
import { canWriteInternalNote, moveLabel, ticketActions } from '../../core/permissions';
import { SessionService } from '../../core/session.service';
import { Person, StaffService } from '../../core/staff.service';
import { TicketsService } from '../../core/tickets.service';
import { ToastService } from '../../core/toast.service';
import { TicketSlaComponent } from './ticket-sla.component';

@Component({
  selector: 'app-ticket-detail',
  imports: [FormsModule, DatePipe, RouterLink, TicketSlaComponent],
  template: `
    <div class="top">
      <div>
        <p class="small"><a routerLink="/tickets">← {{ isClient() ? 'Support' : 'Support tickets' }}</a></p>
        <h1>{{ ticket()?.subject ?? 'Ticket' }}</h1>
        @if (ticket(); as t) {
          <p class="sub">
            <span class="mono">{{ t.number }}</span>
            <span class="pill" [class]="statusBadge().cls">{{ statusBadge().label }}</span>
            <span class="pill" [class]="prio(t.priority).cls">{{ prio(t.priority).label }} priority</span>
          </p>
        }
      </div>
    </div>

    @if (loading()) {
      <p class="muted loading">Loading…</p>
    } @else if (loadError()) {
      <div class="callout bad" role="alert">{{ loadError() }}</div>
    } @else if (ticket(); as t) {
      @if (actionError()) { <div class="callout bad" role="alert">{{ actionError() }}</div> }
      @if (t.status === 'RESOLVED') {
        <div class="callout ok" role="status">This ticket was resolved{{ t.resolvedAt ? ' on ' + (t.resolvedAt | date: 'd MMM y, HH:mm') : '' }}.
          @if (isClient()) { If the problem is not fixed you can reopen it once, or close it if you are happy. }
        </div>
      } @else if (t.status === 'CLOSED') {
        <div class="callout info" role="status">This ticket is closed. Open a new ticket if the problem comes back.</div>
      } @else if (t.status === 'WAITING_CLIENT' && isClient()) {
        <div class="callout warn" role="status">ITZETech is waiting for your answer. Write a comment below to continue.</div>
      }

      <div class="grid2">
        <div class="stack">
          <section class="card" aria-labelledby="td">
            <header><h2 id="td">What happened</h2></header>
            <p class="note">{{ t.description }}</p>
          </section>

          <section class="card" aria-labelledby="tc">
            <header><h2 id="tc">Conversation</h2><span class="muted small">{{ t.comments.length }} {{ t.comments.length === 1 ? 'message' : 'messages' }}</span></header>
            @if (t.comments.length === 0) {
              <p class="muted empty">No messages yet.</p>
            } @else {
              <div class="thread">
                @for (c of t.comments; track c.id) {
                  <article class="msg" [class.internal]="c.internal" [class.mine]="c.authorId === me()?.id && !c.internal">
                    <div class="who2">
                      <b>{{ c.authorName }}</b>
                      <span class="muted">{{ roleName(c.authorRole) }}</span>
                      @if (c.internal) { <span class="internal-tag">Internal note · staff only</span> }
                      <span class="muted num" style="margin-left:auto">{{ c.createdAt | date: 'd MMM, HH:mm' }}</span>
                    </div>
                    <div class="body">{{ c.body }}</div>
                  </article>
                }
              </div>
            }

            @if (acts().comment) {
              <form (ngSubmit)="comment()" novalidate style="margin-top:16px; display:flex; flex-direction:column; gap:10px">
                <div class="field">
                  <label for="cb">{{ internal() ? 'Internal note' : 'Write a reply' }}</label>
                  <textarea id="cb" name="cb" maxlength="4000" [class.invalid]="fields()['body']" [(ngModel)]="body" [placeholder]="internal() ? 'Only ITZETech staff will see this note' : 'Your message'"></textarea>
                  @if (fields()['body']) { <span class="fielderr">{{ fields()['body'] }}</span> }
                </div>
                @if (acts().internalNote) {
                  <label class="sw"><input type="checkbox" name="ci" [ngModel]="internal()" (ngModelChange)="internal.set($event)" /> Internal note: hide this from the client</label>
                }
                <div class="tools"><button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Sending…' : internal() ? 'Add internal note' : 'Send reply' }}</button></div>
              </form>
            } @else if (t.status === 'CLOSED') {
              <p class="muted small" style="margin-top:12px">Closed tickets cannot receive new messages.</p>
            }
          </section>
        </div>

        <div class="stack">
          <section class="card" aria-labelledby="tdl">
            <header><h2 id="tdl">Answer deadline</h2></header>
            <div style="display:flex; flex-direction:column; gap:6px">
              <app-ticket-sla [ticket]="t" />
              <p class="muted small">{{ prio(t.priority).label }} tickets get an answer within {{ t.slaHours }} hours (nights and weekends count).
                @if (t.reopenCount > 0) { This ticket was reopened, so the clock restarted. }
              </p>
              @if (t.resolvedWithinSla !== null) { <p class="small">{{ t.resolvedWithinSla ? 'Resolved within the deadline.' : 'Resolved after the deadline.' }}</p> }
            </div>
          </section>

          <section class="card" aria-labelledby="tdt">
            <header><h2 id="tdt">Details</h2></header>
            <dl>
              @if (!isClient()) { <dt>Client</dt><dd>{{ t.companyName }}</dd> }
              @if (t.projectName) { <dt>Project</dt><dd><a [routerLink]="['/projects', t.projectId]">{{ t.projectName }}</a></dd> }
              <dt>Raised by</dt><dd>{{ t.createdByName }}</dd>
              <dt>Raised on</dt><dd>{{ t.createdAt | date: 'd MMM y, HH:mm' }}</dd>
              <dt>Assigned to</dt><dd>{{ t.assigneeName ?? 'Not assigned yet' }}</dd>
              <dt>Deadline</dt><dd>{{ t.dueAt | date: 'd MMM y, HH:mm' }}</dd>
              @if (t.closedAt) { <dt>Closed on</dt><dd>{{ t.closedAt | date: 'd MMM y, HH:mm' }}</dd> }
            </dl>
          </section>

          @if (hasActions()) {
            <section class="card" aria-labelledby="tac">
              <header><h2 id="tac">Actions</h2></header>
              <div class="stack" style="gap:14px">
                @if (acts().moves.length > 0) {
                  <div class="actionbar" role="group" aria-label="Change status">
                    @for (m of acts().moves; track m) {
                      <button class="btn" [class.primary]="m === 'RESOLVED'" type="button" (click)="move(m)" [disabled]="busy()">{{ label(t.status, m) }}</button>
                    }
                  </div>
                }
                @if (acts().clientReopen || acts().clientClose) {
                  <div class="actionbar">
                    @if (acts().clientReopen) { <button class="btn primary" type="button" (click)="reopen()" [disabled]="busy()">Reopen: not fixed</button> }
                    @if (acts().clientClose) { <button class="btn" type="button" (click)="close()" [disabled]="busy()">Close the ticket</button> }
                  </div>
                  @if (t.status === 'RESOLVED' && t.reopenCount >= 1) { <p class="muted small">This ticket was already reopened once, so it cannot be reopened again.</p> }
                }
                @if (acts().assign) {
                  <div class="field">
                    <label for="as">Assign to</label>
                    <div class="pwrow">
                      <select id="as" [ngModel]="assigneeDraft() || t.assigneeId || ''" (ngModelChange)="assigneeDraft.set($event)">
                        <option value="" disabled>Choose a person</option>
                        @for (p of staff(); track p.id) { <option [value]="p.id">{{ p.name }}</option> }
                      </select>
                      <button class="btn" type="button" (click)="assign()" [disabled]="busy() || !assigneeDraft() || assigneeDraft() === t.assigneeId">Assign</button>
                    </div>
                  </div>
                }
                @if (acts().priority) {
                  <div class="field">
                    <label for="pr">Priority</label>
                    <div class="pwrow">
                      <select id="pr" [ngModel]="priorityDraft() || t.priority" (ngModelChange)="priorityDraft.set($event)">
                        @for (p of priorities; track p) { <option [value]="p">{{ prio(p).label }} ({{ prio(p).sla }} h)</option> }
                      </select>
                      <button class="btn" type="button" (click)="setPriority()" [disabled]="busy() || !priorityDraft() || priorityDraft() === t.priority">Change</button>
                    </div>
                    <span class="muted small">Changing the priority recalculates the deadline.</span>
                  </div>
                }
              </div>
            </section>
          }
        </div>
      </div>
    }
  `,
})
export class TicketDetailComponent implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(TicketsService);
  private readonly staffApi = inject(StaffService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);

  readonly priorities = PRIORITIES;
  readonly ticket = signal<Ticket | null>(null);
  readonly staff = signal<Person[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly actionError = signal('');
  readonly fields = signal<Record<string, string>>({});
  readonly busy = signal(false);
  readonly internal = signal(false);
  readonly assigneeDraft = signal('');
  readonly priorityDraft = signal('');
  body = '';

  readonly me = this.session.user;
  readonly role = computed(() => this.me()?.role);
  readonly isClient = computed(() => this.role() === 'CLIENT');
  readonly statusBadge = computed(() => TICKET_STATUS[this.ticket()?.status ?? 'OPEN']);
  readonly acts = computed(() => {
    const t = this.ticket();
    return t
      ? ticketActions(t, this.role(), this.me()?.id)
      : { moves: [], comment: false, internalNote: false, assign: false, priority: false, clientClose: false, clientReopen: false };
  });
  readonly hasActions = computed(() => {
    const a = this.acts();
    return a.moves.length > 0 || a.assign || a.priority || a.clientClose || a.clientReopen;
  });

  ngOnInit(): void {
    this.api.get(this.id()).subscribe({
      next: (t) => {
        this.ticket.set(t);
        this.loading.set(false);
        if (this.acts().assign) this.staffApi.assignable().subscribe({ next: (l) => this.staff.set(l), error: () => undefined });
      },
      error: (e) => {
        this.loading.set(false);
        this.loadError.set(toApiError(e).message);
      },
    });
  }

  prio(p: TicketPriority) {
    return TICKET_PRIORITY[p];
  }

  label(from: TicketStatus, to: TicketStatus): string {
    return moveLabel(from, to);
  }

  roleName(r: string): string {
    return r === 'CLIENT' ? 'Client' : r === 'DEVELOPER' ? 'Developer' : r === 'PROJECT_MANAGER' ? 'Project manager' : 'Administrator';
  }

  /** Runs a ticket action. The server answers with the whole ticket, which replaces the one on screen. */
  private act(call: ReturnType<TicketsService['get']>, message: string, after?: () => void): void {
    this.actionError.set('');
    this.fields.set({});
    this.busy.set(true);
    call.subscribe({
      next: (t) => {
        this.busy.set(false);
        this.ticket.set(t);
        this.toast.show(message);
        after?.();
      },
      error: (e) => {
        const err = toApiError(e);
        this.busy.set(false);
        this.fields.set(err.fields);
        this.actionError.set(err.code === 'VALIDATION_FAILED' ? 'Please correct the fields marked below.' : err.message);
      },
    });
  }

  comment(): void {
    const internal = this.internal() && canWriteInternalNote(this.role());
    this.act(this.api.comment(this.id(), this.body.trim(), internal), internal ? 'Internal note added' : 'Reply sent', () => {
      this.body = '';
      this.internal.set(false);
    });
  }

  move(s: TicketStatus): void {
    this.act(this.api.setStatus(this.id(), s), 'Status updated');
  }

  reopen(): void {
    this.act(this.api.reopen(this.id()), 'Ticket reopened');
  }

  close(): void {
    this.act(this.api.close(this.id()), 'Ticket closed');
  }

  assign(): void {
    this.act(this.api.assign(this.id(), this.assigneeDraft()), 'Ticket assigned', () => this.assigneeDraft.set(''));
  }

  setPriority(): void {
    this.act(this.api.setPriority(this.id(), this.priorityDraft() as TicketPriority), 'Priority changed. The deadline was recalculated.', () => this.priorityDraft.set(''));
  }
}
