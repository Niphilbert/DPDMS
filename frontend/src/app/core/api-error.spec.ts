import { HttpErrorResponse } from '@angular/common/http';
import { toApiError } from './api-error';

const http = (status: number, error?: unknown) => new HttpErrorResponse({ status, error });

describe('toApiError', () => {
  it.each(['PROJECT_NOT_STARTED', 'CODE_TOO_SOON', 'QUOTE_EXPIRED', 'NOT_YOUR_TASK', 'ALREADY_ANSWERED', 'MANAGER_ONLY'])(
    'shows friendly wording for %s',
    (code) => {
      const e = toApiError(http(409, { code, message: 'server text' }));
      expect(e.code).toBe(code);
      expect(e.message).not.toBe('server text');
      expect(e.message.length).toBeGreaterThan(20);
    },
  );

  it('keeps the server message and fields for validation errors', () => {
    const e = toApiError(http(400, { code: 'VALIDATION_FAILED', message: 'Check the form.', fields: { title: 'Enter the task name.' } }));
    expect(e.message).toBe('Check the form.');
    expect(e.fields).toEqual({ title: 'Enter the task name.' });
  });

  it('uses the server message for codes without friendly wording', () => {
    expect(toApiError(http(401, { code: 'BAD_CREDENTIALS', message: 'Wrong email or password.' })).message).toBe('Wrong email or password.');
  });

  it('copes with answers that have no body (401 and 403 from the security layer)', () => {
    expect(toApiError(http(401)).code).toBe('UNAUTHORIZED');
    expect(toApiError(http(403)).code).toBe('FORBIDDEN');
    expect(toApiError(http(403, null)).message).toContain('do not have access');
  });

  it('reports an unreachable server', () => {
    expect(toApiError(http(0)).code).toBe('NETWORK');
  });
});
