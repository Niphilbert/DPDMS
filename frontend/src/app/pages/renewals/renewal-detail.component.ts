import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { RENEWAL_STATUS, RENEWAL_TYPE, daysText, rwf } from '../../core/format';
import { Renewal } from '../../core/models';
import { canManageRenewals, canSeeMoney } from '../../core/permissions';
import { RenewalsService } from '../../core/renewals.service';
import { SessionService } from '../../core/session.service';
import { ToastService } from '../../core/toast.service';
import { RenewalFormComponent } from './renewal-form.component';

type Dialog = 'edit' | 'renew' | 'delete' | null;

@Component({
  selector: 'app-renewal-detail',
  imports: [FormsModule, DatePipe, RouterLink, RenewalFormComponent],
  template: `
    <div class="top">
      <div>
        <p class="small"><a routerLink="/renewals">← Renewals</a></p>
        <h1>{{ renewal()?.label ?? 'Renewal' }}</h1>
        @if (renewal(); as r) {
          <p class="sub"><span class="pill" [class]="badge().cls">{{ badge().label }}</span> {{ typeLabel() }} · {{ r.projectName }}</p>
        }
      </div>
      @if (renewal(); as r) {
        <div class="tools">
          @if (canManage()) {
            <button class="btn primary" type="button" (click)="openRenew()">Renew</button>
            <button class="btn" type="button" (click)="dialog.set('edit')">Edit</button>
            <button class="btn danger" type="button" (click)="openDelete()">Delete</button>
          }
          @if (isClient()) {
            <button class="btn primary" type="button" (click)="request()" [disabled]="requested() || busy()">{{ requested() ? 'Renewal requested' : 'Request renewal' }}</button>
          }
        </div>
      }
    </div>

    @if (loading()) {
      <p class="muted loading">Loading…</p>
    } @else if (loadError()) {
      <div class="callout bad" role="alert">{{ loadError() }}</div>
    } @else if (renewal(); as r) {
      @if (actionError()) { <div class="callout bad" role="alert">{{ actionError() }}</div> }
      @if (r.status === 'EXPIRED') {
        <div class="callout bad" role="status"><b>Expired {{ -r.daysLeft }} {{ r.daysLeft === -1 ? 'day' : 'days' }} ago</b> ({{ r.expiresOn | date: 'd MMM y' }}). The service may stop working until it is renewed.</div>
      } @else if (r.status === 'EXPIRING') {
        <div class="callout warn" role="status"><b>Expires {{ daysText(r.daysLeft) }}</b> ({{ r.expiresOn | date: 'd MMM y' }}). {{ isClient() ? 'Select "Request renewal" so ITZETech renews it in time.' : 'Renew it before the expiry date.' }}</div>
      }

      <div class="grid2">
        <section class="card" aria-labelledby="rd">
          <header><h2 id="rd">Details</h2></header>
          <dl>
            @if (!isClient()) { <dt>Client</dt><dd>{{ r.companyName }}</dd> }
            <dt>Project</dt><dd><a [routerLink]="['/projects', r.projectId]">{{ r.projectName }}</a></dd>
            <dt>Type</dt><dd>{{ typeLabel() }}</dd>
            @if (r.provider) { <dt>Provider</dt><dd>{{ r.provider }}</dd> }
            <dt>Expires on</dt><dd>{{ r.expiresOn | date: 'd MMM y' }} · {{ daysText(r.daysLeft) }}</dd>
            @if (showPrice() && r.price !== undefined) { <dt>Next renewal</dt><dd class="num">{{ money(r.price) }}</dd> }
            <dt>Renewed</dt><dd>{{ r.renewalCount }} {{ r.renewalCount === 1 ? 'time' : 'times' }}@if (r.lastRenewedOn) { · last on {{ r.lastRenewedOn | date: 'd MMM y' }} }</dd>
            @if (r.note) { <dt>Internal note</dt><dd class="note">{{ r.note }}</dd> }
          </dl>
        </section>

        <section class="card" aria-labelledby="rh">
          <header><h2 id="rh">Renewal history</h2></header>
          @if (!r.history || r.history.length === 0) {
            <p class="muted empty">Not renewed yet.</p>
          } @else {
            <div class="tblwrap cardtable">
              <table style="min-width:0">
                <thead><tr><th>Renewed on</th><th>Expiry</th>@if (showPrice()) { <th class="right">Price</th> }<th>By</th></tr></thead>
                <tbody>
                  @for (h of r.history; track h.id) {
                    <tr>
                      <td class="first num">{{ h.renewedOn | date: 'd MMM y' }}</td>
                      <td class="num" data-label="Expiry">{{ h.previousExpiresOn | date: 'd MMM y' }} → {{ h.newExpiresOn | date: 'd MMM y' }}@if (h.note && !isClient()) { <div class="l2">{{ h.note }}</div> }</td>
                      @if (showPrice()) { <td class="num right" data-label="Price">{{ h.price === null ? '—' : money(h.price) }}</td> }
                      <td data-label="By">{{ h.renewedByName }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>
      </div>

      @switch (dialog()) {
        @case ('edit') { <app-renewal-form [renewal]="r" (saved)="edited($event)" (cancelled)="dialog.set(null)" /> }
        @case ('renew') {
          <div class="overlay" (click)="dialog.set(null)"></div>
          <form class="modal" role="dialog" aria-modal="true" aria-labelledby="rnt" (ngSubmit)="renew()" novalidate>
            <h2 id="rnt">Renew {{ r.label }}</h2>
            <p class="muted small">Currently expires on {{ r.expiresOn | date: 'd MMM y' }}. The client company is told it was renewed and reminders start again for the new date.</p>
            <div class="formgrid">
              <div class="field">
                <label for="rne">New expiry date</label>
                <input id="rne" name="rne" type="date" [min]="r.expiresOn" [class.invalid]="fields()['newExpiresOn']" [(ngModel)]="newExpiry" />
                @if (fields()['newExpiresOn']) { <span class="fielderr">{{ fields()['newExpiresOn'] }}</span> }
              </div>
              <div class="field">
                <label for="rnp">Price paid (RWF)</label>
                <input id="rnp" name="rnp" type="number" min="0" step="1000" inputmode="numeric" [class.invalid]="fields()['price']" [(ngModel)]="newPrice" />
                @if (fields()['price']) { <span class="fielderr">{{ fields()['price'] }}</span> }
              </div>
              <div class="field wide">
                <label for="rnn">Note (optional)</label>
                <textarea id="rnn" name="rnn" maxlength="500" [class.invalid]="fields()['note']" [(ngModel)]="newNote"></textarea>
              </div>
            </div>
            @if (dialogError()) { <p class="err" role="alert">{{ dialogError() }}</p> }
            <div class="tools">
              <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Mark as renewed' }}</button>
              <button class="btn" type="button" (click)="dialog.set(null)">Cancel</button>
            </div>
          </form>
        }
        @case ('delete') {
          <div class="overlay" (click)="dialog.set(null)"></div>
          <div class="modal" role="dialog" aria-modal="true" aria-labelledby="rdt">
            <h2 id="rdt">Delete {{ r.label }}?</h2>
            <p>This removes the item and its reminders. An item that has been renewed before cannot be deleted.</p>
            @if (dialogError()) { <p class="err" role="alert">{{ dialogError() }}</p> }
            <div class="tools">
              <button class="btn danger" type="button" (click)="remove()" [disabled]="busy()">{{ busy() ? 'Deleting…' : 'Delete' }}</button>
              <button class="btn" type="button" (click)="dialog.set(null)">Cancel</button>
            </div>
          </div>
        }
      }
    }
  `,
})
export class RenewalDetailComponent implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(RenewalsService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly renewal = signal<Renewal | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly actionError = signal('');
  readonly dialogError = signal('');
  readonly fields = signal<Record<string, string>>({});
  readonly busy = signal(false);
  readonly requested = signal(false);
  readonly dialog = signal<Dialog>(null);
  newExpiry = '';
  newPrice: number | null = null;
  newNote = '';

  readonly role = computed(() => this.session.user()?.role);
  readonly isClient = computed(() => this.role() === 'CLIENT');
  readonly canManage = computed(() => canManageRenewals(this.role()));
  readonly showPrice = computed(() => canSeeMoney(this.role()));
  readonly badge = computed(() => RENEWAL_STATUS[this.renewal()?.status ?? 'ACTIVE']);
  readonly typeLabel = computed(() => RENEWAL_TYPE[this.renewal()?.type ?? 'DOMAIN']);

  ngOnInit(): void {
    this.api.get(this.id()).subscribe({
      next: (r) => {
        this.renewal.set(r);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.loadError.set(toApiError(e).message);
      },
    });
  }

  daysText = daysText;

  money(n: number): string {
    return rwf(n) || '0 RWF';
  }

  edited(r: Renewal): void {
    this.renewal.set(r);
    this.dialog.set(null);
    this.toast.show('Renewal saved');
  }

  openRenew(): void {
    const r = this.renewal();
    if (!r) return;
    this.fields.set({});
    this.dialogError.set('');
    const d = new Date(r.expiresOn + 'T00:00:00');
    d.setFullYear(d.getFullYear() + 1);
    this.newExpiry = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    this.newPrice = r.price ?? null;
    this.newNote = '';
    this.dialog.set('renew');
  }

  openDelete(): void {
    this.dialogError.set('');
    this.dialog.set('delete');
  }

  renew(): void {
    this.fields.set({});
    this.dialogError.set('');
    this.busy.set(true);
    this.api
      .renew(this.id(), {
        newExpiresOn: this.newExpiry,
        price: this.newPrice === null || (this.newPrice as unknown) === '' ? undefined : Number(this.newPrice),
        note: this.newNote.trim() || undefined,
      })
      .subscribe({
        next: (r) => {
          this.busy.set(false);
          this.renewal.set(r);
          this.dialog.set(null);
          this.toast.show('Marked as renewed. The client was told.');
        },
        error: (e) => {
          const err = toApiError(e);
          this.busy.set(false);
          this.fields.set(err.fields);
          this.dialogError.set(err.code === 'VALIDATION_FAILED' ? 'Please correct the fields marked below.' : err.message);
        },
      });
  }

  remove(): void {
    this.busy.set(true);
    this.dialogError.set('');
    this.api.remove(this.id()).subscribe({
      next: () => {
        this.busy.set(false);
        this.toast.show('Renewal deleted');
        void this.router.navigate(['/renewals']);
      },
      error: (e) => {
        this.busy.set(false);
        this.dialogError.set(toApiError(e).message);
      },
    });
  }

  request(): void {
    this.busy.set(true);
    this.actionError.set('');
    this.api.requestRenewal(this.id()).subscribe({
      next: () => {
        this.busy.set(false);
        this.requested.set(true);
        this.toast.show('We told ITZETech you want this renewed');
      },
      error: (e) => {
        this.busy.set(false);
        this.actionError.set(toApiError(e).message);
      },
    });
  }
}
