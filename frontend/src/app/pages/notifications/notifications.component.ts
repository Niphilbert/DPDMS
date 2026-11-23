import { DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { NotificationsService } from '../../core/notifications.service';

@Component({
  selector: 'app-notifications',
  imports: [DatePipe],
  template: `
    <div class="top">
      <div>
        <h1>Notifications</h1>
        <p class="sub">Select a notification to mark it as read and go to the page it is about.</p>
      </div>
      <div class="tools">
        <button class="btn" type="button" (click)="notes.markAllRead()" [disabled]="notes.unread() === 0">Mark all as read</button>
      </div>
    </div>

    <section class="card" aria-label="All notifications">
      @if (!notes.loaded()) {
        <p class="muted loading">Loading…</p>
      } @else if (notes.failed()) {
        <p class="err">Could not load notifications. Please try again.</p>
      } @else if (notes.items().length === 0) {
        <p class="muted empty">Nothing here yet.</p>
      } @else {
        <div class="list nlist">
          @for (n of notes.items(); track n.id) {
            <div class="item" [class.unread]="!n.read">
              <button class="itembtn" type="button" (click)="notes.open(n)">
                <span class="dot" [style.visibility]="n.read ? 'hidden' : 'visible'" aria-hidden="true"></span>
                <div class="grow"><div class="l1">{{ n.message }}</div><div class="l2 muted small">{{ n.createdAt | date: 'd MMM y, HH:mm' }}</div></div>
                @if (!n.read) { <span class="pill p-info">New</span> } @else { <span class="pill p-neutral">Read</span> }
              </button>
            </div>
          }
        </div>
      }
    </section>
  `,
})
export class NotificationsComponent implements OnInit {
  readonly notes = inject(NotificationsService);

  ngOnInit(): void {
    this.notes.refresh();
  }
}
