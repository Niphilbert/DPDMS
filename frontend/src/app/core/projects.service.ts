import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Approval,
  ChangeRequest,
  MyTask,
  NewApproval,
  NewTask,
  Project,
  ProjectUpdate,
  Task,
  TaskStatus,
  TimeEntry,
  TimeLogged,
  TimeSummary,
} from './models';

/** Projects, tasks, time, updates, approvals and change requests (docs/api.md sections 5 to 10). */
@Injectable({ providedIn: 'root' })
export class ProjectsService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // ---- projects
  list(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.api}/projects`);
  }

  get(id: string): Observable<Project> {
    return this.http.get<Project>(`${this.api}/projects/${id}`);
  }

  start(id: string): Observable<Project> {
    return this.http.post<Project>(`${this.api}/projects/${id}/start`, {});
  }

  // ---- tasks
  mine(): Observable<MyTask[]> {
    return this.http.get<MyTask[]>(`${this.api}/tasks/mine`);
  }

  addTask(projectId: string, body: NewTask): Observable<Task> {
    return this.http.post<Task>(`${this.api}/projects/${projectId}/tasks`, body);
  }

  assign(projectId: string, taskId: string, body: { assigneeId: string; dueDate: string; note?: string }): Observable<Task> {
    return this.http.post<Task>(`${this.api}/projects/${projectId}/tasks/${taskId}/assign`, body);
  }

  setStatus(projectId: string, taskId: string, status: TaskStatus): Observable<Task> {
    return this.http.post<Task>(`${this.api}/projects/${projectId}/tasks/${taskId}/status`, { status });
  }

  submit(projectId: string, taskId: string, body: { file: string; note?: string }): Observable<Task> {
    return this.http.post<Task>(`${this.api}/projects/${projectId}/tasks/${taskId}/submit`, body);
  }

  sendBack(projectId: string, taskId: string, note: string): Observable<Task> {
    return this.http.post<Task>(`${this.api}/projects/${projectId}/tasks/${taskId}/send-back`, { note });
  }

  complete(projectId: string, taskId: string): Observable<Task> {
    return this.http.post<Task>(`${this.api}/projects/${projectId}/tasks/${taskId}/complete`, {});
  }

  // ---- time
  logTime(projectId: string, taskId: string, body: { date: string; hours: number; note?: string }): Observable<TimeLogged> {
    return this.http.post<TimeLogged>(`${this.api}/projects/${projectId}/tasks/${taskId}/time`, body);
  }

  taskTime(projectId: string, taskId: string): Observable<TimeEntry[]> {
    return this.http.get<TimeEntry[]>(`${this.api}/projects/${projectId}/tasks/${taskId}/time`);
  }

  timeSummary(projectId: string): Observable<TimeSummary> {
    return this.http.get<TimeSummary>(`${this.api}/projects/${projectId}/time`);
  }

  // ---- updates feed
  updates(projectId: string): Observable<ProjectUpdate[]> {
    return this.http.get<ProjectUpdate[]>(`${this.api}/projects/${projectId}/updates`);
  }

  // ---- approvals
  approvals(projectId: string): Observable<Approval[]> {
    return this.http.get<Approval[]>(`${this.api}/projects/${projectId}/approvals`);
  }

  sendApproval(projectId: string, body: NewApproval): Observable<Approval> {
    return this.http.post<Approval>(`${this.api}/projects/${projectId}/approvals`, body);
  }

  approve(projectId: string, approvalId: string, note: string): Observable<Approval> {
    return this.http.post<Approval>(`${this.api}/projects/${projectId}/approvals/${approvalId}/approve`, note.trim() ? { note: note.trim() } : {});
  }

  requestApprovalChanges(projectId: string, approvalId: string, note: string): Observable<Approval> {
    return this.http.post<Approval>(`${this.api}/projects/${projectId}/approvals/${approvalId}/request-changes`, { note });
  }

  // ---- change requests
  changes(projectId: string): Observable<ChangeRequest[]> {
    return this.http.get<ChangeRequest[]>(`${this.api}/projects/${projectId}/change-requests`);
  }

  proposeChange(projectId: string, body: { title: string; cost: number }): Observable<ChangeRequest> {
    return this.http.post<ChangeRequest>(`${this.api}/projects/${projectId}/change-requests`, body);
  }

  answerChange(projectId: string, changeId: string, answer: 'accept' | 'decline'): Observable<ChangeRequest> {
    return this.http.post<ChangeRequest>(`${this.api}/projects/${projectId}/change-requests/${changeId}/${answer}`, {});
  }
}
