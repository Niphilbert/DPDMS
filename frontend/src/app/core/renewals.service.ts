import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ReminderRun, Renewal, RenewalFilter, RenewalInput } from './models';

export function renewalParams(f: RenewalFilter): HttpParams {
  let params = new HttpParams();
  if (f.status) params = params.set('status', f.status);
  if (f.type) params = params.set('type', f.type);
  if (f.companyId) params = params.set('companyId', f.companyId);
  if (f.projectId) params = params.set('projectId', f.projectId);
  if (f.expiringWithinDays !== null && f.expiringWithinDays !== undefined && !Number.isNaN(f.expiringWithinDays)) {
    params = params.set('expiringWithinDays', f.expiringWithinDays);
  }
  return params;
}

/** Renewals and the reminder job (docs/api-stage3.md section 2). */
@Injectable({ providedIn: 'root' })
export class RenewalsService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  list(filter: RenewalFilter = {}): Observable<Renewal[]> {
    return this.http.get<Renewal[]>(`${this.api}/renewals`, { params: renewalParams(filter) });
  }

  get(id: string): Observable<Renewal> {
    return this.http.get<Renewal>(`${this.api}/renewals/${id}`);
  }

  forProject(projectId: string): Observable<Renewal[]> {
    return this.http.get<Renewal[]>(`${this.api}/projects/${projectId}/renewals`);
  }

  create(body: RenewalInput): Observable<Renewal> {
    return this.http.post<Renewal>(`${this.api}/renewals`, body);
  }

  /** The project cannot change, so it is not sent. */
  update(id: string, body: Omit<RenewalInput, 'projectId'>): Observable<Renewal> {
    return this.http.put<Renewal>(`${this.api}/renewals/${id}`, body);
  }

  renew(id: string, body: { newExpiresOn: string; price?: number; note?: string }): Observable<Renewal> {
    return this.http.post<Renewal>(`${this.api}/renewals/${id}/renew`, body);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/renewals/${id}`);
  }

  /** A client asks the team to renew this item (202, no body). */
  requestRenewal(id: string): Observable<void> {
    return this.http.post<void>(`${this.api}/renewals/${id}/request-renewal`, {});
  }

  /** Administrator only. `today` pretends it is that day. */
  runReminders(today?: string): Observable<ReminderRun> {
    return this.http.post<ReminderRun>(`${this.api}/admin/renewals/run-reminders`, today ? { today } : {});
  }
}
