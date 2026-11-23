import { Invoice, Payment } from '../../core/models';
import { invoiceHistory } from './invoice-detail.component';

const payment = (over: Partial<Payment>): Payment =>
  ({ id: 'p', amount: 400000, paidOn: '2026-11-02', method: 'CASH', reference: null, recordedByName: 'Jane', recordedAt: '2026-11-02T10:00:00Z', reversed: false, reversedAt: null, reversedByName: null, reverseReason: null, ...over }) as Payment;

const invoice = (payments: Payment[], over: Partial<Invoice> = {}): Invoice =>
  ({ createdAt: '2026-11-01T08:00:00Z', createdByName: 'Jane', sentAt: '2026-11-01T09:00:00Z', voidedAt: null, voidReason: null, payments, ...over }) as Invoice;

describe('invoiceHistory', () => {
  it('lists the events oldest first, including reversals', () => {
    const inv = invoice([payment({ reversed: true, reversedAt: '2026-11-03T08:00:00Z', reversedByName: 'Jane', reverseReason: 'Cheque bounced' })]);
    const h = invoiceHistory(inv, false);
    expect(h.map((e) => e.text)).toEqual([
      'Draft created by Jane',
      'Sent to the client',
      'Payment of 400,000 RWF recorded by Jane',
      'Payment of 400,000 RWF reversed by Jane: Cheque bounced',
    ]);
  });

  it('hides the draft step, the reversing person and the reasons from clients', () => {
    const inv = invoice([payment({ reversed: true, reversedAt: '2026-11-03T08:00:00Z', reversedByName: 'Jane', reverseReason: 'Cheque bounced' })], { voidedAt: '2026-11-04T08:00:00Z', voidReason: 'Wrong client' });
    const text = invoiceHistory(inv, true).map((e) => e.text).join('|');
    expect(text).not.toContain('Draft created');
    expect(text).not.toContain('Cheque bounced');
    expect(text).not.toContain('Wrong client');
    expect(text).toContain('Payment of 400,000 RWF reversed');
    expect(text).toContain('Voided');
  });
});
