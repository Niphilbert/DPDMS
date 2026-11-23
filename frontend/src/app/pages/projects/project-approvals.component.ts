import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toApiError } from '../../core/api-error';
import { APPROVAL_STATUS, APPROVAL_TYPE, isUrl } from '../../core/format';
import { Approval, ApprovalType, Project } from '../../core/models';
import { ProjectsService } from '../../core/projects.service';
import { SessionService } from '../../core/session.service';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-project-approvals',
  imports: [FormsModule, DatePipe],
  template: `
    <section class="card" aria-labelledby="ah">
      <header>
        <h2 id="ah">Approvals</h2>
        @if (isManagement()) { <button class="btn primary" type="button" (click)="openSend()">Send for approval</button> }
      </header>
      <p class="muted small" style="margin-bottom:12px">{{ isClient() ? 'Designs, page content and documents we need your answer on.' : 'Items the client must approve or ask to change. Send a new version after changes are asked.' }}</p>
      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (items().length === 0) {
        <p class="muted empty">Nothing has been sent for approval yet.</p>
      } @else {
        <div class="list">
          @for (a of items(); track a.id) {
            <div class="item" style="align-items:flex-start">
              <div class="grow">
                <div class="l1">{{ a.item }} <span class="muted small">· version {{ a.version }} · {{ typeName(a.type) }}</span></div>
                <div class="small">
                  @if (isLink(a.file)) { <a [href]="a.file" target="_blank" rel="noopener noreferrer">{{ a.file }}</a> } @else { {{ a.file }} }
                </div>
                @if (a.message) { <div class="note" style="margin-top:4px">{{ a.message }}</div> }
                <div class="muted small" style="margin-top:4px">Sent by {{ a.sentByName }} on {{ a.sentAt | date: 'd MMM y, HH:mm' }}@if (a.answeredAt) { · answered by {{ a.answeredByName }} on {{ a.answeredAt | date: 'd MMM y, HH:mm' }} }</div>
                @if (a.answerNote) { <div class="note" style="margin-top:4px"><b>Answer:</b> {{ a.answerNote }}</div> }

                @if (isClient() && a.status === 'PENDING') {
                  @if (answering()?.id === a.id) {
                    <div class="box" style="margin-top:8px">
                      <div class="field">
                        <label [for]="'note' + a.id">{{ answering()!.mode === 'changes' ? 'What should change?' : 'Comment (optional)' }}</label>
                        <textarea [id]="'note' + a.id" [ngModel]="note()" (ngModelChange)="note.set($event)"></textarea>
                        @if (fieldError()) { <span class="fielderr">{{ fieldError() }}</span> }
                      </div>
                      <div class="tools">
                        <button class="btn primary" type="button" (click)="answer(a)" [disabled]="busy()">{{ answering()!.mode === 'changes' ? 'Ask for changes' : 'Approve' }}</button>
                        <button class="btn" type="button" (click)="answering.set(null)">Cancel</button>
                      </div>
                    </div>
                  } @else {
                    <div class="tools" style="margin-top:8px">
                      <button class="btn sm primary" type="button" (click)="startAnswer(a, 'approve')">Approve</button>
                      <button class="btn sm" type="button" (click)="startAnswer(a, 'changes')">Request changes</button>
                    </div>
                  }
                }
              </div>
              <span class="pill" [class]="badge(a).cls">{{ badge(a).label }}</span>
            </div>
          }
        </div>
      }
    </section>

    @if (sending()) {
      <div class="overlay" (click)="sending.set(false)"></div>
      <form class="modal" role="dialog" aria-modal="true" aria-labelledby="sa" (ngSubmit)="send()" novalidate>
        <h2 id="sa">Send for approval</h2>
        <p class="muted small">Using the same name again creates a new version of that item.</p>
        <div class="formgrid">
          <div class="field wide"><label for="si">Item name</label><input id="si" name="si" placeholder="Home page design" [class.invalid]="fields()['item']" [(ngModel)]="fItem" />@if (fields()['item']) { <span class="fielderr">{{ fields()['item'] }}</span> }</div>
          <div class="field"><label for="st">Type</label>
            <select id="st" name="st" [(ngModel)]="fType"><option value="DESIGN">Design</option><option value="PAGE_CONTENT">Page content</option><option value="DOCUMENT">Document</option></select></div>
          <div class="field"><label for="sk">Finishes task (optional)</label>
            <select id="sk" name="sk" [(ngModel)]="fTask"><option value="">No task</option>@for (t of project().tasks ?? []; track t.id) { <option [value]="t.id">{{ t.title }}</option> }</select></div>
          <div class="field wide"><label for="sf">Link or file name</label><input id="sf" name="sf" placeholder="https://…" [class.invalid]="fields()['file']" [(ngModel)]="fFile" />@if (fields()['file']) { <span class="fielderr">{{ fields()['file'] }}</span> }</div>
          <div class="field wide"><label for="sm">Message to the client (optional)</label><textarea id="sm" name="sm" maxlength="1000" [(ngModel)]="fMessage"></textarea></div>
        </div>
        @if (formError()) { <p class="err" role="alert">{{ formError() }}</p> }
        <div class="tools">
          <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Sending…' : 'Send to client' }}</button>
          <button class="btn" type="button" (click)="sending.set(false)">Cancel</button>
        </div>
      </form>
    }
  `,
})
export class ProjectApprovalsComponent implements OnInit {
  readonly project = input.required<Project>();
  readonly changed = output<void>();

