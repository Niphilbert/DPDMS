import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toApiError } from '../../core/api-error';
import { RENEWAL_TYPE, todayIso } from '../../core/format';
import { Project, Renewal, RenewalInput, RenewalType } from '../../core/models';
import { ProjectsService } from '../../core/projects.service';
import { RenewalsService } from '../../core/renewals.service';

/** Add a renewal or edit one (management). The project is chosen when adding and cannot change afterwards. */
@Component({
  selector: 'app-renewal-form',
  imports: [FormsModule],
  template: `
    <div class="overlay" (click)="cancelled.emit()"></div>
    <form class="modal" role="dialog" aria-modal="true" aria-labelledby="rft" (ngSubmit)="save()" novalidate>
      <h2 id="rft">{{ renewal() ? 'Edit renewal' : 'Add a renewal' }}</h2>
      <p class="muted small">Reminders go out 60, 30, 14 and 7 days before the expiry date, and once on the day it expires. Changing the expiry date starts the reminders again.</p>
      <div class="formgrid">
        @if (renewal(); as r) {
          <div class="field wide"><span class="caps">Project</span><div><b>{{ r.projectName }}</b> <span class="muted">· {{ r.companyName }}</span></div></div>
        } @else {
          <div class="field wide">
            <label for="rp">Project</label>
            <select id="rp" name="rp" [class.invalid]="fields()['projectId']" [(ngModel)]="projectId">
              <option value="">Choose a project</option>
              @for (p of projects(); track p.id) { <option [value]="p.id">{{ p.name }} · {{ p.companyName }}</option> }
            </select>
            @if (fields()['projectId']) { <span class="fielderr">{{ fields()['projectId'] }}</span> }
          </div>
        }
        <div class="field">
          <label for="rty">Type</label>
          <select id="rty" name="rty" [(ngModel)]="type">
            @for (t of types; track t) { <option [value]="t">{{ typeLabel(t) }}</option> }
          </select>
        </div>
        <div class="field">
          <label for="rex">Expires on</label>
          <input id="rex" name="rex" type="date" [class.invalid]="fields()['expiresOn']" [(ngModel)]="expiresOn" />
          @if (fields()['expiresOn']) { <span class="fielderr">{{ fields()['expiresOn'] }}</span> }
        </div>
        <div class="field wide">
          <label for="rl">Name</label>
          <input id="rl" name="rl" maxlength="200" placeholder="For example: acmehotels.example or Business hosting plan" [class.invalid]="fields()['label']" [(ngModel)]="label" />
          @if (fields()['label']) { <span class="fielderr">{{ fields()['label'] }}</span> }
        </div>
        <div class="field">
          <label for="rpr">Provider (optional)</label>
          <input id="rpr" name="rpr" maxlength="150" [class.invalid]="fields()['provider']" [(ngModel)]="provider" />
          @if (fields()['provider']) { <span class="fielderr">{{ fields()['provider'] }}</span> }
        </div>
        <div class="field">
          <label for="rpc">Price of the next renewal (RWF)</label>
          <input id="rpc" name="rpc" type="number" min="0" step="1000" inputmode="numeric" [class.invalid]="fields()['price']" [(ngModel)]="price" />
          @if (fields()['price']) { <span class="fielderr">{{ fields()['price'] }}</span> }
        </div>
        <div class="field wide">
          <label for="rn">Internal note (optional, the client does not see it)</label>
          <textarea id="rn" name="rn" maxlength="500" [class.invalid]="fields()['note']" [(ngModel)]="note"></textarea>
          @if (fields()['note']) { <span class="fielderr">{{ fields()['note'] }}</span> }
        </div>
      </div>
      @if (formError()) { <p class="err" role="alert">{{ formError() }}</p> }
      <div class="tools">
        <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save' }}</button>
        <button class="btn" type="button" (click)="cancelled.emit()">Cancel</button>
      </div>
    </form>
  `,
})
export class RenewalFormComponent implements OnInit {
  readonly renewal = input<Renewal | null>(null);
  readonly saved = output<Renewal>();
  readonly cancelled = output<void>();

  private readonly api = inject(RenewalsService);
  private readonly projectsApi = inject(ProjectsService);

  readonly types: RenewalType[] = ['DOMAIN', 'HOSTING', 'SSL', 'MAINTENANCE'];
  readonly projects = signal<Project[]>([]);
  readonly fields = signal<Record<string, string>>({});
  readonly formError = signal('');
  readonly busy = signal(false);
  projectId = '';
  type: RenewalType = 'DOMAIN';
  label = '';
  provider = '';
  expiresOn = todayIso(365);
  price: number | null = null;
  note = '';

  ngOnInit(): void {
    const r = this.renewal();
    if (r) {
      this.type = r.type;
      this.label = r.label;
      this.provider = r.provider ?? '';
      this.expiresOn = r.expiresOn;
      this.price = r.price ?? 0;
      this.note = r.note ?? '';
    } else {
      this.projectsApi.list().subscribe({ next: (l) => this.projects.set(l), error: () => undefined });
    }
  }

  typeLabel(t: RenewalType): string {
    return RENEWAL_TYPE[t];
  }

  save(): void {
    this.fields.set({});
    this.formError.set('');
    const body: RenewalInput = {
      type: this.type,
      label: this.label.trim(),
      provider: this.provider.trim() || undefined,
      expiresOn: this.expiresOn,
      price: Number(this.price ?? 0),
      note: this.note.trim() || undefined,
    };
    const r = this.renewal();
    if (!r && !this.projectId) {
      this.fields.set({ projectId: 'Choose the project this belongs to.' });
      this.formError.set('Please correct the fields marked below.');
      return;
    }
    this.busy.set(true);
    const call = r ? this.api.update(r.id, body) : this.api.create({ ...body, projectId: this.projectId });
    call.subscribe({
      next: (saved) => {
        this.busy.set(false);
        this.saved.emit(saved);
      },
      error: (e) => {
        const err = toApiError(e);
        this.busy.set(false);
        this.fields.set(err.fields);
        this.formError.set(err.code === 'VALIDATION_FAILED' ? 'Please correct the fields marked below.' : err.message);
      },
    });
  }
}
