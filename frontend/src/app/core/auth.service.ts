import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResult, RegisterRequest, User } from './models';
import { SessionService } from './session.service';

/** A client who registered (or tried to sign in) and still has to confirm their e-mail address. */
export interface PendingVerification {
  email: string;
  devCode?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly base = `${environment.apiUrl}/auth`;

  /** Carries the e-mail (and the development code) from the register / sign-in screen to the confirm screen. */
  readonly pending = signal<PendingVerification | null>(null);

  login(email: string, password: string): Observable<AuthResult> {
    return this.http.post<AuthResult>(`${this.base}/login`, { email, password }).pipe(tap((r) => this.accept(r)));
  }

  verifyCode(challenge: string, code: string): Observable<AuthResult> {
    return this.http.post<AuthResult>(`${this.base}/verify-code`, { challenge, code }).pipe(tap((r) => this.accept(r)));
  }

  changePassword(challenge: string, newPassword: string): Observable<AuthResult> {
    return this.http
      .post<AuthResult>(`${this.base}/change-password`, { challenge, newPassword })
      .pipe(tap((r) => this.accept(r)));
  }

  register(body: RegisterRequest): Observable<AuthResult> {
    return this.http.post<AuthResult>(`${this.base}/register`, body).pipe(tap((r) => this.accept(r)));
  }

  verifyEmail(email: string, code: string): Observable<AuthResult> {
    return this.http.post<AuthResult>(`${this.base}/verify-email`, { email, code }).pipe(tap((r) => this.accept(r)));
  }

  /** Always answers 202. In development the body is `{ devCode }` when a code was really sent; otherwise it is empty. */
  resendCode(email: string): Observable<{ devCode?: string } | null> {
    return this.http.post<{ devCode?: string } | null>(`${this.base}/resend-code`, { email });
  }

  /** Reloads the signed-in person (used after a page refresh). */
  me(): Observable<User> {
    return this.http.get<User>(`${this.base}/me`).pipe(tap((u) => this.session.updateUser(u)));
  }

  logout(): void {
    this.session.clear();
    this.pending.set(null);
    void this.router.navigateByUrl('/login');
  }

  /** Keeps the session when the answer is a finished sign-in, and remembers the e-mail when confirmation is needed. */
  private accept(result: AuthResult): void {
    if (result.status === 'AUTHENTICATED' && result.token && result.user) {
      this.session.start(result.token, result.user);
      this.pending.set(null);
    } else if (result.status === 'VERIFICATION_REQUIRED' && result.email) {
      this.pending.set({ email: result.email, devCode: result.devCode });
    }
  }
}
