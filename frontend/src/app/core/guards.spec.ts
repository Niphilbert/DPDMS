import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, UrlTree, provideRouter } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './guards';
import { Role, User } from './models';
import { SessionService } from './session.service';

function userWith(role: Role): User {
  return {
    id: 'u1', email: 'a@b.c', fullName: 'A B', role, roleLabel: role, status: 'ACTIVE',
    companyId: null, companyName: null, selfRegistered: false, lastLoginAt: null, createdAt: '2026-01-01T00:00:00Z',
  };
}

describe('route guards', () => {
  let session: SessionService;
  let router: Router;

  const run = (guard: typeof authGuard, roles: Role[] = []) =>
    TestBed.runInInjectionContext(() => guard({ data: { roles } } as unknown as ActivatedRouteSnapshot, {} as never));

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    session = TestBed.inject(SessionService);
    router = TestBed.inject(Router);
  });

  it('authGuard sends signed-out people to /login', () => {
    const result = run(authGuard) as UrlTree;
    expect(router.serializeUrl(result)).toBe('/login');
  });

  it('authGuard lets signed-in people through', () => {
    session.start('tok', userWith('CLIENT'));
    expect(run(authGuard)).toBe(true);
  });

  it('guestGuard sends signed-in people to the dashboard', () => {
    session.start('tok', userWith('CLIENT'));
    expect(router.serializeUrl(run(guestGuard) as UrlTree)).toBe('/dashboard');
  });

  it('guestGuard lets signed-out people see the sign-in page', () => {
    expect(run(guestGuard)).toBe(true);
  });

  it('roleGuard allows an administrator on an admin route', () => {
    session.start('tok', userWith('ADMIN'));
    expect(run(roleGuard, ['ADMIN'])).toBe(true);
  });

  it.each<Role>(['PROJECT_MANAGER', 'DEVELOPER', 'CLIENT'])('roleGuard turns away a %s from an admin route', (role) => {
    session.start('tok', userWith(role));
    expect(router.serializeUrl(run(roleGuard, ['ADMIN']) as UrlTree)).toBe('/dashboard');
  });
});

describe('role guard on phase 2 routes', () => {
  let session: SessionService;
  let router: Router;
  const asRoute = (roles: Role[]) => TestBed.runInInjectionContext(() => roleGuard({ data: { roles } } as unknown as ActivatedRouteSnapshot, {} as never));

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    session = TestBed.inject(SessionService);
    router = TestBed.inject(Router);
  });

  it('keeps developers out of requests and quotes', () => {
    session.start('t', userWith('DEVELOPER'));
    expect(router.serializeUrl(asRoute(['ADMIN', 'PROJECT_MANAGER', 'CLIENT']) as UrlTree)).toBe('/dashboard');
  });

  it('keeps clients out of my tasks but lets project managers in', () => {
    session.start('t', userWith('CLIENT'));
    expect(asRoute(['ADMIN', 'PROJECT_MANAGER', 'DEVELOPER'])).not.toBe(true);
    session.start('t', userWith('PROJECT_MANAGER'));
    expect(asRoute(['ADMIN', 'PROJECT_MANAGER', 'DEVELOPER'])).toBe(true);
  });
});
