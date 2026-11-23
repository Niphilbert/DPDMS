import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RequestsService } from './requests.service';

describe('RequestsService', () => {
  let api: RequestsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(RequestsService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('lists with an optional status filter', () => {
    api.list('QUOTED').subscribe();
    const req = http.expectOne((r) => r.url === '/api/requests');
    expect(req.request.params.get('status')).toBe('QUOTED');
    req.flush([]);
    api.list('').subscribe();
    expect(http.expectOne((r) => r.url === '/api/requests').request.params.has('status')).toBe(false);
  });

  it('leaves the budget out when the client did not give one', () => {
    api.create({ title: 'Site', description: 'Need a site', budget: '  ', deadline: '2027-01-15' }).subscribe();
    const req = http.expectOne('/api/requests');
    expect(req.request.body).toEqual({ title: 'Site', description: 'Need a site', deadline: '2027-01-15' });
    req.flush({});
  });

  it('posts the quote lines, weeks and validity', () => {
    api.quote('r1', { items: [{ description: 'Build', amount: 1000 }], weeks: 8, validUntil: '2026-12-01' }).subscribe();
    const req = http.expectOne('/api/requests/r1/quote');
    expect(req.request.body.weeks).toBe(8);
    expect(req.request.body.items).toHaveLength(1);
    req.flush({});
  });

  it('answers a quote: accept, ask for changes, decline', () => {
    api.accept('r1').subscribe();
    expect(http.expectOne('/api/requests/r1/accept').request.method).toBe('POST');
    api.requestChanges('r1', 'Add a mobile app').subscribe();
    expect(http.expectOne('/api/requests/r1/request-changes').request.body).toEqual({ note: 'Add a mobile app' });
    api.decline('r1', '').subscribe();
    expect(http.expectOne('/api/requests/r1/decline').request.body).toEqual({});
    api.decline('r1', ' Too dear ').subscribe();
    expect(http.expectOne('/api/requests/r1/decline').request.body).toEqual({ reason: 'Too dear' });
  });
});
