import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { NewTicket, SlaRun, Ticket, TicketFilter, TicketPriority, TicketStatus, TicketSummary } from './models';

export function ticketParams(f: TicketFilter): HttpParams {
  let params = new HttpParams();
  if (f.status) params = params.set('status', f.status);
  if (f.priority) params = params.set('priority', f.priority);
  if (f.projectId) params = params.set('projectId', f.projectId);
  if (f.companyId) params = params.set('companyId', f.companyId);
  if (f.assigneeId) params = params.set('assigneeId', f.assigneeId);
  if (f.unassigned) params = params.set('unassigned', true);
  if (f.breached) params = params.set('breached', true);
  if (f.mine) params = params.set('mine', true);
  return params;
}

/** Support tickets, comments and the SLA check (docs/api-stage3.md section 3). */
@Injectable({ providedIn: 'root' })
export class TicketsService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  list(filter: TicketFilter = {}): Observable<TicketSummary[]> {
    return this.http.get<TicketSummary[]>(`${this.api}/tickets`, { params: ticketParams(filter) });
  }

  get(id: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.api}/tickets/${id}`);
  }

  create(body: NewTicket): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.api}/tickets`, body);
  }

  comment(id: string, body: string, internal: boolean): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.api}/tickets/${id}/comments`, { body, internal });
  }

  setStatus(id: string, status: TicketStatus): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.api}/tickets/${id}/status`, { status });
  }

  assign(id: string, assigneeId: string): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.api}/tickets/${id}/assign`, { assigneeId });
  }

  setPriority(id: string, priority: TicketPriority): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.api}/tickets/${id}/priority`, { priority });
  }

  reopen(id: string): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.api}/tickets/${id}/reopen`, {});
  }

  close(id: string): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.api}/tickets/${id}/close`, {});
  }

  /** Administrator only. `now` (an instant) pretends it is that moment. */
  runSlaCheck(now?: string): Observable<SlaRun> {
    return this.http.post<SlaRun>(`${this.api}/admin/tickets/run-sla-check`, now ? { now } : {});
  }
}
