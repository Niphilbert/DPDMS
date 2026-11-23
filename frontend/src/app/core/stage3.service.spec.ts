import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { toApiError } from './api-error';
import { DashboardService } from './dashboard.service';
import { InvoicesService } from './invoices.service';
import { RenewalsService } from './renewals.service';
import { ReportsService } from './reports.service';
import { TicketsService } from './tickets.service';

function setup() {
  sessionStorage.clear();
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  return TestBed.inject(HttpTestingController);
}

describe('InvoicesService', () => {
  let http: HttpTestingController;
  let api: InvoicesService;
  beforeEach(() => {
    http = setup();
    api = TestBed.inject(InvoicesService);
  });
  afterEach(() => http.verify());

  it('sends only the filters that are set', () => {
    api.list({ status: 'OVERDUE', companyId: 'c1', projectId: '', from: '2026-01-01' }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/invoices');
    expect(req.request.params.get('status')).toBe('OVERDUE');
    expect(req.request.params.get('companyId')).toBe('c1');
    expect(req.request.params.has('projectId')).toBe(false);
    expect(req.request.params.get('from')).toBe('2026-01-01');
    expect(req.request.params.has('to')).toBe(false);
    req.flush([]);
  });

  it('creates and replaces drafts with the documented body', () => {
    const body = { projectId: 'p', issueDate: '2026-11-04', dueDate: '2026-11-18', taxRate: 18, lines: [{ description: 'Design', quantity: 1, unitPrice: 500000 }] };
    api.create(body).subscribe();
    const post = http.expectOne('/api/invoices');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(body);
    post.flush({});
    api.update('i1', body).subscribe();
    const put = http.expectOne('/api/invoices/i1');
    expect(put.request.method).toBe('PUT');
    put.flush({});
  });

  it('sends, records and reverses payments, voids and reminds', () => {
    api.send('i').subscribe();
    expect(http.expectOne('/api/invoices/i/send').request.body).toEqual({});
    api.recordPayment('i', { amount: 400000, paidOn: '2026-11-04', method: 'MOBILE_MONEY', reference: 'MoMo 1' }).subscribe();
    expect(http.expectOne('/api/invoices/i/payments').request.body).toEqual({ amount: 400000, paidOn: '2026-11-04', method: 'MOBILE_MONEY', reference: 'MoMo 1' });
    api.reversePayment('i', 'p', 'Cheque bounced').subscribe();
    expect(http.expectOne('/api/invoices/i/payments/p/reverse').request.body).toEqual({ reason: 'Cheque bounced' });
    api.void('i').subscribe();
    expect(http.expectOne('/api/invoices/i/void').request.body).toEqual({});
    api.void('i', ' Wrong client ').subscribe();
    expect(http.expectOne('/api/invoices/i/void').request.body).toEqual({ reason: 'Wrong client' });
    api.remind('i').subscribe();
    expect(http.expectOne('/api/invoices/i/remind').request.method).toBe('POST');
  });

  it('reads project money, the suggestion, totals, companies and the CSV as a blob', () => {
    api.projectMoney('p').subscribe();
    http.expectOne('/api/projects/p/money');
    api.suggestion('p').subscribe();
    http.expectOne('/api/projects/p/invoice-suggestion');
    api.forProject('p').subscribe();
    http.expectOne('/api/projects/p/invoices');
    api.totals({ companyId: 'c' }).subscribe();
    expect(http.expectOne((r) => r.url === '/api/invoices/summary').request.params.get('companyId')).toBe('c');
    api.companies().subscribe();
    http.expectOne('/api/companies');
    api.exportCsv({ status: 'PAID' }).subscribe();
    const csv = http.expectOne((r) => r.url === '/api/invoices/export.csv');
    expect(csv.request.responseType).toBe('blob');
    expect(csv.request.params.get('status')).toBe('PAID');
  });
});

describe('RenewalsService', () => {
  let http: HttpTestingController;
  let api: RenewalsService;
  beforeEach(() => {
    http = setup();
    api = TestBed.inject(RenewalsService);
  });
  afterEach(() => http.verify());

  it('filters, including expiringWithinDays of 0', () => {
    api.list({ status: 'EXPIRING', type: 'SSL', expiringWithinDays: 0 }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/renewals');
    expect(req.request.params.get('status')).toBe('EXPIRING');
    expect(req.request.params.get('type')).toBe('SSL');
    expect(req.request.params.get('expiringWithinDays')).toBe('0');
    req.flush([]);
    api.list({ expiringWithinDays: null }).subscribe();
    expect(http.expectOne((r) => r.url === '/api/renewals').request.params.has('expiringWithinDays')).toBe(false);
  });

  it('adds, edits, renews, deletes and requests renewals', () => {
    const body = { type: 'DOMAIN' as const, label: 'acme.example', expiresOn: '2027-02-01', price: 25000 };
    api.create({ ...body, projectId: 'p' }).subscribe();
    expect(http.expectOne('/api/renewals').request.method).toBe('POST');
    api.update('r', body).subscribe();
    const put = http.expectOne('/api/renewals/r');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body.projectId).toBeUndefined();
    api.renew('r', { newExpiresOn: '2028-02-01', price: 27000 }).subscribe();
    expect(http.expectOne('/api/renewals/r/renew').request.body).toEqual({ newExpiresOn: '2028-02-01', price: 27000 });
    api.remove('r').subscribe();
    expect(http.expectOne('/api/renewals/r').request.method).toBe('DELETE');
    api.requestRenewal('r').subscribe();
    expect(http.expectOne('/api/renewals/r/request-renewal').request.method).toBe('POST');
  });

  it('runs the reminder job with or without a pretend date', () => {
    api.runReminders().subscribe();
    expect(http.expectOne('/api/admin/renewals/run-reminders').request.body).toEqual({});
    api.runReminders('2026-12-01').subscribe();
    expect(http.expectOne('/api/admin/renewals/run-reminders').request.body).toEqual({ today: '2026-12-01' });
  });
});

describe('TicketsService', () => {
  let http: HttpTestingController;
  let api: TicketsService;
  beforeEach(() => {
    http = setup();
    api = TestBed.inject(TicketsService);
  });
  afterEach(() => http.verify());

  it('sends the list filters as the contract names them', () => {
    api.list({ status: 'OPEN', priority: 'URGENT', unassigned: true, breached: true, mine: false }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/tickets');
    expect(req.request.params.get('status')).toBe('OPEN');
    expect(req.request.params.get('priority')).toBe('URGENT');
    expect(req.request.params.get('unassigned')).toBe('true');
    expect(req.request.params.get('breached')).toBe('true');
    expect(req.request.params.has('mine')).toBe(false);
  });

  it('raises tickets and comments with the internal flag', () => {
    api.create({ subject: 's', description: 'd', priority: 'HIGH' }).subscribe();
    expect(http.expectOne('/api/tickets').request.body).toEqual({ subject: 's', description: 'd', priority: 'HIGH' });
    api.comment('t', 'Hello', true).subscribe();
    expect(http.expectOne('/api/tickets/t/comments').request.body).toEqual({ body: 'Hello', internal: true });
  });

  it('uses the documented action endpoints', () => {
    api.setStatus('t', 'RESOLVED').subscribe();
    expect(http.expectOne('/api/tickets/t/status').request.body).toEqual({ status: 'RESOLVED' });
    api.assign('t', 'u').subscribe();
    expect(http.expectOne('/api/tickets/t/assign').request.body).toEqual({ assigneeId: 'u' });
    api.setPriority('t', 'LOW').subscribe();
    expect(http.expectOne('/api/tickets/t/priority').request.body).toEqual({ priority: 'LOW' });
    api.reopen('t').subscribe();
    expect(http.expectOne('/api/tickets/t/reopen').request.method).toBe('POST');
    api.close('t').subscribe();
    expect(http.expectOne('/api/tickets/t/close').request.method).toBe('POST');
  });

  it('runs the SLA check', () => {
    api.runSlaCheck().subscribe();
    expect(http.expectOne('/api/admin/tickets/run-sla-check').request.body).toEqual({});
    api.runSlaCheck('2026-11-05T10:00:00.000Z').subscribe();
    expect(http.expectOne('/api/admin/tickets/run-sla-check').request.body).toEqual({ now: '2026-11-05T10:00:00.000Z' });
  });
});

describe('ReportsService and the stage 3 dashboard', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    http = setup();
  });
  afterEach(() => http.verify());

  it('passes the date range, the month count and downloads the CSV as a blob', () => {
    const api = TestBed.inject(ReportsService);
    api.summary('2026-01-01', '2026-12-31').subscribe();
    const s = http.expectOne((r) => r.url === '/api/reports/summary');
    expect(s.request.params.get('from')).toBe('2026-01-01');
    expect(s.request.params.get('to')).toBe('2026-12-31');
    api.summary().subscribe();
    expect(http.expectOne((r) => r.url === '/api/reports/summary').request.params.keys()).toEqual([]);
    api.revenueByMonth(6).subscribe();
    expect(http.expectOne((r) => r.url === '/api/reports/revenue-by-month').request.params.get('months')).toBe('6');
    api.summaryCsv('2026-01-01').subscribe();
    const csv = http.expectOne((r) => r.url === '/api/reports/summary.csv');
    expect(csv.request.responseType).toBe('blob');
  });

  it('reads the stage 3 dashboard counts', () => {
    TestBed.inject(DashboardService).stage3().subscribe();
    expect(http.expectOne('/api/dashboard/stage3').request.method).toBe('GET');
  });
});

describe('friendly wording for the stage 3 error codes', () => {
  const codes = [
    'DUE_BEFORE_ISSUE', 'INVALID_RANGE', 'EXPIRY_NOT_LATER', 'INVOICE_NOT_DRAFT', 'INVOICE_NOT_PAYABLE', 'OVERPAYMENT',
    'INVOICE_HAS_PAYMENTS', 'PAYMENT_ALREADY_REVERSED', 'HAS_HISTORY', 'TICKET_CLOSED', 'REOPEN_LIMIT', 'INVALID_TRANSITION', 'INVALID_STATE',
  ];

  it.each(codes)('says what to do next for %s', (code) => {
    const e = toApiError(new HttpErrorResponse({ status: 409, error: { code, message: 'server text' } }));
    expect(e.code).toBe(code);
    expect(e.message).not.toBe('server text');
    expect(e.message.length).toBeGreaterThan(20);
  });

  it('explains an over-payment and a delete conflict', () => {
    expect(toApiError(new HttpErrorResponse({ status: 409, error: { code: 'OVERPAYMENT' } })).message).toContain('more than');
    expect(toApiError(new HttpErrorResponse({ status: 409, error: { code: 'HAS_HISTORY' } })).message).toContain('cannot be deleted');
  });
});
