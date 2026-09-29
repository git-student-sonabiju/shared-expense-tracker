import { describe, expect, it } from 'vitest';
import { centsToDecimal, parseCents, splitEqually, toCents } from './money';

describe('parseCents', () => {
  it('accepts whole numbers and up to two decimals', () => {
    expect(parseCents('12')).toBe(1200);
    expect(parseCents('12.5')).toBe(1250);
    expect(parseCents(' 12.05 ')).toBe(1205);
    expect(parseCents('0.01')).toBe(1);
  });

  it('rejects invalid input', () => {
    expect(parseCents('')).toBeNull();
    expect(parseCents('abc')).toBeNull();
    expect(parseCents('-5')).toBeNull();
    expect(parseCents('1.234')).toBeNull();
    expect(parseCents('1e3')).toBeNull();
    expect(parseCents('99999999999')).toBeNull();
  });
});

describe('conversions', () => {
  it('round-trips without floating point drift', () => {
    expect(toCents(0.1 + 0.2)).toBe(30);
    expect(toCents(33.33)).toBe(3333);
    expect(centsToDecimal(1205)).toBe('12.05');
    expect(centsToDecimal(-5)).toBe('-0.05');
  });
});

describe('splitEqually', () => {
  it('matches the backend rule for leftover cents', () => {
    expect(splitEqually(1000, 3)).toEqual([334, 333, 333]);
    expect(splitEqually(900, 3)).toEqual([300, 300, 300]);
    expect(splitEqually(100, 0)).toEqual([]);
  });
});
