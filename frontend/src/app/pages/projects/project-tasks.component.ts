import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { toApiError } from '../../core/api-error';
import { PHASES, TASK_COLUMNS, TASK_STATUS, isUrl, todayIso } from '../../core/format';
import { Project, Task, TaskStatus } from '../../core/models';
import { ProjectsService } from '../../core/projects.service';
import { SessionService } from '../../core/session.service';
import { Person, StaffService } from '../../core/staff.service';
import { ToastService } from '../../core/toast.service';

type Dialog = 'add' | 'assign' | 'submit' | 'sendback' | 'time';

/**
 * The task board. Clients get a simple list (title, phase, status, due date). Staff get a board with the actions
 * their role allows: developers move and submit only their own tasks; managers add, assign, review and finish.
 */
@Component({
  selector: 'app-project-tasks',
  imports: [FormsModule, DatePipe],
  template: `
    @if (isClient()) {
      <section class="card" aria-label="Tasks">
        @if (tasks().length === 0) {
          <p class="muted empty">No tasks yet.</p>
        } @else {
          <div class="tblwrap cardtable">
            <table style="min-width:480px">
              <thead><tr><th>Task</th><th>Phase</th><th>Status</th><th>Due</th></tr></thead>
              <tbody>
                @for (t of tasks(); track t.id) {
                  <tr>
                    <td class="l1 first">{{ t.title }}</td>
                    <td data-label="Phase">{{ phase(t.phase) }}</td>
                    <td data-label="Status"><span class="pill" [class]="badge(t.status).cls">{{ badge(t.status).label }}</span></td>
                    <td class="num" data-label="Due">@if (t.dueDate) { {{ t.dueDate | date: 'd MMM y' }} } @if (t.late) { <span class="pill p-bad">Late</span> }</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    } @else {
      <div class="tools">
        @if (isManagement() && project().status !== 'LAUNCHED') {
          <button class="btn primary" type="button" (click)="openAdd()">Add a task</button>
        }
        @if (isDeveloper()) { <span class="muted small">You can move, hand in and log time on your own tasks only.</span> }
      </div>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      <div class="kanban" aria-label="Task board">
        @for (col of columns; track col) {
          <section class="col" [attr.aria-label]="badge(col).label">
            <h3><span>{{ badge(col).label }}</span><span>{{ byStatus(col).length }}</span></h3>
            @for (t of byStatus(col); track t.id) {
              <article class="task" [class.late]="t.late">
                <div class="ttl">{{ t.title }}</div>
                <div class="small muted">{{ phase(t.phase) }}@if (t.dueDate) { · due {{ t.dueDate | date: 'd MMM' }} }@if (t.late) { · <b style="color:var(--bad)">late</b> }</div>
                <div class="small">{{ t.assigneeName ?? 'Not assigned' }}@if (t.plannedHours !== undefined) { · <span class="num">{{ t.loggedHours ?? 0 }}/{{ t.plannedHours }} h</span> }</div>
                @if (t.sentBackNote && t.status !== 'DONE') { <div class="callout warn"><b>Sent back:</b> {{ t.sentBackNote }}</div> }
                @if (t.status === 'REVIEW' || (t.status === 'DONE' && t.submissionFile)) {
                  <div class="small">
                    Submitted by {{ t.submittedByName }}:
                    @if (isLink(t.submissionFile)) { <a [href]="t.submissionFile" target="_blank" rel="noopener noreferrer">{{ t.submissionFile }}</a> } @else { {{ t.submissionFile }} }
                    @if (t.submissionNote) { <div class="note" style="margin-top:4px">{{ t.submissionNote }}</div> }
                  </div>
                }
                <div class="acts">
                  @if (started()) {
                    @if (canMove(t) && t.status === 'TODO') { <button class="btn sm" type="button" (click)="move(t, 'DOING')">Start</button> }
                    @if (canMove(t) && t.status === 'DOING') {
                      <button class="btn sm primary" type="button" (click)="open('submit', t)">Submit work</button>
                      <button class="btn sm" type="button" (click)="move(t, 'TODO')">Back to to do</button>
                    }
                    @if (canMove(t) && t.status === 'REVIEW' && isDeveloper()) { <button class="btn sm" type="button" (click)="move(t, 'DOING')">Take back</button> }
                    @if (isMine(t) && t.status !== 'DONE') { <button class="btn sm" type="button" (click)="open('time', t)">Log time</button> }
                    @if (isManagement() && t.status === 'REVIEW') {
                      <button class="btn sm primary" type="button" (click)="complete(t)">Mark done</button>
                      <button class="btn sm" type="button" (click)="open('sendback', t)">Send back</button>
                    }
                    @if (isManagement() && (t.status === 'TODO' || t.status === 'DOING')) {
                      <label class="small muted">Move to
                        <select [attr.aria-label]="'Move ' + t.title" (change)="moveTo(t, $any($event.target).value); $any($event.target).value = ''">
                          <option value="">…</option>
                          @for (s of moveOptions(t); track s) { <option [value]="s">{{ badge(s).label }}</option> }
                        </select>
                      </label>
                    }
                  }
                  @if (isManagement() && t.status !== 'DONE') {
                    <button class="btn sm" type="button" (click)="open('assign', t)">{{ t.assigneeId ? 'Reassign' : 'Assign' }}</button>
                  }
                </div>
              </article>
            } @empty {
              <p class="muted small">Nothing here.</p>
            }
          </section>
        }
      </div>
    }

    @if (dialog(); as d) {
      <div class="overlay" (click)="close()"></div>
      <form class="modal" role="dialog" aria-modal="true" aria-labelledby="dlg" (ngSubmit)="submitDialog()" novalidate>
        @switch (d) {
          @case ('add') {
            <h2 id="dlg">Add a task</h2>
            <div class="formgrid">
              <div class="field wide"><label for="at">Task name</label><input id="at" name="at" [class.invalid]="fields()['title']" [(ngModel)]="fTitle" />@if (fields()['title']) { <span class="fielderr">{{ fields()['title'] }}</span> }</div>
              <div class="field"><label for="ap">Phase</label>
                <select id="ap" name="ap" [(ngModel)]="fPhase">@for (p of phaseNames; track $index) { <option [ngValue]="$index">{{ p }}</option> }</select></div>
              <div class="field"><label for="ah">Planned hours</label><input id="ah" name="ah" type="number" min="1" max="2000" [class.invalid]="fields()['plannedHours']" [(ngModel)]="fHours" />@if (fields()['plannedHours']) { <span class="fielderr">{{ fields()['plannedHours'] }}</span> }</div>
              <div class="field"><label for="ad">Due date</label><input id="ad" name="ad" type="date" [min]="today" [class.invalid]="fields()['dueDate']" [(ngModel)]="fDue" />@if (fields()['dueDate']) { <span class="fielderr">{{ fields()['dueDate'] }}</span> }</div>
              <div class="field"><label for="aa">Assign to (optional)</label>
                <select id="aa" name="aa" [(ngModel)]="fAssignee"><option value="">Nobody yet</option>@for (p of people(); track p.id) { <option [value]="p.id">{{ p.name }}</option> }</select></div>
            </div>
          }
          @case ('assign') {
            <h2 id="dlg">{{ target()?.assigneeId ? 'Reassign' : 'Assign' }}: {{ target()?.title }}</h2>
            <div class="formgrid">
              <div class="field wide"><label for="as">Give the task to</label>
                <select id="as" name="as" [(ngModel)]="fAssignee"><option value="">Choose a person…</option>@for (p of people(); track p.id) { <option [value]="p.id">{{ p.name }}</option> }</select>
                @if (fields()['assigneeId']) { <span class="fielderr">{{ fields()['assigneeId'] }}</span> }
              </div>
              <div class="field"><label for="adu">Due date</label><input id="adu" name="adu" type="date" [min]="today" [class.invalid]="fields()['dueDate']" [(ngModel)]="fDue" />@if (fields()['dueDate']) { <span class="fielderr">{{ fields()['dueDate'] }}</span> }</div>
              <div class="field wide"><label for="an">Note for them (optional)</label><textarea id="an" name="an" maxlength="500" [(ngModel)]="fNote"></textarea></div>
            </div>
          }
          @case ('submit') {
            <h2 id="dlg">Submit work: {{ target()?.title }}</h2>
            <p class="muted small">Add a link or the name of the file you are handing in. The project manager will review it.</p>
            <div class="formgrid">
              <div class="field wide"><label for="sf">Link or file name</label><input id="sf" name="sf" placeholder="https://…" [class.invalid]="fields()['file']" [(ngModel)]="fFile" />@if (fields()['file']) { <span class="fielderr">{{ fields()['file'] }}</span> }</div>
              <div class="field wide"><label for="sn">Note (optional)</label><textarea id="sn" name="sn" maxlength="1000" [(ngModel)]="fNote"></textarea></div>
            </div>
          }
          @case ('sendback') {
            <h2 id="dlg">Send back: {{ target()?.title }}</h2>
            <div class="field"><label for="bn">What must change?</label><textarea id="bn" name="bn" [class.invalid]="fields()['note']" [(ngModel)]="fNote"></textarea>@if (fields()['note']) { <span class="fielderr">{{ fields()['note'] }}</span> }</div>
          }
          @case ('time') {
            <h2 id="dlg">Log time: {{ target()?.title }}</h2>
            <div class="formgrid">
              <div class="field"><label for="td">Day</label><input id="td" name="td" type="date" [max]="today" [class.invalid]="fields()['date']" [(ngModel)]="fDate" />@if (fields()['date']) { <span class="fielderr">{{ fields()['date'] }}</span> }</div>
              <div class="field"><label for="th">Hours</label><input id="th" name="th" type="number" min="0.25" max="24" step="0.25" [class.invalid]="fields()['hours']" [(ngModel)]="fHoursLogged" />@if (fields()['hours']) { <span class="fielderr">{{ fields()['hours'] }}</span> }</div>
              <div class="field wide"><label for="tn">What did you do? (optional)</label><input id="tn" name="tn" maxlength="500" [(ngModel)]="fNote" /></div>
            </div>
          }
        }
        @if (formError()) { <p class="err" role="alert">{{ formError() }}</p> }
        <div class="tools">
          <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save' }}</button>
          <button class="btn" type="button" (click)="close()">Cancel</button>
        </div>
      </form>
    }
  `,
})
export class ProjectTasksComponent {
  readonly project = input.required<Project>();
  readonly changed = output<void>();

