import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toApiError } from '../../core/api-error';
import { invoiceAmounts, lineTotal, plainNumber, rwf, todayIso } from '../../core/format';
import { InvoicesService } from '../../core/invoices.service';
import { Company, Invoice, InvoiceInput, InvoiceSuggestion, Project } from '../../core/models';
import { ProjectsService } from '../../core/projects.service';

interface Row {
  description: string;
  quantity: number | null;
  unitPrice: number | null;
}

const EMPTY_ROW = (): Row => ({ description: '', quantity: 1, unitPrice: null });

/**
 * Create or edit a draft invoice, as a dialog. Line items with live totals, tax, dates and a note. When a project is
 * chosen the form offers the time-based suggestion from the server (hours logged, remaining budget). Nothing is
 * saved until "Save draft".
 */
@Component({
  selector: 'app-invoice-form',
  imports: [FormsModule],
  template: `
    <div class="overlay" (click)="cancelled.emit()"></div>
    <form class="modal wide" role="dialog" aria-modal="true" aria-labelledby="ift" (ngSubmit)="save()" novalidate>
      <h2 id="ift">{{ invoice() ? 'Edit draft ' + invoice()!.number : 'New invoice' }}</h2>
      <p class="muted small">A draft is only visible to ITZETech staff. The client sees it after you send it.</p>

      <div class="formgrid">
        @if (invoice() || presetProjectId()) {
          <div class="field wide">
            <span class="caps">Billed to</span>
            <div><b>{{ fixedCompany() }}</b>@if (fixedProject()) { <span class="muted"> · {{ fixedProject() }}</span> }</div>
          </div>
        } @else {
          <div class="field wide">
            <label for="ip">Project (optional)</label>
            <select id="ip" name="ip" [ngModel]="projectId()" (ngModelChange)="pickProject($event)">
              <option value="">No project (company invoice, for example a renewal fee)</option>
              @for (p of projects(); track p.id) { <option [value]="p.id">{{ p.name }} · {{ p.companyName }}</option> }
            </select>
          </div>
          @if (!projectId()) {
            <div class="field wide">
              <label for="ic">Client company</label>
              <select id="ic" name="ic" [class.invalid]="fields()['companyId']" [(ngModel)]="companyId">
                <option value="">Choose a company</option>
                @for (c of companies(); track c.id) { <option [value]="c.id">{{ c.name }}</option> }
              </select>
              @if (fields()['companyId']) { <span class="fielderr">{{ fields()['companyId'] }}</span> }
            </div>
          }
        }
        <div class="field">
          <label for="id1">Issue date</label>
          <input id="id1" name="id1" type="date" [class.invalid]="fields()['issueDate']" [(ngModel)]="issueDate" />
          @if (fields()['issueDate']) { <span class="fielderr">{{ fields()['issueDate'] }}</span> }
        </div>
        <div class="field">
          <label for="id2">Due date</label>
          <input id="id2" name="id2" type="date" [class.invalid]="fields()['dueDate']" [(ngModel)]="dueDate" />
          @if (fields()['dueDate']) { <span class="fielderr">{{ fields()['dueDate'] }}</span> }
        </div>
      </div>

      @if (suggestion(); as s) {
        @if (s.timeLine || s.balanceLine) {
          <div class="callout info">
            <b>Suggested from the project.</b> {{ money(s.budget) }} budget, {{ money(s.invoiced) }} already invoiced, {{ money(s.unbilled) }} not invoiced yet.
            <div class="tools" style="margin-top:8px">
              @if (s.timeLine; as t) {
                <button class="btn sm" type="button" (click)="addSuggested(t)">Add {{ plainNumber(t.quantity) }} h logged at {{ money(t.unitPrice) }} ({{ money(t.lineTotal) }})</button>
              }
              @if (s.balanceLine; as b) {
                <button class="btn sm" type="button" (click)="addSuggested(b)">Add remaining budget ({{ money(b.lineTotal) }})</button>
              }
            </div>
          </div>
        }
      }

      <div class="lines" role="group" aria-label="Invoice lines">
        <div class="lhead" aria-hidden="true"><span>Description</span><span>Quantity</span><span>Unit price (RWF)</span><span class="right">Line total</span><span></span></div>
        @for (l of rows(); track $index) {
          <div class="lrow">
            <div class="field">
              <input [attr.aria-label]="'Description, line ' + ($index + 1)" placeholder="For example: Design milestone" [class.invalid]="fields()['lines[' + $index + '].description']" [ngModel]="l.description" (ngModelChange)="set($index, 'description', $event)" [ngModelOptions]="{ standalone: true }" />
              @if (fields()['lines[' + $index + '].description']) { <span class="fielderr">{{ fields()['lines[' + $index + '].description'] }}</span> }
            </div>
            <div class="field">
              <input [attr.aria-label]="'Quantity, line ' + ($index + 1)" type="number" min="0.01" step="0.25" inputmode="decimal" [class.invalid]="fields()['lines[' + $index + '].quantity']" [ngModel]="l.quantity" (ngModelChange)="set($index, 'quantity', $event)" [ngModelOptions]="{ standalone: true }" />
              @if (fields()['lines[' + $index + '].quantity']) { <span class="fielderr">{{ fields()['lines[' + $index + '].quantity'] }}</span> }
            </div>
            <div class="field">
              <input [attr.aria-label]="'Unit price, line ' + ($index + 1)" type="number" min="1" step="1000" inputmode="numeric" [class.invalid]="fields()['lines[' + $index + '].unitPrice']" [ngModel]="l.unitPrice" (ngModelChange)="set($index, 'unitPrice', $event)" [ngModelOptions]="{ standalone: true }" />
              @if (fields()['lines[' + $index + '].unitPrice']) { <span class="fielderr">{{ fields()['lines[' + $index + '].unitPrice'] }}</span> }
            </div>
            <div class="lt num" [attr.aria-label]="'Line total ' + ($index + 1)">{{ money(rowTotal(l)) }}</div>
            <button class="btn sm" type="button" (click)="remove($index)" [disabled]="rows().length === 1" [attr.aria-label]="'Remove line ' + ($index + 1)">Remove</button>
          </div>
        }
        <p><button class="link" type="button" (click)="add()" [disabled]="rows().length >= 50">Add a line</button></p>
        @if (fields()['lines']) { <span class="fielderr">{{ fields()['lines'] }}</span> }
      </div>

      <div class="formgrid">
        <div class="field">
          <label for="it">Tax rate (%)</label>
          <input id="it" name="it" type="number" min="0" max="100" step="0.5" [class.invalid]="fields()['taxRate']" [ngModel]="taxRate()" (ngModelChange)="taxRate.set($event)" />
          @if (fields()['taxRate']) { <span class="fielderr">{{ fields()['taxRate'] }}</span> }
        </div>
        <dl class="sumbox" aria-live="polite" aria-label="Totals">
          <dt>Subtotal</dt><dd class="num right">{{ money(amounts().subtotal) }}</dd>
          <dt>Tax ({{ taxText() }}%)</dt><dd class="num right">{{ money(amounts().taxAmount) }}</dd>
          <dt class="grand">Total</dt><dd class="grand num right">{{ money(amounts().total) }}</dd>
        </dl>
        <div class="field wide">
          <label for="in">Note for the client (optional)</label>
          <textarea id="in" name="in" maxlength="1000" [class.invalid]="fields()['note']" [(ngModel)]="note" placeholder="For example: Milestone 2, design approval"></textarea>
          @if (fields()['note']) { <span class="fielderr">{{ fields()['note'] }}</span> }
        </div>
      </div>

      @if (formError()) { <p class="err" role="alert">{{ formError() }}</p> }
      <div class="tools">
        <button class="btn primary" type="submit" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save draft' }}</button>
        <button class="btn" type="button" (click)="cancelled.emit()">Cancel</button>
      </div>
    </form>
  `,
})
export class InvoiceFormComponent implements OnInit {
  /** The draft being edited, or null for a new invoice. */
  readonly invoice = input<Invoice | null>(null);
  /** For a new invoice opened from a project page: the project is fixed. */
  readonly presetProjectId = input<string | null>(null);
  readonly saved = output<Invoice>();
  readonly cancelled = output<void>();

