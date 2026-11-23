import { Observable } from 'rxjs';
import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toApiError } from '../../core/api-error';
import { AdminService } from '../../core/admin.service';
import { Role, StaffRole, User } from '../../core/models';
import { SessionService } from '../../core/session.service';
import { ToastService } from '../../core/toast.service';

const STATUS: Record<string, { label: string; cls: string }> = {
  ACTIVE: { label: 'Active', cls: 'p-ok' },
  INVITED: { label: 'Invited', cls: 'p-warn' },
  DEACTIVATED: { label: 'Deactivated', cls: 'p-bad' },
};

@Component({
  selector: 'app-admin-users',
  imports: [FormsModule, DatePipe],
  template: `
    <div class="top">
      <div>
        <h1>Users and access</h1>
        <p class="sub">Create accounts, choose roles and give access. Only administrators can do this.</p>
      </div>
      <div class="tools"><button class="btn primary" type="button" (click)="openAdd()">Add staff member</button></div>
    </div>

    <div class="callout info">
      <b>Staff are added here; clients register themselves.</b> A new staff member gets a temporary password and must choose their own at the first sign-in.
      Clients create their own account on the register page and appear in this list automatically.
    </div>

    <section class="card">
      <div class="filters">
        <div class="field" style="flex:1; min-width:200px">
          <label for="q">Search</label>
          <input id="q" type="search" placeholder="Name, email or company" [ngModel]="q()" (ngModelChange)="onSearch($event)" />
        </div>
        <div class="field">
          <label for="role">Role</label>
          <select id="role" [ngModel]="role()" (ngModelChange)="onRole($event)">
            <option value="">All roles</option>
            <option value="ADMIN">Administrator</option>
            <option value="PROJECT_MANAGER">Project manager</option>
            <option value="DEVELOPER">Developer</option>
            <option value="CLIENT">Client</option>
          </select>
        </div>
      </div>

      @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
      @if (loading()) {
        <p class="muted loading">Loading…</p>
      } @else if (users().length === 0 && !error()) {
        <p class="muted empty">No users match.</p>
      } @else {
        <div class="tblwrap cardtable">
          <table>
            <thead><tr><th>Person</th><th>Role</th><th>Status</th><th>Last sign-in</th></tr></thead>
            <tbody>
              @for (u of users(); track u.id) {
                <tr class="row" tabindex="0" (click)="select(u)" (keydown.enter)="select(u)">
                  <td class="first">
                    <div class="l1">{{ u.fullName }}</div>
                    <div class="l2">{{ u.email }}@if (u.companyName) { · {{ u.companyName }} }</div>
                  </td>
                  <td data-label="Role">{{ u.roleLabel }}</td>
                  <td data-label="Status"><span class="pill" [class]="statusOf(u).cls">{{ statusOf(u).label }}</span></td>
                  <td data-label="Last sign-in">@if (u.lastLoginAt) { {{ u.lastLoginAt | date: 'd MMM y, HH:mm' }} } @else { <span class="muted">Never</span> }</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    @if (adding()) {
      <div class="overlay" (click)="closeAdd()"></div>
      <form class="modal" role="dialog" aria-modal="true" aria-labelledby="addt" (ngSubmit)="create()" novalidate>
        <h2 id="addt">Add a staff member</h2>
        @if (created(); as c) {
          <div class="callout ok">
            <b>{{ c.user.fullName }}</b> was added as {{ c.user.roleLabel }}.
            @if (c.temporaryPassword) {
              <div class="devhint" style="margin-top:8px"><b>Development only — temporary password:</b> <span class="mono">{{ c.temporaryPassword }}</span><br /><span class="small">Give it to them once. They must choose a new password when they sign in.</span></div>
            } @else {
              Their temporary password was sent to {{ c.user.email }}.
            }
          </div>
          <div><button class="btn primary" type="button" (click)="closeAdd()">Done</button></div>
        } @else {
          <div class="formgrid">
            <div class="field wide">
              <label for="fn">Full name</label>
              <input id="fn" name="fn" [class.invalid]="fields()['fullName']" [(ngModel)]="newName" />
              @if (fields()['fullName']) { <span class="fielderr">{{ fields()['fullName'] }}</span> }
            </div>
            <div class="field wide">
              <label for="em">Email</label>
              <input id="em" name="em" type="email" [class.invalid]="fields()['email']" [(ngModel)]="newEmail" />
              @if (fields()['email']) { <span class="fielderr">{{ fields()['email'] }}</span> }
            </div>
            <div class="field wide">
              <label for="rl">Role</label>
              <select id="rl" name="rl" [(ngModel)]="newRole">
                <option value="DEVELOPER">Developer</option>
                <option value="PROJECT_MANAGER">Project manager</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </div>
          </div>
          @if (formError()) { <p class="err" role="alert">{{ formError() }}</p> }
          <div class="tools">
            <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Adding…' : 'Add staff member' }}</button>
            <button class="btn" type="button" (click)="closeAdd()">Cancel</button>
          </div>
        }
      </form>
    }

    @if (selected(); as u) {
      <div class="overlay" (click)="close()"></div>
      <aside class="drawer" role="dialog" aria-modal="true" aria-labelledby="dt">
        <header>
          <div>
            <h2 id="dt">{{ u.fullName }}</h2>
            <p class="muted">{{ u.email }}</p>
          </div>
          <button class="btn sm" type="button" (click)="close()">Close</button>
        </header>
        <div class="tools">
          <span class="pill" [class]="statusOf(u).cls">{{ statusOf(u).label }}</span>
          @if (u.selfRegistered) { <span class="pill p-info">Registered themselves</span> }
          @if (isMe(u)) { <span class="pill p-neutral">This is you</span> }
        </div>
        <dl>
          <dt>Role</dt><dd>{{ u.roleLabel }}</dd>
          @if (u.companyName) { <dt>Company</dt><dd>{{ u.companyName }}</dd> }
          <dt>Created</dt><dd>{{ u.createdAt | date: 'd MMM y, HH:mm' }}</dd>
          <dt>Last sign-in</dt><dd>@if (u.lastLoginAt) { {{ u.lastLoginAt | date: 'd MMM y, HH:mm' }} } @else { Never }</dd>
        </dl>

        @if (drawerError()) { <p class="err" role="alert">{{ drawerError() }}</p> }
        @if (tempPassword()) {
          <div class="devhint"><b>Development only — temporary password:</b> <span class="mono">{{ tempPassword() }}</span><br /><span class="small">They must choose a new password at their next sign-in.</span></div>
        } @else if (resetDone()) {
          <div class="callout ok">A temporary password was sent to {{ u.email }}.</div>
        }

        @if (u.role !== 'CLIENT') {
          <div class="box">
            <h3>Role</h3>
            @if (isMe(u)) {
              <p class="muted small">You cannot change your own role.</p>
            } @else {
              <div class="field">
                <label for="nr">Change role</label>
                <select id="nr" [ngModel]="roleDraft()" (ngModelChange)="roleDraft.set($event)">
                  <option value="DEVELOPER">Developer</option>
                  <option value="PROJECT_MANAGER">Project manager</option>
                  <option value="ADMIN">Administrator</option>
                </select>
              </div>
              <div><button class="btn" type="button" (click)="saveRole(u)" [disabled]="busy() || roleDraft() === u.role">Save role</button></div>
            }
          </div>
        } @else {
          <p class="muted small">Client roles cannot be changed. Clients only see their own projects, invoices and websites.</p>
        }

        <div class="box">
          <h3>Access</h3>
          @if (isMe(u)) {
            <p class="muted small">You cannot deactivate your own account.</p>
          } @else {
            <div class="tools">
              @if (u.status === 'DEACTIVATED') {
                <button class="btn" type="button" (click)="reactivate(u)" [disabled]="busy()">Reactivate account</button>
              } @else {
                <button class="btn danger" type="button" (click)="deactivate(u)" [disabled]="busy()">Deactivate account</button>
              }
              <button class="btn" type="button" (click)="resetPassword(u)" [disabled]="busy()">Reset password</button>
            </div>
            <p class="muted small">Deactivating stops the person at once, even if they are signed in. Resetting gives them a temporary password they must replace.</p>
          }
        </div>
      </aside>
    }
  `,
})
export class AdminUsersComponent implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);

  readonly users = signal<User[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly q = signal('');
  readonly role = signal<Role | ''>('');

  readonly selected = signal<User | null>(null);
  readonly roleDraft = signal<StaffRole>('DEVELOPER');
  readonly drawerError = signal('');
  readonly tempPassword = signal('');
  readonly resetDone = signal(false);
  readonly busy = signal(false);

  readonly adding = signal(false);
  readonly created = signal<{ user: User; temporaryPassword?: string } | null>(null);
  readonly fields = signal<Record<string, string>>({});
  readonly formError = signal('');
  newName = '';
  newEmail = '';
  newRole: StaffRole = 'DEVELOPER';

  readonly myId = computed(() => this.session.user()?.id);
  private timer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.load();
  }

  statusOf(u: User): { label: string; cls: string } {
    return STATUS[u.status] ?? { label: u.status, cls: 'p-neutral' };
  }

  isMe(u: User): boolean {
    return u.id === this.myId();
  }

  onSearch(value: string): void {
    this.q.set(value);
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.load(), 250);
  }

  onRole(value: Role | ''): void {
    this.role.set(value);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.admin.listUsers(this.q(), this.role()).subscribe({
      next: (list) => {
        this.users.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(toApiError(e).message);
      },
    });
  }

  // ---- add staff
  openAdd(): void {
    this.newName = '';
    this.newEmail = '';
    this.newRole = 'DEVELOPER';
    this.fields.set({});
    this.formError.set('');
    this.created.set(null);
    this.adding.set(true);
  }

  closeAdd(): void {
    this.adding.set(false);
  }

  create(): void {
    this.fields.set({});
    this.formError.set('');
    this.busy.set(true);
    this.admin.createStaff({ fullName: this.newName.trim(), email: this.newEmail.trim(), role: this.newRole }).subscribe({
      next: (c) => {
        this.busy.set(false);
        this.created.set(c);
        this.load();
      },
      error: (e) => {
        const err = toApiError(e);
        this.busy.set(false);
        this.fields.set(err.fields);
        this.formError.set(err.code === 'VALIDATION_FAILED' ? 'Please correct the fields marked below.' : err.message);
      },
    });
  }

  // ---- drawer
  select(u: User): void {
    this.drawerError.set('');
    this.tempPassword.set('');
    this.resetDone.set(false);
    this.roleDraft.set(u.role === 'CLIENT' ? 'DEVELOPER' : (u.role as StaffRole));
    this.selected.set(u);
  }

  close(): void {
    this.selected.set(null);
  }

  saveRole(u: User): void {
    this.act(this.admin.changeRole(u.id, this.roleDraft()), 'Role changed');
  }

  deactivate(u: User): void {
    this.act(this.admin.deactivate(u.id), 'Account deactivated');
  }

  reactivate(u: User): void {
    this.act(this.admin.reactivate(u.id), 'Account reactivated');
  }

  resetPassword(u: User): void {
    this.drawerError.set('');
    this.tempPassword.set('');
    this.resetDone.set(false);
    this.busy.set(true);
    this.admin.resetPassword(u.id).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.replace(r.user);
        this.tempPassword.set(r.temporaryPassword ?? '');
        this.resetDone.set(!r.temporaryPassword);
        this.toast.show('Password reset');
      },
      error: (e) => {
        this.busy.set(false);
        this.drawerError.set(toApiError(e).message);
      },
    });
  }

  private act(call: Observable<User>, doneText: string): void {
    this.drawerError.set('');
    this.busy.set(true);
    call.subscribe({
      next: (user) => {
        this.busy.set(false);
        this.replace(user);
        this.toast.show(doneText);
      },
      error: (e) => {
        this.busy.set(false);
        this.drawerError.set(toApiError(e).message);
      },
    });
  }

  private replace(user: User): void {
    this.users.update((list) => list.map((x) => (x.id === user.id ? user : x)));
    this.selected.set(user);
    this.roleDraft.set(user.role === 'CLIENT' ? 'DEVELOPER' : (user.role as StaffRole));
  }
}
