import { useState, type FormEvent } from 'react';
import { errorMessage } from '../api';
import { centsToDecimal, formatCents, parseCents, splitEqually } from '../money';
import type { Member, NewExpense, SplitType } from '../types';
import ErrorBanner from './ErrorBanner';
import Avatar from './Avatar';

interface Props {
  members: Member[];
  onSubmit: (expense: NewExpense) => Promise<void>;
}

export default function ExpenseForm({ members, onSubmit }: Props) {
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [paidBy, setPaidBy] = useState<number>(members[0].id);
  const [splitType, setSplitType] = useState<SplitType>('EQUAL');
  const [included, setIncluded] = useState<Set<number>>(() => new Set(members.map((m) => m.id)));
  const [exactAmounts, setExactAmounts] = useState<Record<number, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Members added after the form mounted are not selected by default; members removed are ignored.
  const memberIds = new Set(members.map((m) => m.id));
  const selectedIds = [...included].filter((id) => memberIds.has(id)).sort((a, b) => a - b);
  const totalCents = parseCents(amount);
  const payerId = memberIds.has(paidBy) ? paidBy : members[0].id;

  const equalShares = new Map<number, number>();
  if (splitType === 'EQUAL' && totalCents !== null) {
    splitEqually(totalCents, selectedIds.length).forEach((cents, i) => equalShares.set(selectedIds[i], cents));
  }

  const exactEntries = members
    .filter((m) => (exactAmounts[m.id] ?? '').trim() !== '')
    .map((m) => ({ member: m, cents: parseCents(exactAmounts[m.id]) }));
  const exactSum = exactEntries.reduce((sum, e) => sum + (e.cents ?? 0), 0);

  function toggle(memberId: number) {
    setIncluded((prev) => {
      const next = new Set(prev);
      if (next.has(memberId)) {
        next.delete(memberId);
      } else {
        next.add(memberId);
      }
      return next;
    });
  }

  function validate(): NewExpense | string {
    if (!description.trim()) return 'Description is required';
    if (totalCents === null) return 'Amount must be a positive number with at most 2 decimal places';
    if (totalCents === 0) return 'Amount must be greater than zero';

    if (splitType === 'EQUAL') {
      if (selectedIds.length === 0) return 'Select at least one member to split with';
      if (totalCents < selectedIds.length) return `Amount is too small to split among ${selectedIds.length} members`;
      return {
        description: description.trim(),
        amount: centsToDecimal(totalCents),
        paidByMemberId: payerId,
        splitType,
        splits: selectedIds.map((memberId) => ({ memberId })),
      };
    }

    if (exactEntries.length === 0) return 'Enter an amount for at least one member';
    const invalid = exactEntries.find((e) => e.cents === null || e.cents === 0);
    if (invalid) return `${invalid.member.name}'s share must be a positive amount with at most 2 decimal places`;
    if (exactSum !== totalCents) {
      return `Shares add up to ${formatCents(exactSum)} but the expense is ${formatCents(totalCents)}`;
    }
    return {
      description: description.trim(),
      amount: centsToDecimal(totalCents),
      paidByMemberId: payerId,
      splitType,
      splits: exactEntries.map((e) => ({ memberId: e.member.id, amount: centsToDecimal(e.cents!) })),
    };
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const result = validate();
    if (typeof result === 'string') {
      setError(result);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit(result);
      setDescription('');
      setAmount('');
      setExactAmounts({});
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="stack">
      <div className="two-col">
        <label>
          Description
          <input value={description} onChange={(e) => setDescription(e.target.value)} maxLength={200} placeholder="Dinner" />
        </label>
        <label>
          Amount
          <input value={amount} onChange={(e) => setAmount(e.target.value)} inputMode="decimal" placeholder="0.00" />
        </label>
      </div>

      <div className="two-col">
        <label>
          Paid by
          <select value={payerId} onChange={(e) => setPaidBy(Number(e.target.value))}>
            {members.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>
        </label>
        <fieldset className="segmented">
          <legend>Split</legend>
          {(['EQUAL', 'EXACT'] as const).map((type) => (
            <label key={type} className={splitType === type ? 'active' : ''}>
              <input
                type="radio"
                name="splitType"
                checked={splitType === type}
                onChange={() => setSplitType(type)}
              />
              {type === 'EQUAL' ? 'Equally' : 'Exact amounts'}
            </label>
          ))}
        </fieldset>
      </div>

      <div className="participants">
        {members.map((m) =>
          splitType === 'EQUAL' ? (
            <label key={m.id} className={`participant ${included.has(m.id) ? 'selected' : ''}`}>
              <input type="checkbox" checked={included.has(m.id)} onChange={() => toggle(m.id)} />
              <Avatar name={m.name} size="sm" />
              <span>{m.name}</span>
              <span className="muted amount">
                {equalShares.has(m.id) ? formatCents(equalShares.get(m.id)!) : ''}
              </span>
            </label>
          ) : (
            <label key={m.id} className="participant">
              <Avatar name={m.name} size="sm" />
              <span>{m.name}</span>
              <input
                className="amount-input"
                value={exactAmounts[m.id] ?? ''}
                onChange={(e) => setExactAmounts((prev) => ({ ...prev, [m.id]: e.target.value }))}
                inputMode="decimal"
                placeholder="0.00"
              />
            </label>
          ),
        )}
        {splitType === 'EXACT' && totalCents !== null && (
          <p className={exactSum === totalCents ? 'split-status ok' : 'split-status pending'}>
            {exactSum === totalCents
              ? 'Shares add up to the total.'
              : `${formatCents(Math.abs(totalCents - exactSum))} ${exactSum < totalCents ? 'left to assign' : 'over the total'}`}
          </p>
        )}
      </div>

      <ErrorBanner message={error} />
      <button type="submit" className="btn-primary" disabled={submitting}>
        {submitting ? 'Saving…' : 'Add expense'}
      </button>
    </form>
  );
}
