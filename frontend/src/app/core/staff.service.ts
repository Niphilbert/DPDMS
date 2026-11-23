import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { Role } from './models';
import { ROLE_LABEL } from './nav';

export interface Person {
  id: string;
  name: string;
  role?: Role;
}

/** One row of `GET /api/staff` (active staff only; readable by administrators and project managers). */
interface StaffDto {
  id: string;
  fullName: string;
  email: string;
  role: Role;
}

/** Who can be picked in the assign and add-task dialogs, and who can be given project access. */
@Injectable({ providedIn: 'root' })
export class StaffService {
  private readonly http = inject(HttpClient);

  /** Active developers and project managers: the people a task can be given to. */
  assignable(): Observable<Person[]> {
    return this.list().pipe(map((list) => list.filter((p) => p.role === 'DEVELOPER' || p.role === 'PROJECT_MANAGER')));
  }

  /** Active developers an administrator may give project access to. */
  developers(): Observable<Person[]> {
    return this.list('DEVELOPER');
  }

  private list(role?: Role): Observable<Person[]> {
    let params = new HttpParams();
    if (role) params = params.set('role', role);
    return this.http.get<StaffDto[]>(`${environment.apiUrl}/staff`, { params }).pipe(
      map((rows) => rows.map((r) => ({ id: r.id, name: `${r.fullName} (${ROLE_LABEL[r.role]})`, role: r.role }))),
    );
  }
}
