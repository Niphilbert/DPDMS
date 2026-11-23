import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { MonthRevenue, ReportSummary } from './models';

export function rangeParams(from?: string, to?: string): HttpParams {
  let params = new HttpParams();
  if (from) params = params.set('from', from);
  if (to) params = params.set('to', to);
  return params;
}

/** Management reports (docs/api-stage3.md section 4). */
@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  summary(from?: string, to?: string): Observable<ReportSummary> {
    return this.http.get<ReportSummary>(`${this.api}/reports/summary`, { params: rangeParams(from, to) });
  }

  revenueByMonth(months = 12): Observable<MonthRevenue[]> {
    return this.http.get<MonthRevenue[]>(`${this.api}/reports/revenue-by-month`, { params: new HttpParams().set('months', months) });
  }

  summaryCsv(from?: string, to?: string): Observable<Blob> {
    return this.http.get(`${this.api}/reports/summary.csv`, { params: rangeParams(from, to), responseType: 'blob' });
  }
}
