import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { AuthResult } from '../../core/models';
import { AuthLayoutComponent } from './auth-layout.component';

type Step = 'password' | 'code' | 'newPassword';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, AuthLayoutComponent],
  template: `
    <app-auth-layout>
      @switch (step()) {
        @case ('password') {
          <form (ngSubmit)="signIn()" aria-labelledby="t">
            <h1 id="t">Sign in</h1>
            <p class="muted">Use the email and password for your account.</p>
            <div class="field">
              <label for="email">Email</label>
              <input id="email" name="email" type="email" autocomplete="username" required [(ngModel)]="email" />
            </div>
            <div class="field">
              <label for="password">Password</label>
              <div class="pwrow">
                <input id="password" name="password" [type]="show() ? 'text' : 'password'" autocomplete="current-password" required [(ngModel)]="password" />
                <button type="button" class="btn" (click)="show.set(!show())" [attr.aria-pressed]="show()">{{ show() ? 'Hide' : 'Show' }}</button>
              </div>
            </div>
            @if (error()) {
              <p class="err" role="alert">{{ error() }}
                @if (needsConfirm()) { <a routerLink="/verify-email">Confirm your email</a> }
              </p>
            }
            <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Signing in…' : 'Sign in' }}</button>
          </form>
          <p class="switch muted">New client? <a routerLink="/register">Create your account</a></p>
        }
        @case ('code') {
          <form (ngSubmit)="sendCode()" aria-labelledby="t">
            <h1 id="t">Check your email</h1>
            <p class="muted">We sent a 6-digit code to your email address. Enter it to finish signing in.</p>
            @if (devCode()) {
              <div class="devhint"><b>Development only.</b> The code is shown here because no email server is set up: <span class="mono">{{ devCode() }}</span></div>
            }
            <div class="field">
              <label for="code">6-digit code</label>
              <input id="code" name="code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" required [(ngModel)]="code" />
            </div>
            @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
            <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Checking…' : 'Continue' }}</button>
            <button class="link" type="button" (click)="backToPassword()">Back to sign in</button>
          </form>
        }
        @case ('newPassword') {
          <form (ngSubmit)="saveNewPassword()" aria-labelledby="t">
            <h1 id="t">Choose a new password</h1>
            <p class="muted">This is your first sign-in (or your password was reset), so you must replace the temporary password.</p>
            <div class="field">
              <label for="np">New password</label>
              <input id="np" name="np" type="password" autocomplete="new-password" required [(ngModel)]="newPassword" />
              <span class="muted small">Use at least 8 characters, with letters and a number.</span>
            </div>
            <div class="field">
              <label for="np2">Repeat new password</label>
              <input id="np2" name="np2" type="password" autocomplete="new-password" required [(ngModel)]="newPassword2" />
            </div>
            @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
            <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save and continue' }}</button>
            <button class="link" type="button" (click)="backToPassword()">Cancel</button>
          </form>
        }
      }
    </app-auth-layout>
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly step = signal<Step>('password');
  readonly busy = signal(false);
  readonly error = signal('');
  readonly needsConfirm = signal(false);
  readonly show = signal(false);
  readonly devCode = signal<string | null>(null);

  email = '';
  password = '';
  code = '';
  newPassword = '';
  newPassword2 = '';
  private challenge = '';

  signIn(): void {
    this.begin();
    this.auth.login(this.email.trim(), this.password).subscribe({
      next: (r) => this.handle(r),
      error: (e) => {
        const err = toApiError(e);
        this.fail(err.message);
        if (err.code === 'EMAIL_NOT_VERIFIED') {
          this.auth.pending.set({ email: this.email.trim() });
          this.needsConfirm.set(true);
        }
      },
    });
  }

  sendCode(): void {
    this.begin();
    this.auth.verifyCode(this.challenge, this.code.trim()).subscribe({
      next: (r) => this.handle(r),
      error: (e) => this.failStep(toApiError(e)),
    });
  }

  saveNewPassword(): void {
    if (this.newPassword !== this.newPassword2) {
      this.error.set('The two passwords are not the same.');
      return;
    }
    this.begin();
    this.auth.changePassword(this.challenge, this.newPassword).subscribe({
      next: (r) => this.handle(r),
      error: (e) => this.failStep(toApiError(e)),
    });
  }

  backToPassword(): void {
    this.step.set('password');
    this.password = '';
    this.code = '';
    this.error.set('');
    this.devCode.set(null);
  }

  private handle(r: AuthResult): void {
    this.busy.set(false);
    switch (r.status) {
      case 'AUTHENTICATED':
        void this.router.navigateByUrl('/dashboard');
        break;
      case 'CODE_REQUIRED':
        this.challenge = r.challenge ?? '';
        this.devCode.set(r.devCode ?? null);
        this.code = '';
        this.step.set('code');
        break;
      case 'PASSWORD_CHANGE_REQUIRED':
        this.challenge = r.challenge ?? '';
        this.step.set('newPassword');
        break;
      case 'VERIFICATION_REQUIRED':
        void this.router.navigateByUrl('/verify-email');
        break;
    }
  }

  private begin(): void {
    this.busy.set(true);
    this.error.set('');
    this.needsConfirm.set(false);
  }

  private fail(message: string): void {
    this.busy.set(false);
    this.error.set(message);
  }

  /** An expired step sends the person back to the start; other errors stay on the step. */
  private failStep(err: { code: string; message: string }): void {
    this.fail(err.message);
    if (err.code === 'CHALLENGE_INVALID') {
      this.step.set('password');
    }
  }
}
