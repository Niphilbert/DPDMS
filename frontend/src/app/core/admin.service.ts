import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Access, AuditEntry, Role, StaffCreated, StaffRole, User } from './models';

/** Administrator-only calls: accounts and the audit trail. */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly users = `${environment.apiUrl}/admin/users`;

  listUsers(q: string, role: Role | ''): Observable<User[]> {
    let params = new HttpParams();
    if (q.trim()) params = params.set('q', q.trim());
    if (role) params = params.set('role', role);
    return this.http.get<User[]>(this.users, { params });
  }

  createStaff(body: { fullName: string; email: string; role: StaffRole }): Observable<StaffCreated> {
    return this.http.post<StaffCreated>(this.users, body);
  }

  changeRole(id: string, role: StaffRole): Observable<User> {
    return this.http.patch<User>(`${this.users}/${id}/role`, { role });
  }

  deactivate(id: string): Observable<User> {
    return this.http.post<User>(`${this.users}/${id}/deactivate`, {});
  }

  reactivate(id: string): Observable<User> {
    return this.http.post<User>(`${this.users}/${id}/reactivate`, {});
  }

  resetPassword(id: string): Observable<StaffCreated> {
    return this.http.post<StaffCreated>(`${this.users}/${id}/reset-password`, {});
  }

  audit(q: string, limit = 200): Observable<AuditEntry[]> {
    let params = new HttpParams().set('limit', limit);
    if (q.trim()) params = params.set('q', q.trim());
    return this.http.get<AuditEntry[]>(`${environment.apiUrl}/admin/audit`, { params });
  }

  /** Developers the administrator gave extra access to a project. */
  projectAccess(projectId: string): Observable<Access[]> {
    return this.http.get<Access[]>(`${environment.apiUrl}/admin/projects/${projectId}/access`);
  }

  grantAccess(projectId: string, userId: string): Observable<Access[]> {
    return this.http.post<Access[]>(`${environment.apiUrl}/admin/projects/${projectId}/access`, { userId });
  }

  revokeAccess(projectId: string, userId: string): Observable<Access[]> {
    return this.http.delete<Access[]>(`${environment.apiUrl}/admin/projects/${projectId}/access/${userId}`);
  }
}
