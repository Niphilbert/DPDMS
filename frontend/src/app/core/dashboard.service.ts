import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { MyTask, Project, ProjectRequest, Role, Stage3Dashboard } from './models';
import { ProjectsService } from './projects.service';
import { RequestsService } from './requests.service';

export interface DashboardCounts {
  /** Requests that are not finished yet (new, quoted or waiting for a new quote). Not for developers. */
  openRequests?: number;
  /** Of those, how many still need a quote from the team. */
  needQuote?: number;
  activeProjects: number;
  /** Tasks assigned to me that are not done. Staff only. */
  myTasks?: number;
  lateTasks?: number;
  /** Items waiting for the client's answer. Not for developers. */
  pendingApprovals?: number;
}

/** Pure counting, kept apart from the HTTP calls so it is easy to test. */
export function countFor(role: Role, requests: ProjectRequest[] | null, projects: Project[] | null, mine: MyTask[] | null): DashboardCounts {
  const active = (projects ?? []).filter((p) => p.status !== 'LAUNCHED');
  const counts: DashboardCounts = { activeProjects: active.length };
  if (role !== 'DEVELOPER') {
    const open = (requests ?? []).filter((r) => r.status === 'NEW' || r.status === 'QUOTED' || r.status === 'CHANGES_REQUESTED');
    counts.openRequests = open.length;
    counts.needQuote = open.filter((r) => r.status === 'NEW' || r.status === 'CHANGES_REQUESTED').length;
    counts.pendingApprovals = (projects ?? []).reduce((sum, p) => sum + (p.openApprovals ?? 0), 0);
  }
  if (role !== 'CLIENT') {
    const open = (mine ?? []).filter((m) => m.task.status !== 'DONE');
    counts.myTasks = open.length;
    counts.lateTasks = open.filter((m) => m.task.late).length;
  }
  return counts;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly requests = inject(RequestsService);
  private readonly projects = inject(ProjectsService);

  /** The money and care counts (`GET /api/dashboard/stage3`). Blocks the role may not see are missing from the answer. */
  stage3(): Observable<Stage3Dashboard> {
    return this.http.get<Stage3Dashboard>(`${environment.apiUrl}/dashboard/stage3`);
  }

  /** Loads only what the role may read; a call that fails simply counts as nothing. */
  counts(role: Role): Observable<DashboardCounts> {
    const none = <T>() => catchError<T, Observable<null>>(() => of(null));
    return forkJoin({
      requests: role === 'DEVELOPER' ? of(null) : this.requests.list().pipe(none()),
      projects: this.projects.list().pipe(none()),
      mine: role === 'CLIENT' ? of(null) : this.projects.mine().pipe(none()),
    }).pipe(map((r) => countFor(role, r.requests, r.projects, r.mine)));
  }
}
