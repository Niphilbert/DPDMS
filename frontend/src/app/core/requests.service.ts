import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { NewRequest, ProjectRequest, QuoteInput, RequestStatus } from './models';

/** Client requests and the quote that answers them (docs/api.md section 4). */
@Injectable({ providedIn: 'root' })
export class RequestsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/requests`;

  list(status?: RequestStatus | ''): Observable<ProjectRequest[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<ProjectRequest[]>(this.base, { params });
  }

  get(id: string): Observable<ProjectRequest> {
    return this.http.get<ProjectRequest>(`${this.base}/${id}`);
  }

  create(body: NewRequest): Observable<ProjectRequest> {
    const payload: Partial<NewRequest> = { title: body.title, description: body.description, deadline: body.deadline };
    if (body.budget.trim()) payload.budget = body.budget.trim();
    return this.http.post<ProjectRequest>(this.base, payload);
  }

  /** Writes the first quote or revises the existing one (management). */
  quote(id: string, body: QuoteInput): Observable<ProjectRequest> {
    return this.http.post<ProjectRequest>(`${this.base}/${id}/quote`, body);
  }

  accept(id: string): Observable<ProjectRequest> {
    return this.http.post<ProjectRequest>(`${this.base}/${id}/accept`, {});
  }

  requestChanges(id: string, note: string): Observable<ProjectRequest> {
    return this.http.post<ProjectRequest>(`${this.base}/${id}/request-changes`, { note });
  }

  decline(id: string, reason: string): Observable<ProjectRequest> {
    return this.http.post<ProjectRequest>(`${this.base}/${id}/decline`, reason.trim() ? { reason: reason.trim() } : {});
  }
}
