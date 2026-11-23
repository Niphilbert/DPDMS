import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { User } from './models';
import { SessionService } from './session.service';

const admin: User = {
  id: 'u1', email: 'admin@itzetech.local', fullName: 'Admin User', role: 'ADMIN', roleLabel: 'Administrator', status: 'ACTIVE',
  companyId: null, companyName: null, selfRegistered: false, lastLoginAt: null, createdAt: '2026-01-01T00:00:00Z',
};

describe('AuthService', () => {
  let auth: AuthService;
  let session: SessionService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    auth = TestBed.inject(AuthService);
    session = TestBed.inject(SessionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts a session when sign-in is finished', () => {
    auth.login('admin@itzetech.local', 'pw').subscribe();
    const req = http.expectOne('/api/auth/login');
    expect(req.request.body).toEqual({ email: 'admin@itzetech.local', password: 'pw' });
    req.flush({ status: 'AUTHENTICATED', token: 'tok', user: admin });

    expect(session.signedIn()).toBe(true);
    expect(session.token()).toBe('tok');
    expect(session.user()?.role).toBe('ADMIN');
    expect(JSON.parse(sessionStorage.getItem('dpdms.session')!).token).toBe('tok');
  });

  it('does not start a session while a code is still needed', () => {
    auth.login('a@b.c', 'pw').subscribe();
    http.expectOne('/api/auth/login').flush({ status: 'CODE_REQUIRED', challenge: 'ch', devCode: '123456' });
    expect(session.signedIn()).toBe(false);
  });

  it('remembers the e-mail when confirmation is required', () => {
    auth.register({ companyName: 'Acme', fullName: 'Jean', email: 'jean@acme.example', phone: '', password: 'Sunrise2026', acceptTerms: true }).subscribe();
    http.expectOne('/api/auth/register').flush({ status: 'VERIFICATION_REQUIRED', email: 'jean@acme.example', devCode: '654321' });
    expect(auth.pending()).toEqual({ email: 'jean@acme.example', devCode: '654321' });
    expect(session.signedIn()).toBe(false);
  });

  it('clears the session on logout', () => {
    const nav = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    session.start('tok', admin);
    auth.logout();
    expect(nav).toHaveBeenCalledWith('/login');
    expect(session.signedIn()).toBe(false);
    expect(sessionStorage.getItem('dpdms.session')).toBeNull();
  });

  it('sends the challenge and the new password when changing a temporary password', () => {
    auth.changePassword('ch', 'NewPass2026').subscribe();
    const req = http.expectOne('/api/auth/change-password');
    expect(req.request.body).toEqual({ challenge: 'ch', newPassword: 'NewPass2026' });
    req.flush({ status: 'CODE_REQUIRED', challenge: 'ch2' });
  });
});
