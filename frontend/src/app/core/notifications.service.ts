import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { AppNotification, NotificationList } from './models';

/** In-app notifications. The shell refreshes them on a timer so the bell badge stays current. */
@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly base = `${environment.apiUrl}/notifications`;

  readonly items = signal<AppNotification[]>([]);
  readonly unread = signal(0);
  readonly loaded = signal(false);
  readonly failed = signal(false);
  private timer: ReturnType<typeof setInterval> | null = null;

  refresh(): void {
    this.http.get<NotificationList>(this.base).subscribe({
      next: (list) => {
        this.items.set(list.items);
        this.unread.set(list.unread);
        this.loaded.set(true);
        this.failed.set(false);
      },
      error: () => {
        this.loaded.set(true);
        this.failed.set(true);
      },
    });
  }

  startPolling(everyMs = 30000): void {
    this.stopPolling();
    this.refresh();
    this.timer = setInterval(() => this.refresh(), everyMs);
  }

  stopPolling(): void {
    if (this.timer !== null) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  reset(): void {
    this.stopPolling();
    this.items.set([]);
    this.unread.set(0);
    this.loaded.set(false);
    this.failed.set(false);
  }

  /** Marks one notification read (if it was not) and goes to the page it points to. */
  open(n: AppNotification): void {
    if (!n.read) {
      this.items.update((list) => list.map((i) => (i.id === n.id ? { ...i, read: true } : i)));
      this.unread.update((c) => Math.max(0, c - 1));
      this.http.post<void>(`${this.base}/${n.id}/read`, {}).subscribe({ error: () => this.refresh() });
    }
    if (n.link) {
      void this.router.navigateByUrl(n.link);
    }
  }

  markAllRead(): void {
    this.items.update((list) => list.map((i) => ({ ...i, read: true })));
    this.unread.set(0);
    this.http.post<void>(`${this.base}/read-all`, {}).subscribe({ error: () => this.refresh() });
  }
}
