// Client-side money helpers. Like the backend, all arithmetic is done in integer cents;
// amounts are sent to the API as decimal strings (e.g. "12.50") so no precision is lost.

const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/;
const MAX_CENTS = 999_999_999_999; // 10 integer digits, matching the backend limit

/** Parses user input like "12", "12.5" or "12.50" into cents. Returns null if it is not a valid amount. */
export function parseCents(input: string): number | null {
  const trimmed = input.trim();
  if (!AMOUNT_PATTERN.test(trimmed)) {
    return null;
  }
  const [whole, fraction = ''] = trimmed.split('.');
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
  return cents <= MAX_CENTS ? cents : null;
}

/** Converts an API amount (a JSON number with up to 2 decimals) to cents. */
export function toCents(amount: number): number {
  return Math.round(amount * 100);
}

/** Formats cents as a plain decimal string for sending to the API, e.g. 1250 -> "12.50". */
export function centsToDecimal(cents: number): string {
  const sign = cents < 0 ? '-' : '';
  const abs = Math.abs(cents);
  return `${sign}${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, '0')}`;
}

const formatter = new Intl.NumberFormat(undefined, {
  style: 'currency',
  currency: import.meta.env.VITE_CURRENCY ?? 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** Formats an API amount for display, e.g. 12.5 -> "₹12.50". */
export function formatMoney(amount: number): string {
  return formatter.format(toCents(amount) / 100);
}

export function formatCents(cents: number): string {
  return formatter.format(cents / 100);
}

/** Same rule as the backend's SplitCalculator: leftover cents go to the first members. */
export function splitEqually(totalCents: number, count: number): number[] {
  if (count <= 0) {
    return [];
  }
  const base = Math.floor(totalCents / count);
  const remainder = totalCents % count;
  return Array.from({ length: count }, (_, i) => base + (i < remainder ? 1 : 0));
}
