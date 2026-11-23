import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { AuthLayoutComponent } from './auth-layout.component';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink, AuthLayoutComponent],
  template: `
    <app-auth-layout>
      <form (ngSubmit)="submit()" aria-labelledby="t" novalidate>
        <h1 id="t">Create your client account</h1>
        <p class="muted">For companies ordering work from ITZETech. Staff accounts are created by an administrator.</p>

        <div class="field">
          <label for="company">Company or organisation</label>
          <input id="company" name="company" autocomplete="organization" [class.invalid]="fields()['companyName']" [(ngModel)]="companyName" />
          @if (fields()['companyName']) { <span class="fielderr">{{ fields()['companyName'] }}</span> }
        </div>
        <div class="field">
          <label for="name">Your full name</label>
          <input id="name" name="name" autocomplete="name" [class.invalid]="fields()['fullName']" [(ngModel)]="fullName" />
          @if (fields()['fullName']) { <span class="fielderr">{{ fields()['fullName'] }}</span> }
        </div>
        <div class="field">
          <label for="email">Email</label>
          <input id="email" name="email" type="email" autocomplete="email" [class.invalid]="fields()['email']" [(ngModel)]="email" />
          @if (fields()['email']) { <span class="fielderr">{{ fields()['email'] }}</span> }
        </div>
        <div class="field">
          <label for="phone">Phone (optional)</label>
          <input id="phone" name="phone" type="tel" autocomplete="tel" [class.invalid]="fields()['phone']" [(ngModel)]="phone" />
          @if (fields()['phone']) { <span class="fielderr">{{ fields()['phone'] }}</span> }
        </div>
        <div class="field">
          <label for="pw">Password</label>
          <input id="pw" name="pw" type="password" autocomplete="new-password" [class.invalid]="fields()['password']" [(ngModel)]="password" />
          <span class="muted small">Use at least 8 characters, with letters and a number.</span>
          @if (fields()['password']) { <span class="fielderr">{{ fields()['password'] }}</span> }
        </div>
        <div class="field">
          <label for="pw2">Repeat password</label>
          <input id="pw2" name="pw2" type="password" autocomplete="new-password" [class.invalid]="fields()['password2']" [(ngModel)]="password2" />
          @if (fields()['password2']) { <span class="fielderr">{{ fields()['password2'] }}</span> }
        </div>
        <div class="field">
          <label class="sw"><input type="checkbox" name="terms" [(ngModel)]="acceptTerms" /> I accept the terms of use</label>
          @if (fields()['acceptTerms']) { <span class="fielderr">{{ fields()['acceptTerms'] }}</span> }
        </div>

        @if (error()) { <p class="err" role="alert">{{ error() }}</p> }
        <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Creating account…' : 'Create account' }}</button>
      </form>
      <p class="switch muted">Already have an account? <a routerLink="/login">Sign in</a></p>
    </app-auth-layout>
  `,
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly busy = signal(false);
  readonly error = signal('');
  readonly fields = signal<Record<string, string>>({});

  companyName = '';
  fullName = '';
  email = '';
  phone = '';
  password = '';
  password2 = '';
  acceptTerms = false;

  submit(): void {
    this.error.set('');
    if (this.password !== this.password2) {
      this.fields.set({ password2: 'The two passwords are not the same.' });
      return;
    }
    this.fields.set({});
    this.busy.set(true);
    this.auth
      .register({
        companyName: this.companyName.trim(),
        fullName: this.fullName.trim(),
        email: this.email.trim(),
        phone: this.phone.trim(),
        password: this.password,
        acceptTerms: this.acceptTerms,
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          void this.router.navigateByUrl('/verify-email');
        },
        error: (e) => {
          const err = toApiError(e);
          this.busy.set(false);
          this.fields.set(err.fields);
          this.error.set(err.code === 'VALIDATION_FAILED' ? 'Please correct the fields marked below.' : err.message);
        },
      });
  }
}