  private readonly api = inject(InvoicesService);
  private readonly projectsApi = inject(ProjectsService);

  readonly projects = signal<Project[]>([]);
  readonly companies = signal<Company[]>([]);
  readonly suggestion = signal<InvoiceSuggestion | null>(null);
  readonly rows = signal<Row[]>([EMPTY_ROW()]);
  readonly taxRate = signal<number | null>(0);
  readonly projectId = signal('');
  readonly fields = signal<Record<string, string>>({});
  readonly formError = signal('');
  readonly busy = signal(false);
  companyId = '';
  issueDate = todayIso();
  dueDate = todayIso(14);
  note = '';

  readonly amounts = computed(() => invoiceAmounts(this.rows().map((r) => ({ quantity: r.quantity ?? 0, unitPrice: r.unitPrice ?? 0 })), this.taxRate() ?? 0));
  readonly taxText = computed(() => plainNumber(this.taxRate() ?? 0));
  readonly fixedCompany = computed(() => {
    const inv = this.invoice();
    if (inv) return inv.companyName;
    return this.projects().find((p) => p.id === this.presetProjectId())?.companyName ?? '';
  });
  readonly fixedProject = computed(() => {
    const inv = this.invoice();
    if (inv) return inv.projectName ?? '';
    return this.projects().find((p) => p.id === this.presetProjectId())?.name ?? '';
  });

