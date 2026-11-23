import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { AuthLayoutComponent } from './auth-layout.component';

@Component({
  selector: 'app-verify-email',
  imports: [FormsModule, RouterLink, AuthLayoutComponent],
  template: `
    <app-auth-layout>
      <form (ngSubmit)="submit()" aria-labelledby="t">
        <h1 id="t">Confirm your email</h1>
        <p class="muted">We sent a 6-digit code to confirm your email address. Enter it below.</p>
        @if (devCode()) {
          <div class="devhint"><b>Development only.</b> The code is shown here because no email server is set up: <span class="mono">{{ devCode() }}</span></div>
        }
        <div class="field">
          <label for="email">Email</label>
          <input id="email" name="email" type="email" autocomplete="username" required [(ngModel)]="email" />
        </div>
        <div class="field">
          <label for="code">6-digit code</label>
          <input id="code" name="code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" required [(ngModel)]="code" />
        </div>
        @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
        @if (info()) { <p class="small" role="status">{{ info() }}</p> }
        <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Checking…' : 'Confirm and continue' }}</button>
        <button class="link" type="button" (click)="resend()" [disabled]="busy()">Send a new code</button>
      </form>
      <p class="switch muted"><a routerLink="/login">Back to sign in</a></p>
    </app-auth-layout>
  `,
})
export class VerifyEmailComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly busy = signal(false);
  readonly error = signal('');
  readonly info = signal('');
  readonly devCode = signal<string | null>(this.auth.pending()?.devCode ?? null);

  email = this.auth.pending()?.email ?? '';
  code = '';

  submit(): void {
    this.error.set('');
    this.info.set('');
    this.busy.set(true);
    this.auth.verifyEmail(this.email.trim(), this.code.trim()).subscribe({
      next: () => {
        this.busy.set(false);
        void this.router.navigateByUrl('/dashboard');
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(toApiError(e).message);
      },
    });
  }

  resend(): void {
    this.error.set('');
    this.info.set('');
    if (!this.email.trim()) {
      this.error.set('Enter your email first.');
      return;
    }
    this.busy.set(true);
    this.auth.resendCode(this.email.trim()).subscribe({
      next: (body) => {
        this.busy.set(false);
        this.devCode.set(body?.devCode ?? null);
        this.info.set('A new code is on its way. Check your email.');
      },
      error: (e) => {
        const err = toApiError(e);
        this.busy.set(false);
        this.error.set(err.message); // CODE_TOO_SOON (429) gets its friendly text from toApiError
      },
    });
  }
}
