import { Invoice, InvoiceSummary, Role, TicketStatus, TicketSummary } from './models';

/**
 * Who may do what in the money and care modules. The backend enforces the same rules; these helpers only decide
 * which buttons and menu entries the screens show (docs/api-stage3.md, "Who sees what").
 */

/** Roles that may open the invoice screens (developers never see money). Used by the route guard. */
export const MONEY_ROLES: Role[] = ['ADMIN', 'PROJECT_MANAGER', 'CLIENT'];
/** Roles that may open the reports. */
export const REPORT_ROLES: Role[] = ['ADMIN', 'PROJECT_MANAGER'];

/** Administrators and project managers. */
export const isManagement = (role: Role | undefined | null): boolean => role === 'ADMIN' || role === 'PROJECT_MANAGER';

/** Developers never see money: invoices, payments, project money, reports, renewal prices. */
export const canSeeMoney = (role: Role | undefined | null): boolean => role === 'ADMIN' || role === 'PROJECT_MANAGER' || role === 'CLIENT';

/** Create, edit, send, void invoices; record or reverse payments. */
export const canManageMoney = (role: Role | undefined | null): boolean => isManagement(role);

export const canSeeReports = (role: Role | undefined | null): boolean => isManagement(role);

/** Only the administrator may run the reminder job and the SLA check by hand. */
export const canRunJobs = (role: Role | undefined | null): boolean => role === 'ADMIN';

/** Add, edit, renew and delete renewals. */
export const canManageRenewals = (role: Role | undefined | null): boolean => isManagement(role);

/** Only a client raises a ticket. */
export const canRaiseTicket = (role: Role | undefined | null): boolean => role === 'CLIENT';

/** Internal notes exist for staff only. A client is never offered them. */
export const canWriteInternalNote = (role: Role | undefined | null): boolean => role === 'ADMIN' || role === 'PROJECT_MANAGER' || role === 'DEVELOPER';

export interface InvoiceActions {
  edit: boolean;
  send: boolean;
  pay: boolean;
  remind: boolean;
  void: boolean;
  reversePayments: boolean;
}

/** The buttons for an invoice. Everything is false for people who may not manage money. */
export function invoiceActions(invoice: InvoiceSummary | Invoice, role: Role | undefined | null): InvoiceActions {
  const none: InvoiceActions = { edit: false, send: false, pay: false, remind: false, void: false, reversePayments: false };
  if (!canManageMoney(role)) return none;
  const open = invoice.status === 'SENT' || invoice.status === 'PARTIALLY_PAID';
  return {
    edit: invoice.status === 'DRAFT',
    send: invoice.status === 'DRAFT',
    pay: open && invoice.balance > 0,
    remind: open,
    // an invoice with an active payment cannot be voided: reverse the payment first
    void: invoice.status !== 'VOID' && invoice.paidAmount === 0,
    reversePayments: invoice.status !== 'DRAFT' && invoice.status !== 'VOID',
  };
}

/**
 * Where a ticket may be moved by staff (management, or the developer it is assigned to).
 * RESOLVED to CLOSED is for management only.
 */
const STAFF_MOVES: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'WAITING_CLIENT', 'RESOLVED'],
  IN_PROGRESS: ['WAITING_CLIENT', 'RESOLVED'],
  WAITING_CLIENT: ['IN_PROGRESS', 'RESOLVED'],
  RESOLVED: ['IN_PROGRESS', 'CLOSED'],
  CLOSED: [],
};

export function staffTicketMoves(status: TicketStatus, role: Role | undefined | null): TicketStatus[] {
  if (role === 'CLIENT' || !role) return [];
  return STAFF_MOVES[status].filter((s) => s !== 'CLOSED' || isManagement(role));
}

export interface TicketActions {
  /** Status buttons for staff, in the order to show them. Empty for clients. */
  moves: TicketStatus[];
  comment: boolean;
  internalNote: boolean;
  assign: boolean;
  priority: boolean;
  clientClose: boolean;
  clientReopen: boolean;
}

/**
 * Everything a person may do to a ticket, given their role and id. A developer works only on tickets assigned to
 * them; a client only closes, reopens (once) and comments.
 */
export function ticketActions(ticket: TicketSummary, role: Role | undefined | null, userId: string | undefined | null): TicketActions {
  const closed = ticket.status === 'CLOSED';
  const base: TicketActions = { moves: [], comment: false, internalNote: false, assign: false, priority: false, clientClose: false, clientReopen: false };
  if (!role) return base;
  if (role === 'CLIENT') {
    return {
      ...base,
      comment: !closed,
      clientClose: !closed,
      clientReopen: ticket.status === 'RESOLVED' && ticket.reopenCount < 1,
    };
  }
  const mine = isManagement(role) || (role === 'DEVELOPER' && !!userId && ticket.assigneeId === userId);
  if (!mine) return base;
  const live = ticket.status === 'OPEN' || ticket.status === 'IN_PROGRESS' || ticket.status === 'WAITING_CLIENT';
  return {
    ...base,
    moves: staffTicketMoves(ticket.status, role),
    comment: !closed,
    internalNote: !closed && canWriteInternalNote(role),
    assign: isManagement(role) && !closed,
    priority: isManagement(role) && live,
  };
}

/** Wording for the button that moves a ticket to a status. */
export const MOVE_LABEL: Record<TicketStatus, string> = {
  OPEN: 'Reopen as open',
  IN_PROGRESS: 'Move to in progress',
  WAITING_CLIENT: 'Wait for the client',
  RESOLVED: 'Mark as resolved',
  CLOSED: 'Close the ticket',
};

/** "Reopen" is what staff call moving a resolved ticket back to in progress. */
export function moveLabel(from: TicketStatus, to: TicketStatus): string {
  return from === 'RESOLVED' && to === 'IN_PROGRESS' ? 'Reopen' : MOVE_LABEL[to];
}
