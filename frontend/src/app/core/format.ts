import {
  ApprovalStatus,
  ChangeStatus,
  InvoiceDisplayStatus,
  PaymentMethod,
  ProjectStatus,
  RenewalStatus,
  RenewalType,
  RequestStatus,
  SlaState,
  TaskStatus,
  TicketPriority,
  TicketStatus,
} from './models';

export interface Badge {
  label: string;
  cls: string;
}

/** Whole Rwandan francs, for example "1,500,000 RWF". */
export function rwf(amount: number | null | undefined): string {
  return amount === null || amount === undefined ? '' : `${new Intl.NumberFormat('en-US').format(amount)} RWF`;
}

/** Today as YYYY-MM-DD in the person's own time zone (what a date input expects). */
export function todayIso(offsetDays = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

export const REQUEST_STATUS: Record<RequestStatus, Badge> = {
  NEW: { label: 'Waiting for a quote', cls: 'p-info' },
  QUOTED: { label: 'Quote sent', cls: 'p-warn' },
  CHANGES_REQUESTED: { label: 'Changes asked', cls: 'p-warn' },
  ACCEPTED: { label: 'Accepted', cls: 'p-ok' },
  DECLINED: { label: 'Declined', cls: 'p-bad' },
};

export const PROJECT_STATUS: Record<ProjectStatus, Badge> = {
  PLANNING: { label: 'Planning', cls: 'p-neutral' },
  IN_PROGRESS: { label: 'In progress', cls: 'p-info' },
  IN_REVIEW: { label: 'In review', cls: 'p-warn' },
  LAUNCHED: { label: 'Launched', cls: 'p-ok' },
};

export const TASK_STATUS: Record<TaskStatus, Badge> = {
  TODO: { label: 'To do', cls: 'p-neutral' },
  DOING: { label: 'Doing', cls: 'p-info' },
  REVIEW: { label: 'In review', cls: 'p-warn' },
  DONE: { label: 'Done', cls: 'p-ok' },
};

export const APPROVAL_STATUS: Record<ApprovalStatus, Badge> = {
  PENDING: { label: 'Waiting for answer', cls: 'p-warn' },
  APPROVED: { label: 'Approved', cls: 'p-ok' },
  CHANGES_REQUESTED: { label: 'Changes asked', cls: 'p-bad' },
};

export const CHANGE_STATUS: Record<ChangeStatus, Badge> = {
  PROPOSED: { label: 'Waiting for answer', cls: 'p-warn' },
  ACCEPTED: { label: 'Accepted', cls: 'p-ok' },
  DECLINED: { label: 'Declined', cls: 'p-bad' },
};

export const APPROVAL_TYPE = { DESIGN: 'Design', PAGE_CONTENT: 'Page content', DOCUMENT: 'Document' } as const;
export const PHASES = ['Discovery', 'Design', 'Build', 'Test and launch'];
export const TASK_COLUMNS: TaskStatus[] = ['TODO', 'DOING', 'REVIEW', 'DONE'];

/** A submission may be a web link; show it as a link only when it is one. */
export function isUrl(text: string | undefined | null): boolean {
  return !!text && /^https?:\/\//i.test(text);
}

// ---------------------------------------------------------------- stage 3: money and care

export const INVOICE_STATUS: Record<InvoiceDisplayStatus, Badge> = {
  DRAFT: { label: 'Draft', cls: 'p-neutral' },
  SENT: { label: 'Sent', cls: 'p-info' },
  PARTIALLY_PAID: { label: 'Partly paid', cls: 'p-warn' },
  PAID: { label: 'Paid', cls: 'p-ok' },
  OVERDUE: { label: 'Overdue', cls: 'p-bad' },
  VOID: { label: 'Void', cls: 'p-neutral' },
};

export const PAYMENT_METHOD: Record<PaymentMethod, string> = {
  MOBILE_MONEY: 'Mobile money',
  BANK_TRANSFER: 'Bank transfer',
  CASH: 'Cash',
};

export const RENEWAL_STATUS: Record<RenewalStatus, Badge> = {
  ACTIVE: { label: 'Active', cls: 'p-ok' },
  EXPIRING: { label: 'Expiring soon', cls: 'p-warn' },
  EXPIRED: { label: 'Expired', cls: 'p-bad' },
};

export const RENEWAL_TYPE: Record<RenewalType, string> = {
  DOMAIN: 'Domain',
  HOSTING: 'Hosting',
  SSL: 'SSL certificate',
  MAINTENANCE: 'Maintenance plan',
};

export const TICKET_STATUS: Record<TicketStatus, Badge> = {
  OPEN: { label: 'Open', cls: 'p-info' },
  IN_PROGRESS: { label: 'In progress', cls: 'p-info' },
  WAITING_CLIENT: { label: 'Waiting for the client', cls: 'p-warn' },
  RESOLVED: { label: 'Resolved', cls: 'p-ok' },
  CLOSED: { label: 'Closed', cls: 'p-neutral' },
};

export const TICKET_PRIORITY: Record<TicketPriority, Badge & { sla: number }> = {
  URGENT: { label: 'Urgent', cls: 'p-bad', sla: 4 },
  HIGH: { label: 'High', cls: 'p-warn', sla: 8 },
  MEDIUM: { label: 'Medium', cls: 'p-info', sla: 24 },
  LOW: { label: 'Low', cls: 'p-neutral', sla: 72 },
};
export const PRIORITIES: TicketPriority[] = ['URGENT', 'HIGH', 'MEDIUM', 'LOW'];

export const SLA_STATE: Record<SlaState, Badge> = {
  ON_TRACK: { label: 'On track', cls: 'p-ok' },
  AT_RISK: { label: 'At risk', cls: 'p-warn' },
  BREACHED: { label: 'Deadline missed', cls: 'p-bad' },
  MET: { label: 'Deadline met', cls: 'p-ok' },
  MISSED: { label: 'Deadline missed', cls: 'p-bad' },
};

/**
 * Time to the answer deadline in words. Positive minutes are "left", negative ones are "breached by".
 * Examples: "2 h 5 min left", "1 d 3 h left", "breached by 40 min".
 */
export function slaText(minutesLeft: number): string {
  const total = Math.abs(Math.trunc(minutesLeft));
  const d = Math.floor(total / 1440);
  const h = Math.floor((total % 1440) / 60);
  const m = total % 60;
  let span: string;
  if (d > 0) span = h > 0 ? `${d} d ${h} h` : `${d} d`;
  else if (h > 0) span = m > 0 ? `${h} h ${m} min` : `${h} h`;
  else span = `${m} min`;
  return minutesLeft < 0 ? `breached by ${span}` : `${span} left`;
}

/** Whole days to a date in words: "in 12 days", "today", "5 days ago". */
export function daysText(days: number): string {
  if (days === 0) return 'today';
  const n = Math.abs(days);
  const unit = n === 1 ? 'day' : 'days';
  return days > 0 ? `in ${n} ${unit}` : `${n} ${unit} ago`;
}

/**
 * Line total as the server computes it: round(quantity x unitPrice), half up. Done in whole hundredths so that
 * 11.75 x 25000 or 0.1 x 3 never drift on floating point.
 */
export function lineTotal(quantity: number, unitPrice: number): number {
  const q = Math.round((Number(quantity) || 0) * 100);
  const p = Math.round(Number(unitPrice) || 0);
  return Math.floor((q * p + 50) / 100);
}

/** Subtotal, tax and total of an invoice as the server computes them. `taxRate` is a percentage with up to 2 decimals. */
export function invoiceAmounts(lines: { quantity: number; unitPrice: number }[], taxRate: number): { subtotal: number; taxAmount: number; total: number } {
  const subtotal = lines.reduce((sum, l) => sum + lineTotal(l.quantity, l.unitPrice), 0);
  const rate = Math.round((Number(taxRate) || 0) * 100);
  const taxAmount = Math.floor((subtotal * rate + 5000) / 10000);
  return { subtotal, taxAmount, total: subtotal + taxAmount };
}

/** Quantities and rates without trailing zeros: 1, 11.75, 7.5. */
export function plainNumber(n: number): string {
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(n);
}

/** "2026-11" as "Nov 2026". */
export function monthLabel(ym: string): string {
  const [y, m] = ym.split('-').map(Number);
  return new Intl.DateTimeFormat('en-US', { month: 'short', year: 'numeric', timeZone: 'UTC' }).format(new Date(Date.UTC(y, (m || 1) - 1, 1)));
}