  private readonly api = inject(ProjectsService);
  private readonly session = inject(SessionService);
  private readonly staff = inject(StaffService);
  private readonly toast = inject(ToastService);

  readonly columns = TASK_COLUMNS;
  readonly phaseNames = PHASES;
  readonly today = todayIso();

  readonly role = computed(() => this.session.user()?.role);
  readonly isClient = computed(() => this.role() === 'CLIENT');
  readonly isDeveloper = computed(() => this.role() === 'DEVELOPER');
  readonly isManagement = computed(() => this.role() === 'ADMIN' || this.role() === 'PROJECT_MANAGER');
  readonly started = computed(() => this.project().status !== 'PLANNING');
  readonly tasks = computed(() => this.project().tasks ?? []);

  readonly error = signal('');
  readonly dialog = signal<Dialog | null>(null);
  readonly target = signal<Task | null>(null);
  readonly people = signal<Person[]>([]);
  readonly busy = signal(false);
  readonly fields = signal<Record<string, string>>({});
  readonly formError = signal('');

  fTitle = '';
  fPhase = 2;
  fHours: number | null = 8;
  fDue = '';
  fAssignee = '';
  fNote = '';
  fFile = '';
  fDate = '';
  fHoursLogged: number | null = null;

  badge(s: TaskStatus) {
    return TASK_STATUS[s];
  }

