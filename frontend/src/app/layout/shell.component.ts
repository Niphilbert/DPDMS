import { DatePipe } from '@angular/common';
import { Component, HostListener, OnDestroy, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { AppNotification } from '../core/models';
import { NAV, ROLE_LABEL, initials } from '../core/nav';
import { NAV_GROUP, NAV_GROUP_ORDER, ICON, navIcon } from '../core/nav-icons';
import { NotificationsService } from '../core/notifications.service';
import { SessionService } from '../core/session.service';

/** The signed-in frame: left menu for the person's role, bell with notifications, and the page area. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DatePipe],
  template: `
    <div class="app">
      <aside class="rail" [class.open]="menuOpen()" id="sidebar">
        <div class="brand">
          <span class="mk" aria-hidden="true">
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path [attr.d]="icons.layers" /></svg>
          </span>
          <div><b>ITZETech Group</b><small>Digital Project Delivery</small></div>
        </div>
        <div class="rail-scroll">
          <nav class="nav" aria-label="Main menu" (click)="menuOpen.set(false)">
            @for (g of groups(); track g.label) {
              <div class="nav-group" role="group" [attr.aria-label]="g.label">
                <div class="nav-label" aria-hidden="true">{{ g.label }}</div>
                @for (item of g.items; track item.path) {
                  <a [routerLink]="item.path" routerLinkActive="active" [attr.aria-label]="item.label">
                    <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="iconFor(item.path)" /></svg>
                    <span>{{ item.label }}</span>
                    @if (item.path === '/notifications' && notes.unread() > 0) { <span class="badge">{{ notes.unread() }}</span> }
                  </a>
                }
              </div>
            }
          </nav>
        </div>
        <div class="acct">
          <div class="acct-who">
            <span class="av" aria-hidden="true">{{ initial() }}</span>
            <div><b>{{ user()?.fullName }}</b><span class="muted small">{{ roleLabel() }}</span></div>
          </div>
          <button class="signout" type="button" (click)="signOut()">
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="icons.logout" /></svg>
            Sign out
          </button>
        </div>
      </aside>
      <div class="scrim" [class.open]="menuOpen()" (click)="menuOpen.set(false)" aria-hidden="true"></div>

      <div class="main-col">
        <header class="topbar">
          <button class="menubtn" type="button" (click)="menuOpen.set(!menuOpen())" aria-controls="sidebar" [attr.aria-expanded]="menuOpen()" aria-label="Open the menu">
            <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="icons.menu" /></svg>
          </button>
          <div class="topbar-hello">
            <b>{{ greeting() }}</b>
            <span>{{ today | date: 'EEEE, d MMMM y' }}</span>
          </div>
          <div class="tools">
            <button class="iconbtn" type="button" (click)="togglePanel()" aria-haspopup="true" [attr.aria-expanded]="panelOpen()"
                    [attr.aria-label]="'Notifications, ' + notes.unread() + ' unread'">
              <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path [attr.d]="icons.bell" />
              </svg>
              @if (notes.unread() > 0) { <span class="dotbadge">{{ notes.unread() }}</span> }
            </button>
            <span class="chip who"><span class="av sm" aria-hidden="true">{{ initial() }}</span>{{ user()?.fullName }}</span>

            @if (panelOpen()) {
              <div class="overlay clear" style="z-index:30" (click)="panelOpen.set(false)"></div>
              <div class="card notif-panel" role="dialog" aria-label="Notifications">
                <header>
                  <h3>Notifications</h3>
                  <button class="link small" type="button" (click)="notes.markAllRead()" [disabled]="notes.unread() === 0">Mark all as read</button>
                </header>
                @if (notes.failed()) {
                  <p class="err">Could not load notifications.</p>
                } @else if (notes.items().length === 0) {
                  <p class="muted empty">Nothing new. We will tell you here when something needs you.</p>
                } @else {
                  <div class="list">
                    @for (n of recent(); track n.id) {
                      <div class="item" [class.unread]="!n.read">
                        <button class="itembtn" type="button" (click)="open(n)">
                          <div class="grow"><div class="l1">{{ n.message }}</div><div class="l2 muted small">{{ n.createdAt | date: 'd MMM, HH:mm' }}</div></div>
                          @if (!n.read) { <span class="pill p-info">New</span> }
                        </button>
                      </div>
                    }
                  </div>
                  <p style="margin-top:14px"><a routerLink="/notifications" (click)="panelOpen.set(false)">See all notifications</a></p>
                }
              </div>
            }
          </div>
        </header>
        <main class="main">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class ShellComponent implements OnDestroy {
  private readonly session = inject(SessionService);
  private readonly auth = inject(AuthService);
  readonly notes = inject(NotificationsService);

  readonly user = this.session.user;
  readonly panelOpen = signal(false);
  readonly menuOpen = signal(false);
  readonly icons = ICON;
  readonly today = new Date();
  readonly greeting = computed(() => {
    const h = this.today.getHours();
    const part = h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening';
    const first = (this.user()?.fullName ?? '').split(' ')[0];
    return first ? `${part}, ${first}` : part;
  });
  readonly groups = computed(() => {
    const items = this.menu();
    return NAV_GROUP_ORDER.map((label) => ({ label, items: items.filter((i) => (NAV_GROUP[i.path] ?? 'Overview') === label) })).filter((g) => g.items.length > 0);
  });
  readonly menu = computed(() => {
    const u = this.user();
    return u ? NAV[u.role] : [];
  });
  readonly roleLabel = computed(() => {
    const u = this.user();
    return u ? (u.companyName ? `${ROLE_LABEL[u.role]} · ${u.companyName}` : ROLE_LABEL[u.role]) : '';
  });
  readonly initial = computed(() => initials(this.user()?.fullName ?? ''));
  readonly recent = computed(() => this.notes.items().slice(0, 8));

  constructor() {
    // refresh the person (also proves the saved token is still valid) and start the bell
    this.auth.me().subscribe({ error: () => undefined });
    this.notes.startPolling();
  }

  ngOnDestroy(): void {
    this.notes.reset();
  }

  iconFor(path: string): string {
    return navIcon(path);
  }

  togglePanel(): void {
    this.panelOpen.update((v) => !v);
    if (this.panelOpen()) {
      this.notes.refresh();
    }
  }

  open(n: AppNotification): void {
    this.panelOpen.set(false);
    this.notes.open(n);
  }

  signOut(): void {
    this.notes.reset();
    this.auth.logout();
  }

  @HostListener('document:keydown.escape')
  closePanel(): void {
    this.panelOpen.set(false);
    this.menuOpen.set(false);
  }
}
