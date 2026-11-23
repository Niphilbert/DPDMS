import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Project, Role, Task, User } from '../../core/models';
import { SessionService } from '../../core/session.service';
import { ProjectTasksComponent } from './project-tasks.component';

const person = (role: Role): User => ({
  id: 'me', email: 'a@b.c', fullName: 'Me', role, roleLabel: role, status: 'ACTIVE', companyId: null, companyName: null,
  selfRegistered: false, lastLoginAt: null, createdAt: '',
});
const task = (id: string, status: Task['status'], assigneeId?: string): Task => ({
  id, phase: 2, title: `Task ${id}`, status, late: false, dueDate: '2030-01-01', plannedHours: 10, assigneeId, assigneeName: assigneeId ? 'Someone' : undefined,
});
const project = (tasks: Task[], status: Project['status'] = 'IN_PROGRESS'): Project => ({
  id: 'p', name: 'Site', status, companyId: 'c', companyName: 'Acme', managerId: 'm', managerName: 'Jane', startDate: null, dueDate: null,
  progress: 0, phases: [], lateTasks: 0, tasks, createdAt: '',
});

function render(role: Role, p: Project) {
  sessionStorage.clear();
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  TestBed.inject(SessionService).start('t', person(role));
  const fixture = TestBed.createComponent(ProjectTasksComponent);
  fixture.componentRef.setInput('project', p);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

const buttons = (el: HTMLElement) => [...el.querySelectorAll('button')].map((b) => b.textContent!.trim());

describe('ProjectTasksComponent', () => {
  it('lets a developer act only on their own tasks', () => {
    const el = render('DEVELOPER', project([task('1', 'TODO', 'me'), task('2', 'TODO', 'other')]));
    const cards = [...el.querySelectorAll('article.task')];
    expect(cards[0].textContent).toContain('Start');
    expect(cards[1].textContent).not.toContain('Start');
    expect(buttons(el)).not.toContain('Add a task');
    expect(buttons(el)).not.toContain('Assign');
    expect(el.textContent).not.toContain('Mark done');
  });

  it('shows a client a plain list without people, hours or buttons', () => {
    const el = render('CLIENT', project([task('1', 'DOING', 'someone')]));
    expect(el.querySelector('article.task')).toBeNull();
    expect(el.querySelector('table')).not.toBeNull();
    expect(el.textContent).not.toContain('Someone');
    expect(el.querySelectorAll('button')).toHaveLength(0);
  });

  it('gives a manager review actions and assignment', () => {
    const el = render('PROJECT_MANAGER', project([task('1', 'REVIEW', 'other'), task('2', 'TODO')]));
    expect(buttons(el)).toEqual(expect.arrayContaining(['Add a task', 'Mark done', 'Send back', 'Reassign', 'Assign']));
  });

  it('hides moving and submitting until the project has started', () => {
    const el = render('DEVELOPER', project([task('1', 'TODO', 'me')], 'PLANNING'));
    expect(buttons(el)).not.toContain('Start');
    expect(buttons(el)).not.toContain('Log time');
  });
});
