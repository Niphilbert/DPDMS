import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

/**
 * Friendly wording for the error codes in docs/api.md. When a code is listed here this text is shown instead of the
 * server's sentence, because it tells the person what to do next. VALIDATION_FAILED is never listed: its message and
 * its per-field messages come from the server.
 */
export const FRIENDLY: Record<string, string> = {
  PROJECT_NOT_STARTED: 'This project has not started yet. A project manager must start it before work can be moved, handed in or logged.',
  ALREADY_STARTED: 'This project has already started.',
  PROJECT_LAUNCHED: 'This project has launched, so no new tasks can be added.',
  CODE_TOO_SOON: 'A code was sent a moment ago. Please wait a minute before asking for another one; the code we already sent still works.',
  INVALID_TRANSITION: 'That change is not possible from the current status. Reload the page to see the latest status.',
  SUBMISSION_REQUIRED: 'To send a task for review, use "Submit work" and add a file or link.',
  INVALID_STATE: 'This was already done, or it is not possible in the current status. Reload the page to see the latest status.',
  QUOTE_EXPIRED: 'This quote is no longer valid. Ask the project manager to send a new quote.',
  NO_MANAGER: 'No project manager is available to run this project yet. Please contact ITZETech.',
  TASK_DONE: 'This task is finished, so it cannot be changed, reassigned or get more time.',
  ALREADY_ANSWERED: 'This was already answered.',
  NOT_YOUR_TASK: 'This task belongs to someone else.',
  MANAGER_ONLY: 'Only a project manager can mark a task as done. Submit your work for review instead.',
  CLIENT_ONLY: 'Only the client can do this.',
  FORBIDDEN: 'You do not have access to this.',
  NOT_FOUND: 'We could not find that. It may have been removed, or you may not have access to it.',
  DEADLINE_PAST: 'The deadline must be after today.',
  VALID_UNTIL_PAST: 'The quote must be valid until today or a later date.',
  DUE_DATE_PAST: 'The due date must be today or a later date.',
  NOT_ASSIGNABLE: 'Tasks can only be given to active developers and project managers.',
  NOT_A_DEVELOPER: 'Project access can only be given to an active developer.',
  DATE_IN_FUTURE: 'You cannot log time for a day that has not happened yet.',
  METHOD_NOT_ALLOWED: 'That action is not available.',
  SERVER_ERROR: 'Something went wrong on our side. Please try again in a moment.',
  LAST_ADMIN: 'There must always be at least one active administrator.',
  SELF_NOT_ALLOWED: 'You cannot do this to your own account.',
  // ---- stage 3: invoices, renewals and tickets
  DUE_BEFORE_ISSUE: 'The due date cannot be before the issue date.',
  INVALID_RANGE: 'The "from" date must be on or before the "to" date.',
  EXPIRY_NOT_LATER: 'The new expiry date must be after the current expiry date.',
  INVOICE_NOT_DRAFT: 'Only a draft invoice can be changed or sent. This one has already been sent. Reload the page to see its latest status.',
  INVOICE_NOT_PAYABLE: 'A payment can only be recorded on an invoice that was sent and is not fully paid yet.',
  OVERPAYMENT: 'That payment is more than the amount still owed on this invoice. Enter the balance or less. Nothing was recorded.',
  INVOICE_HAS_PAYMENTS: 'This invoice has a payment on it, so it cannot be voided. Reverse the payment first, then void the invoice.',
  PAYMENT_ALREADY_REVERSED: 'This payment was already reversed.',
  HAS_HISTORY: 'This item has been renewed before, so it cannot be deleted. Its history must be kept.',
  TICKET_CLOSED: 'This ticket is closed and cannot be changed any more. Open a new ticket if the problem comes back.',
  REOPEN_LIMIT: 'This ticket was already reopened once. Please open a new ticket and mention this one.',
  BAD_REQUEST: 'That request was not understood. Check the values you chose and try again.',
};

/** Turns whatever HttpClient throws into the plain `{status, code, message, fields}` shape the screens use. */
export function toApiError(err: unknown): ApiError {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return { status: 0, code: 'NETWORK', message: 'Cannot reach the server. Check your connection and try again.', fields: {} };
    }
    // Security-layer answers (401, and 403 on /api/admin) arrive without a body.
    const body = (typeof err.error === 'object' && err.error !== null ? err.error : {}) as {
      code?: string;
      message?: string;
      fields?: Record<string, string>;
    };
    const code = body.code ?? (err.status === 401 ? 'UNAUTHORIZED' : err.status === 403 ? 'FORBIDDEN' : err.status === 404 ? 'NOT_FOUND' : 'ERROR');
    const message = FRIENDLY[code] ?? body.message ?? (code === 'UNAUTHORIZED' ? 'Please sign in again.' : 'Something went wrong. Please try again.');
    return { status: err.status, code, message, fields: body.fields ?? {} };
  }
  return { status: 0, code: 'ERROR', message: 'Something went wrong. Please try again.', fields: {} };
}
