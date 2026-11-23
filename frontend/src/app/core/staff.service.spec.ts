import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { StaffService } from './staff.service';

const rows = [
  { id: 'a', fullName: 'Dev A', email: 'a@x.example', role: 'DEVELOPER' },
  { id: 'd', fullName: 'Jane', email: 'j@x.example', role: 'PROJECT_MANAGER' },
  { id: 'z', fullName: 'Root', email: 'r@x.example', role: 'ADMIN' },
];

describe('StaffService', () => {
  let staff: StaffService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    staff = TestBed.inject(StaffService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('offers every active developer and project manager (but not administrators) for assignment', () => {
    let people: { id: string; name: string }[] = [];
    staff.assignable().subscribe((p) => (people = p));
    const req = http.expectOne((r) => r.url === '/api/staff');
    expect(req.request.params.has('role')).toBe(false);
    req.flush(rows);
    expect(people.map((p) => p.id)).toEqual(['a', 'd']);
    expect(people[0].name).toBe('Dev A (Developer)');
    expect(people[1].name).toBe('Jane (Project manager)');
  });

  it('asks only for developers when choosing who gets project access', () => {
    let ids: string[] = [];
    staff.developers().subscribe((p) => (ids = p.map((x) => x.id)));
    const req = http.expectOne((r) => r.url === '/api/staff');
    expect(req.request.params.get('role')).toBe('DEVELOPER');
    req.flush([rows[0]]);
    expect(ids).toEqual(['a']);
  });

  it('passes a 403 on to the caller', () => {
    let status = 0;
    staff.assignable().subscribe({ error: (e) => (status = e.status) });
    http.expectOne((r) => r.url === '/api/staff').flush({ code: 'FORBIDDEN' }, { status: 403, statusText: 'Forbidden' });
    expect(status).toBe(403);
  });
});
