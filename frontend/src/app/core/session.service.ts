import { Injectable, computed, signal } from '@angular/core';
import { Role, User } from './models';

const KEY = 'dpdms.session';

/**
 * Who is signed in. The token lives in memory and is mirrored to sessionStorage so a page refresh keeps
 * the person signed in; closing the tab ends the session.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly _token = signal<string | null>(null);
  private readonly _user = signal<User | null>(null);

  readonly token = this._token.asReadonly();
  readonly user = this._user.asReadonly();
  readonly signedIn = computed(() => this._token() !== null);

  constructor() {
    try {
      const raw = sessionStorage.getItem(KEY);
      if (raw) {
        const saved = JSON.parse(raw) as { token: string; user: User | null };
        if (saved.token) {
          this._token.set(saved.token);
          this._user.set(saved.user ?? null);
        }
      }
    } catch {
      /* storage blocked or corrupted: start signed out */
    }
  }

  start(token: string, user: User): void {
    this._token.set(token);
    this._user.set(user);
    this.persist();
  }

  updateUser(user: User): void {
    this._user.set(user);
    this.persist();
  }

  clear(): void {
    this._token.set(null);
    this._user.set(null);
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      /* ignore */
    }
  }

  hasRole(...roles: Role[]): boolean {
    const u = this._user();
    return u !== null && roles.includes(u.role);
  }

  private persist(): void {
    try {
      sessionStorage.setItem(KEY, JSON.stringify({ token: this._token(), user: this._user() }));
    } catch {
      /* ignore */
    }
  }
}
