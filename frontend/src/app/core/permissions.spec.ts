import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, UrlTree, provideRouter } from '@angular/router';
import { roleGuard } from './guards';
import { InvoiceSummary, Role, TicketStatus, TicketSummary, User } from './models';
import { NAV } from './nav';
import {
  MONEY_ROLES,
  REPORT_ROLES,
  canManageMoney,
  canRaiseTicket,
  canRunJobs,
  canSeeMoney,
  canSeeReports,
  canWriteInternalNote,
  invoiceActions,
  moveLabel,
  staffTicketMoves,
  ticketActions,
} from './permissions';
import { SessionService } from './session.service';

const inv = (over: Partial<InvoiceSummary>) => ({ status: 'SENT', balance: 100, paidAmount: 0, ...over }) as InvoiceSummary;
const ticket = (status: TicketStatus, over: Partial<TicketSummary> = {}) => ({ status, reopenCount: 0, assigneeId: null, ...over }) as TicketSummary;

describe('role permissions', () => {
  it('keeps money away from developers only', () => {
    expect(canSeeMoney('DEVELOPER')).toBe(false);
    for (const r of ['ADMIN', 'PROJECT_MANAGER', 'CLIENT'] as Role[]) expect(canSeeMoney(r)).toBe(true);
    expect(canManageMoney('CLIENT')).toBe(false);
    expect(canManageMoney('PROJECT_MANAGER')).toBe(true);
  });

  it('limits reports to management, jobs to the administrator and ticket raising to clients', () => {
    expect(canSeeReports('CLIENT')).toBe(false);
    expect(canSeeReports('DEVELOPER')).toBe(false);
    expect(canSeeReports('ADMIN')).toBe(true);
    expect(canRunJobs('PROJECT_MANAGER')).toBe(false);
    expect(canRunJobs('ADMIN')).toBe(true);
    expect(canRaiseTicket('CLIENT')).toBe(true);
    expect(canRaiseTicket('ADMIN')).toBe(false);
  });

  it('offers internal notes to staff and never to clients', () => {
    expect(canWriteInternalNote('CLIENT')).toBe(false);
    expect(canWriteInternalNote('DEVELOPER')).toBe(true);
    expect(canWriteInternalNote(undefined)).toBe(false);
  });
});

describe('menu', () => {
  const labels = (r: Role) => NAV[r].map((i) => i.label);

  it('has no Invoices and no Reports for developers', () => {
    expect(labels('DEVELOPER')).not.toContain('Invoices');
    expect(labels('DEVELOPER')).not.toContain('Reports');
    expect(NAV.DEVELOPER.map((i) => i.path)).not.toContain('/invoices');
  });

  it('gives clients invoices but no reports, and management both', () => {
    expect(labels('CLIENT')).toContain('Invoices');
    expect(labels('CLIENT')).not.toContain('Reports');
    expect(labels('ADMIN')).toEqual(expect.arrayContaining(['Invoices', 'Renewals', 'Support tickets', 'Reports']));
    expect(labels('PROJECT_MANAGER')).toEqual(expect.arrayContaining(['Invoices', 'Renewals', 'Support tickets', 'Reports']));
  });

  it('every role can reach renewals and tickets', () => {
    for (const r of ['ADMIN', 'PROJECT_MANAGER', 'DEVELOPER', 'CLIENT'] as Role[]) {
      expect(NAV[r].map((i) => i.path)).toEqual(expect.arrayContaining(['/renewals', '/tickets']));
    }
  });
});

describe('invoiceActions', () => {
  it('lets management edit and send a draft but not pay it', () => {
    const a = invoiceActions(inv({ status: 'DRAFT', balance: 0 }), 'PROJECT_MANAGER');
    expect(a).toMatchObject({ edit: true, send: true, pay: false, remind: false, void: true });
  });

  it('lets management record a payment and remind on a sent or part-paid invoice, not edit it', () => {
    for (const status of ['SENT', 'PARTIALLY_PAID'] as const) {
      const a = invoiceActions(inv({ status, paidAmount: status === 'SENT' ? 0 : 40 }), 'ADMIN');
      expect(a).toMatchObject({ edit: false, send: false, pay: true, remind: true });
    }
  });

  it('cannot void an invoice that has an active payment, but can reverse payments', () => {
    const a = invoiceActions(inv({ status: 'PARTIALLY_PAID', paidAmount: 50 }), 'ADMIN');
    expect(a.void).toBe(false);
    expect(a.reversePayments).toBe(true);
  });

  it('offers nothing on a paid or void invoice', () => {
    const paid = invoiceActions(inv({ status: 'PAID', balance: 0, paidAmount: 100 }), 'ADMIN');
    expect(paid).toMatchObject({ pay: false, remind: false, send: false, edit: false, void: false });
    const voided = invoiceActions(inv({ status: 'VOID', balance: 0 }), 'ADMIN');
    expect(voided).toMatchObject({ void: false, reversePayments: false, pay: false });
  });

  it('gives clients and developers no actions at all', () => {
    for (const role of ['CLIENT', 'DEVELOPER'] as Role[]) {
      expect(Object.values(invoiceActions(inv({ status: 'DRAFT' }), role)).every((v) => v === false)).toBe(true);
    }
  });
});

