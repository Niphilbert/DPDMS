import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProjectsService } from './projects.service';

describe('ProjectsService', () => {
  let api: ProjectsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ProjectsService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  const call = (url: string, body?: unknown) => {
    const req = http.expectOne(url);
    if (body !== undefined) expect(req.request.body).toEqual(body);
    req.flush({});
    return req.request;
  };

  it('moves tasks through the documented endpoints', () => {
    api.setStatus('p', 't', 'DOING').subscribe();
    call('/api/projects/p/tasks/t/status', { status: 'DOING' });
    api.submit('p', 't', { file: 'https://x.example/a.zip', note: 'ready' }).subscribe();
    call('/api/projects/p/tasks/t/submit', { file: 'https://x.example/a.zip', note: 'ready' });
    api.sendBack('p', 't', 'fix the footer').subscribe();
    call('/api/projects/p/tasks/t/send-back', { note: 'fix the footer' });
    api.complete('p', 't').subscribe();
    expect(call('/api/projects/p/tasks/t/complete', {}).method).toBe('POST');
  });

  it('assigns, adds tasks and starts a project', () => {
    api.assign('p', 't', { assigneeId: 'u', dueDate: '2026-12-01' }).subscribe();
    call('/api/projects/p/tasks/t/assign', { assigneeId: 'u', dueDate: '2026-12-01' });
    api.addTask('p', { title: 'Pay', phase: 2, plannedHours: 10, dueDate: '2026-12-01' }).subscribe();
    call('/api/projects/p/tasks');
    api.start('p').subscribe();
    call('/api/projects/p/start', {});
  });

  it('logs time and reads the summary', () => {
    api.logTime('p', 't', { date: '2026-11-04', hours: 2.5 }).subscribe();
    call('/api/projects/p/tasks/t/time', { date: '2026-11-04', hours: 2.5 });
    api.timeSummary('p').subscribe();
    expect(call('/api/projects/p/time').method).toBe('GET');
  });

  it('answers approvals and change requests', () => {
    api.approve('p', 'a', '').subscribe();
    call('/api/projects/p/approvals/a/approve', {});
    api.approve('p', 'a', ' Looks great ').subscribe();
    call('/api/projects/p/approvals/a/approve', { note: 'Looks great' });
    api.requestApprovalChanges('p', 'a', 'Bigger logo').subscribe();
    call('/api/projects/p/approvals/a/request-changes', { note: 'Bigger logo' });
    api.answerChange('p', 'c', 'accept').subscribe();
    call('/api/projects/p/change-requests/c/accept', {});
    api.answerChange('p', 'c', 'decline').subscribe();
    call('/api/projects/p/change-requests/c/decline', {});
  });
});
