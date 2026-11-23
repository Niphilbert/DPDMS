import { daysText, invoiceAmounts, lineTotal, monthLabel, plainNumber, slaText } from './format';

describe('invoice arithmetic (same rules as the server)', () => {
  it('multiplies quantity by unit price and rounds half up', () => {
    expect(lineTotal(1, 500000)).toBe(500000);
    expect(lineTotal(11.75, 25000)).toBe(293750);
    expect(lineTotal(0.5, 3)).toBe(2); // 1.5 rounds up
    expect(lineTotal(0.33, 3)).toBe(1); // 0.99
    expect(lineTotal(0.1, 3)).toBe(0); // 0.3
  });

  it('treats empty or invalid input as zero', () => {
    expect(lineTotal(NaN, 1000)).toBe(0);
    expect(lineTotal(2, null as unknown as number)).toBe(0);
  });

  it('adds up lines, tax and total', () => {
    const a = invoiceAmounts([{ quantity: 1, unitPrice: 500000 }, { quantity: 11.75, unitPrice: 25000 }], 18);
    expect(a.subtotal).toBe(793750);
    expect(a.taxAmount).toBe(142875); // 793750 x 18 / 100
    expect(a.total).toBe(936625);
  });

  it('rounds tax half up and accepts a decimal rate', () => {
    expect(invoiceAmounts([{ quantity: 1, unitPrice: 1000 }], 7.5).taxAmount).toBe(75);
    expect(invoiceAmounts([{ quantity: 1, unitPrice: 5 }], 10).taxAmount).toBe(1); // 0.5 up
    expect(invoiceAmounts([{ quantity: 1, unitPrice: 1000 }], 0)).toEqual({ subtotal: 1000, taxAmount: 0, total: 1000 });
  });
});

describe('time words', () => {
  it('says how long is left', () => {
    expect(slaText(125)).toBe('2 h 5 min left');
    expect(slaText(120)).toBe('2 h left');
    expect(slaText(45)).toBe('45 min left');
    expect(slaText(1440 + 180)).toBe('1 d 3 h left');
    expect(slaText(2880)).toBe('2 d left');
  });

  it('says how late a ticket is', () => {
    expect(slaText(-40)).toBe('breached by 40 min');
    expect(slaText(-135)).toBe('breached by 2 h 15 min');
  });

  it('describes days to a date', () => {
    expect(daysText(0)).toBe('today');
    expect(daysText(1)).toBe('in 1 day');
    expect(daysText(12)).toBe('in 12 days');
    expect(daysText(-5)).toBe('5 days ago');
  });

  it('formats numbers and months', () => {
    expect(plainNumber(11.75)).toBe('11.75');
    expect(plainNumber(7)).toBe('7');
    expect(monthLabel('2026-11')).toBe('Nov 2026');
  });
});
