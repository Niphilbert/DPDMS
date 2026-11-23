import { barLayout, niceMax } from './reports.component';

describe('revenue chart layout', () => {
  const pad = { left: 10, right: 10, top: 10, bottom: 20 };

  it('rounds the axis maximum up to a tidy number', () => {
    expect(niceMax(0)).toBe(0);
    expect(niceMax(1)).toBe(1);
    expect(niceMax(1180000)).toBe(2000000);
    expect(niceMax(2100000)).toBe(2500000);
    expect(niceMax(4000000)).toBe(5000000);
    expect(niceMax(7000000)).toBe(10000000);
  });

  it('scales every bar on one axis and gives empty months no height', () => {
    const { bars, max } = barLayout(
      [
        { month: '2026-10', invoiced: 0, paid: 0 },
        { month: '2026-11', invoiced: 1180000, paid: 400000 },
      ],
      220,
      130,
      pad,
    );
    expect(max).toBe(2000000);
    expect(bars[0].invoicedH).toBe(0);
    expect(bars[1].invoicedH).toBeCloseTo(100 * (1180000 / 2000000));
    expect(bars[1].paidH).toBeCloseTo(100 * (400000 / 2000000));
    expect(bars[1].invoicedY + bars[1].invoicedH).toBeCloseTo(110); // sits on the axis line
    expect(bars[1].x).toBeGreaterThan(bars[0].x);
  });

  it('copes with no data', () => {
    expect(barLayout([], 220, 130, pad).bars).toEqual([]);
  });
});
