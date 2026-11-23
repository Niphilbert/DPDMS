import { countFor } from './dashboard.service';
import { MyTask, Project, ProjectRequest } from './models';

const request = (status: ProjectRequest['status']) => ({ status }) as ProjectRequest;
const project = (status: Project['status'], openApprovals?: number) => ({ status, openApprovals }) as Project;
const mine = (status: MyTask['task']['status'], late = false) => ({ task: { status, late } }) as MyTask;

describe('countFor', () => {
  const requests = [request('NEW'), request('QUOTED'), request('CHANGES_REQUESTED'), request('ACCEPTED'), request('DECLINED')];
  const projects = [project('IN_PROGRESS', 2), project('PLANNING', 1), project('LAUNCHED', 0)];

  it('counts open requests, active projects and pending approvals for a client', () => {
    const c = countFor('CLIENT', requests, projects, null);
    expect(c.openRequests).toBe(3);
    expect(c.activeProjects).toBe(2);
    expect(c.pendingApprovals).toBe(3);
    expect(c.myTasks).toBeUndefined();
  });

  it('counts requests that need a quote and my open tasks for a project manager', () => {
    const c = countFor('PROJECT_MANAGER', requests, projects, [mine('TODO'), mine('DOING', true), mine('DONE')]);
    expect(c.needQuote).toBe(2);
    expect(c.myTasks).toBe(2);
    expect(c.lateTasks).toBe(1);
  });

  it('gives developers no request or approval numbers', () => {
    const c = countFor('DEVELOPER', null, [project('IN_PROGRESS')], [mine('DOING')]);
    expect(c.openRequests).toBeUndefined();
    expect(c.pendingApprovals).toBeUndefined();
    expect(c.activeProjects).toBe(1);
    expect(c.myTasks).toBe(1);
  });
});
