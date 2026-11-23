import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Company,
  Invoice,
  InvoiceFilter,
  InvoiceInput,
  InvoiceSuggestion,
  InvoiceSummary,
  InvoiceTotals,
  PaymentMethod,
  ProjectMoney,
} from './models';

/** Query string for the list, the totals and the CSV: only the filters that are set. */
export function invoiceParams(f: InvoiceFilter): HttpParams {
  let params = new HttpParams();
  if (f.status) params = params.set('status', f.status);
  if (f.projectId) params = params.set('projectId', f.projectId);
  if (f.companyId) params = params.set('companyId', f.companyId);
  if (f.from) params = params.set('from', f.from);
  if (f.to) params = params.set('to', f.to);
  return params;
}

/** Invoices, payments and project money (docs/api-stage3.md section 1) and the company pick list (section 5). */
@Injectable({ providedIn: 'root' })
export class InvoicesService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  list(filter: InvoiceFilter = {}): Observable<InvoiceSummary[]> {
    return this.http.get<InvoiceSummary[]>(`${this.api}/invoices`, { params: invoiceParams(filter) });
  }

  get(id: string): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.api}/invoices/${id}`);
  }

  /** Totals for the invoices the person can see; management may narrow them by project and company. */
  totals(filter: Pick<InvoiceFilter, 'projectId' | 'companyId'> = {}): Observable<InvoiceTotals> {
    return this.http.get<InvoiceTotals>(`${this.api}/invoices/summary`, { params: invoiceParams(filter) });
  }

  create(body: InvoiceInput): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.api}/invoices`, body);
  }

  update(id: string, body: InvoiceInput): Observable<Invoice> {
    return this.http.put<Invoice>(`${this.api}/invoices/${id}`, body);
  }

  send(id: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.api}/invoices/${id}/send`, {});
  }

  recordPayment(id: string, body: { amount: number; paidOn: string; method: PaymentMethod; reference?: string }): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.api}/invoices/${id}/payments`, body);
  }

  reversePayment(id: string, paymentId: string, reason: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.api}/invoices/${id}/payments/${paymentId}/reverse`, { reason });
  }

  void(id: string, reason?: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.api}/invoices/${id}/void`, reason?.trim() ? { reason: reason.trim() } : {});
  }

  remind(id: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.api}/invoices/${id}/remind`, {});
  }

  exportCsv(filter: InvoiceFilter = {}): Observable<Blob> {
    return this.http.get(`${this.api}/invoices/export.csv`, { params: invoiceParams(filter), responseType: 'blob' });
  }

  forProject(projectId: string): Observable<InvoiceSummary[]> {
    return this.http.get<InvoiceSummary[]>(`${this.api}/projects/${projectId}/invoices`);
  }

  projectMoney(projectId: string): Observable<ProjectMoney> {
    return this.http.get<ProjectMoney>(`${this.api}/projects/${projectId}/money`);
  }

  suggestion(projectId: string): Observable<InvoiceSuggestion> {
    return this.http.get<InvoiceSuggestion>(`${this.api}/projects/${projectId}/invoice-suggestion`);
  }

  /** Client companies, for the pick lists (management only). */
  companies(): Observable<Company[]> {
    return this.http.get<Company[]>(`${this.api}/companies`);
  }
}