  private readonly api = inject(ProjectsService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);

  readonly items = signal<Approval[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly busy = signal(false);
  readonly isClient = computed(() => this.session.user()?.role === 'CLIENT');
  readonly isManagement = computed(() => this.session.user()?.role !== 'CLIENT');

  readonly answering = signal<{ id: string; mode: 'approve' | 'changes' } | null>(null);
  readonly note = signal('');
  readonly fieldError = signal('');

  readonly sending = signal(false);
  readonly fields = signal<Record<string, string>>({});
  readonly formError = signal('');
  fItem = '';
  fType: ApprovalType = 'DESIGN';
  fFile = '';
  fMessage = '';
  fTask = '';

  ngOnInit(): void {
    this.load();
  }

  badge(a: Approval) {
    return APPROVAL_STATUS[a.status];
  }

  typeName(t: ApprovalType): string {
    return APPROVAL_TYPE[t];
  }

  isLink(text: string): boolean {
    return isUrl(text);
  }

  load(): void {
    this.api.approvals(this.project().id).subscribe({
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

  // ---- client
  startAnswer(a: Approval, mode: 'approve' | 'changes'): void {
    this.note.set('');
    this.fieldError.set('');
    this.error.set('');
    this.answering.set({ id: a.id, mode });
  }

  answer(a: Approval): void {
    const mode = this.answering()?.mode;
    if (mode === 'changes' && !this.note().trim()) {
      this.fieldError.set('Tell us what should change.');
      return;
    }
    this.busy.set(true);
    const call = mode === 'changes' ? this.api.requestApprovalChanges(this.project().id, a.id, this.note().trim()) : this.api.approve(this.project().id, a.id, this.note());
    call.subscribe({
      next: () => {
        this.busy.set(false);
        this.answering.set(null);
        this.toast.show(mode === 'changes' ? 'Your request for changes was sent' : 'Approved. Thank you.');
        this.load();
        this.changed.emit();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(toApiError(e).message);
        this.answering.set(null);
        this.load();
      },
    });
  }

  // ---- management
  openSend(): void {
    this.fItem = '';
    this.fType = 'DESIGN';
    this.fFile = '';
    this.fMessage = '';
    this.fTask = '';
    this.fields.set({});
    this.formError.set('');
    this.sending.set(true);
  }

  send(): void {
    this.fields.set({});
    this.formError.set('');
    this.busy.set(true);
    this.api
      .sendApproval(this.project().id, {
        item: this.fItem.trim(), type: this.fType, file: this.fFile.trim(),
        ...(this.fMessage.trim() ? { message: this.fMessage.trim() } : {}),
        ...(this.fTask ? { taskId: this.fTask } : {}),
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.sending.set(false);
          this.toast.show('Sent to the client for approval');
          this.load();
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