describe('ticket status moves', () => {
  it('follows the transition table for management', () => {
    expect(staffTicketMoves('OPEN', 'ADMIN')).toEqual(['IN_PROGRESS', 'WAITING_CLIENT', 'RESOLVED']);
    expect(staffTicketMoves('IN_PROGRESS', 'PROJECT_MANAGER')).toEqual(['WAITING_CLIENT', 'RESOLVED']);
    expect(staffTicketMoves('WAITING_CLIENT', 'ADMIN')).toEqual(['IN_PROGRESS', 'RESOLVED']);
    expect(staffTicketMoves('RESOLVED', 'ADMIN')).toEqual(['IN_PROGRESS', 'CLOSED']);
    expect(staffTicketMoves('CLOSED', 'ADMIN')).toEqual([]);
  });

  it('does not let a developer close a resolved ticket (management only)', () => {
    expect(staffTicketMoves('RESOLVED', 'DEVELOPER')).toEqual(['IN_PROGRESS']);
  });

  it('gives clients no staff moves', () => {
    expect(staffTicketMoves('OPEN', 'CLIENT')).toEqual([]);
  });

  it('names reopening clearly', () => {
    expect(moveLabel('RESOLVED', 'IN_PROGRESS')).toBe('Reopen');
    expect(moveLabel('OPEN', 'IN_PROGRESS')).toBe('Move to in progress');
  });
});

describe('ticketActions', () => {
  it('lets a client comment, close, and reopen a resolved ticket once, but never write internal notes', () => {
    const open = ticketActions(ticket('OPEN'), 'CLIENT', 'c1');
    expect(open).toMatchObject({ comment: true, internalNote: false, clientClose: true, clientReopen: false, assign: false, priority: false });
    expect(open.moves).toEqual([]);
    expect(ticketActions(ticket('RESOLVED'), 'CLIENT', 'c1').clientReopen).toBe(true);
    expect(ticketActions(ticket('RESOLVED', { reopenCount: 1 }), 'CLIENT', 'c1').clientReopen).toBe(false);
  });

  it('freezes a closed ticket for everybody', () => {
    for (const role of ['CLIENT', 'ADMIN', 'DEVELOPER'] as Role[]) {
      const a = ticketActions(ticket('CLOSED', { assigneeId: 'd1' }), role, 'd1');
      expect(a.comment).toBe(false);
      expect(a.clientClose).toBe(false);
      expect(a.moves).toEqual([]);
      expect(a.assign).toBe(false);
    }
  });

  it('lets management assign, change priority and write internal notes while the ticket is live', () => {
    const a = ticketActions(ticket('IN_PROGRESS'), 'PROJECT_MANAGER', 'm1');
    expect(a).toMatchObject({ assign: true, priority: true, internalNote: true, comment: true });
    expect(ticketActions(ticket('RESOLVED'), 'ADMIN', 'a').priority).toBe(false);
  });

  it('lets a developer work only on a ticket assigned to them, without assign or priority', () => {
    const mine = ticketActions(ticket('OPEN', { assigneeId: 'd1' }), 'DEVELOPER', 'd1');
    expect(mine.moves).toEqual(['IN_PROGRESS', 'WAITING_CLIENT', 'RESOLVED']);
    expect(mine).toMatchObject({ internalNote: true, assign: false, priority: false });
    const other = ticketActions(ticket('OPEN', { assigneeId: 'd2' }), 'DEVELOPER', 'd1');
    expect(other.moves).toEqual([]);
    expect(other.comment).toBe(false);
  });
});

describe('route guards for money and reports', () => {
  let session: SessionService;
  let router: Router;
  const user = (role: Role) =>
    ({ id: 'u', email: 'a@b.c', fullName: 'A B', role, roleLabel: role, status: 'ACTIVE', companyId: null, companyName: null, selfRegistered: false, lastLoginAt: null, createdAt: '' }) as User;
  const check = (roles: Role[]) => TestBed.runInInjectionContext(() => roleGuard({ data: { roles } } as unknown as ActivatedRouteSnapshot, {} as never));

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    session = TestBed.inject(SessionService);
    router = TestBed.inject(Router);
  });

  it('turns a developer away from the invoice screens', () => {
    session.start('t', user('DEVELOPER'));
    expect(router.serializeUrl(check(MONEY_ROLES) as UrlTree)).toBe('/dashboard');
  });

  it.each<Role>(['ADMIN', 'PROJECT_MANAGER', 'CLIENT'])('lets a %s open the invoice screens', (role) => {
    session.start('t', user(role));
    expect(check(MONEY_ROLES)).toBe(true);
  });

  it.each<Role>(['CLIENT', 'DEVELOPER'])('keeps a %s out of the reports', (role) => {
    session.start('t', user(role));
    expect(router.serializeUrl(check(REPORT_ROLES) as UrlTree)).toBe('/dashboard');
  });

  it('sends signed-out people to the sign-in page', () => {
    expect(router.serializeUrl(check(REPORT_ROLES) as UrlTree)).toBe('/login');
  });
});