  phase(i: number): string {
    return PHASES[i] ?? '';
  }

  isLink(text: string | undefined): boolean {
    return isUrl(text);
  }

  byStatus(s: TaskStatus): Task[] {
    return this.tasks().filter((t) => t.status === s);
  }

  isMine(t: Task): boolean {
    return !!t.assigneeId && t.assigneeId === this.session.user()?.id;
  }

  /** Developers may move only their own tasks; managers may move any. */
  canMove(t: Task): boolean {
    return this.isManagement() || (this.isDeveloper() && this.isMine(t));
  }

  moveOptions(t: Task): TaskStatus[] {
    return (['TODO', 'DOING', 'DONE'] as TaskStatus[]).filter((s) => s !== t.status);
  }

  // ---- direct moves
  move(t: Task, status: TaskStatus): void {
    this.act(this.api.setStatus(this.project().id, t.id, status), `Moved to ${TASK_STATUS[status].label.toLowerCase()}`);
  }

  moveTo(t: Task, value: string): void {
    if (value) this.move(t, value as TaskStatus);
  }

  complete(t: Task): void {
    this.act(this.api.complete(this.project().id, t.id), 'Task marked done');
  }

  private act(call: Observable<unknown>, done: string): void {
    this.error.set('');
    call.subscribe({
      next: () => {
        this.toast.show(done);
        this.changed.emit();
      },
      error: (e) => this.error.set(toApiError(e).message),
    });
  }

