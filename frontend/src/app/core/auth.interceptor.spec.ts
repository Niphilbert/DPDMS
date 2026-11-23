import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { User } from './models';
import { SessionService } from './session.service';

const user: User = {
  id: 'u1', email: 'a@b.c', fullName: 'A B', role: 'DEVELOPER', roleLabel: 'Developer', status: 'ACTIVE',
  companyId: null, companyName: null, selfRegistered: false, lastLoginAt: null, createdAt: '2026-01-01T00:00:00Z',
};

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let session: SessionService;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionService);
    router = TestBed.inject(Router);
  });

  afterEach(() => ctrl.verify());

  it('adds the Bearer token to API calls', () => {
    session.start('tok', user);
    http.get('/api/notifications').subscribe();
    expect(ctrl.expectOne('/api/notifications').request.headers.get('Authorization')).toBe('Bearer tok');
  });

  it('does not add the token to sign-in calls', () => {
    session.start('tok', user);
    http.post('/api/auth/login', {}).subscribe();
    expect(ctrl.expectOne('/api/auth/login').request.headers.has('Authorization')).toBe(false);
  });

  it('does not add the token to other hosts', () => {
    session.start('tok', user);
    http.get('https://example.com/data').subscribe();
    expect(ctrl.expectOne('https://example.com/data').request.headers.has('Authorization')).toBe(false);
  });

  it('clears the session and goes to /login on a 401', () => {
    const nav = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    session.start('tok', user);
    http.get('/api/notifications').subscribe({ error: () => undefined });
    ctrl.expectOne('/api/notifications').flush({ code: 'UNAUTHORIZED' }, { status: 401, statusText: 'Unauthorized' });
    expect(session.signedIn()).toBe(false);
    expect(nav).toHaveBeenCalledWith('/login');
  });

  it('leaves a wrong-password 401 alone', () => {
    const nav = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    let status = 0;
    http.post('/api/auth/login', {}).subscribe({ error: (e) => (status = e.status) });
    ctrl.expectOne('/api/auth/login').flush({ code: 'BAD_CREDENTIALS' }, { status: 401, statusText: 'Unauthorized' });
    expect(status).toBe(401);
    expect(nav).not.toHaveBeenCalled();
  });
});