  ngOnInit(): void {
    const inv = this.invoice();
    if (inv) {
      this.issueDate = inv.issueDate;
      this.dueDate = inv.dueDate;
      this.note = inv.note ?? '';
      this.taxRate.set(inv.taxRate);
      this.rows.set(inv.lines.map((l) => ({ description: l.description, quantity: l.quantity, unitPrice: l.unitPrice })));
      return;
    }
    const preset = this.presetProjectId();
    if (preset) {
      this.projectId.set(preset);
      this.loadSuggestion(preset);
      this.projectsApi.list().subscribe({ next: (list) => this.projects.set(list), error: () => undefined });
    } else {
      this.projectsApi.list().subscribe({ next: (list) => this.projects.set(list), error: () => undefined });
      this.api.companies().subscribe({ next: (list) => this.companies.set(list), error: () => undefined });
    }
  }

  money(n: number): string {
    return rwf(n) || '0 RWF';
  }

  plainNumber = plainNumber;

  rowTotal(r: Row): number {
    return lineTotal(r.quantity ?? 0, r.unitPrice ?? 0);
  }

  set(i: number, key: keyof Row, value: string | number | null): void {
    this.rows.update((rs) =>
      rs.map((r, idx) => {
        if (idx !== i) return r;
        if (key === 'description') return { ...r, description: String(value ?? '') };
        return { ...r, [key]: value === null || value === '' ? null : Number(value) };
      }),
    );
  }

  add(): void {
    this.rows.update((rs) => [...rs, EMPTY_ROW()]);
  }

  remove(i: number): void {
    this.rows.update((rs) => rs.filter((_, idx) => idx !== i));
  }

  pickProject(id: string): void {
    this.projectId.set(id);
    this.suggestion.set(null);
    if (id) this.loadSuggestion(id);
  }

  /** Drops a suggested line into the form. A single empty starting row is replaced instead of kept. */
  addSuggested(line: { description: string; quantity: number; unitPrice: number }): void {
    const blank = (r: Row) => !r.description.trim() && !r.unitPrice;
    this.rows.update((rs) => [...rs.filter((r) => !blank(r)), { description: line.description, quantity: line.quantity, unitPrice: line.unitPrice }]);
  }

  private loadSuggestion(projectId: string): void {
    this.api.suggestion(projectId).subscribe({ next: (s) => this.suggestion.set(s), error: () => this.suggestion.set(null) });
  }

  save(): void {
    this.fields.set({});
    this.formError.set('');
    const inv = this.invoice();
    const body: InvoiceInput = {
      issueDate: this.issueDate,
      dueDate: this.dueDate,
      taxRate: Number(this.taxRate() ?? 0),
      note: this.note.trim() || undefined,
      lines: this.rows().map((r) => ({ description: r.description.trim(), quantity: Number(r.quantity ?? 0), unitPrice: Number(r.unitPrice ?? 0) })),
    };
    if (inv) {
      if (inv.projectId) body.projectId = inv.projectId;
      else body.companyId = inv.companyId;
    } else if (this.projectId()) {
      body.projectId = this.projectId();
    } else {
      body.companyId = this.companyId || undefined;
    }
    this.busy.set(true);
    const call = inv ? this.api.update(inv.id, body) : this.api.create(body);
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