  // ---- dialogs
  openAdd(): void {
    this.fTitle = '';
    this.fPhase = 2;
    this.fHours = 8;
    this.fDue = todayIso(14);
    this.fAssignee = '';
    this.target.set(null);
    this.loadPeople();
    this.show('add');
  }

  open(kind: Exclude<Dialog, 'add'>, t: Task): void {
    this.target.set(t);
    this.fNote = '';
    this.fFile = '';
    this.fAssignee = t.assigneeId ?? '';
    this.fDue = t.dueDate && t.dueDate >= this.today ? t.dueDate : todayIso(7);
    this.fDate = this.today;
    this.fHoursLogged = null;
    if (kind === 'assign') this.loadPeople();
    this.show(kind);
  }

  close(): void {
    this.dialog.set(null);
  }

  private show(kind: Dialog): void {
    this.fields.set({});
    this.formError.set('');
    this.dialog.set(kind);
  }

  private loadPeople(): void {
    this.people.set([]);
    this.staff.assignable().subscribe({
      next: (list) => this.people.set(list),
      error: (e) => this.formError.set(toApiError(e).message),
    });
  }

  submitDialog(): void {
    const kind = this.dialog();
    const t = this.target();
    const pid = this.project().id;
    this.fields.set({});
    this.formError.set('');
    let call: Observable<unknown>;
    let done = 'Saved';
    switch (kind) {
      case 'add':
        call = this.api.addTask(pid, {
          title: this.fTitle.trim(), phase: Number(this.fPhase), plannedHours: Number(this.fHours), dueDate: this.fDue,
          ...(this.fAssignee ? { assigneeId: this.fAssignee } : {}),
        });
        done = 'Task added';
        break;
      case 'assign':
        if (!this.fAssignee) {
          this.fields.set({ assigneeId: 'Choose a person.' });
          return;
        }
        call = this.api.assign(pid, t!.id, { assigneeId: this.fAssignee, dueDate: this.fDue, ...(this.fNote.trim() ? { note: this.fNote.trim() } : {}) });
        done = 'Task assigned';
        break;
      case 'submit':
        call = this.api.submit(pid, t!.id, { file: this.fFile.trim(), ...(this.fNote.trim() ? { note: this.fNote.trim() } : {}) });
        done = 'Work submitted for review';
        break;
      case 'sendback':
        call = this.api.sendBack(pid, t!.id, this.fNote.trim());
        done = 'Task sent back with your comments';
        break;
      case 'time':
        call = this.api.logTime(pid, t!.id, { date: this.fDate, hours: Number(this.fHoursLogged), ...(this.fNote.trim() ? { note: this.fNote.trim() } : {}) });
        done = 'Time logged';
        break;
      default:
        return;
    }
    this.busy.set(true);
    call.subscribe({
      next: () => {
        this.busy.set(false);
        this.dialog.set(null);
        this.toast.show(done);
        this.changed.emit();
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
