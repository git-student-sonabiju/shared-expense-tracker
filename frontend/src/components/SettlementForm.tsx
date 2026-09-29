import { useEffect, useState, type FormEvent } from 'react';
import { errorMessage } from '../api';
import { centsToDecimal, parseCents, toCents } from '../money';
import type { Member, NewSettlement, SuggestedPayment } from '../types';
import ErrorBanner from './ErrorBanner';

interface Props {
  members: Member[];
  prefill: SuggestedPayment | null;
  onSubmit: (settlement: NewSettlement) => Promise<void>;
}

export default function SettlementForm({ members, prefill, onSubmit }: Props) {
  const [fromId, setFromId] = useState<number>(members[0].id);
  const [toId, setToId] = useState<number>(members[1].id);
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (prefill) {
      setFromId(prefill.fromMemberId);
      setToId(prefill.toMemberId);
      setAmount(centsToDecimal(toCents(prefill.amount)));
      setError(null);
    }
  }, [prefill]);

  // Fall back to valid members if a selected member was removed from the group.
  const memberIds = new Set(members.map((m) => m.id));
  const from = memberIds.has(fromId) ? fromId : members[0].id;
  const to = memberIds.has(toId) ? toId : members.find((m) => m.id !== from)!.id;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const cents = parseCents(amount);
    if (from === to) {
      setError('Payer and recipient must be different members');
      return;
    }
    if (cents === null || cents === 0) {
      setError('Amount must be a positive number with at most 2 decimal places');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({ fromMemberId: from, toMemberId: to, amount: centsToDecimal(cents), note: note.trim() || undefined });
      setAmount('');
      setNote('');
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
          From
          <select value={from} onChange={(e) => setFromId(Number(e.target.value))}>
            {members.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          To
          <select value={to} onChange={(e) => setToId(Number(e.target.value))}>
            {members.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="two-col">
        <label>
          Amount
          <input value={amount} onChange={(e) => setAmount(e.target.value)} inputMode="decimal" placeholder="0.00" />
        </label>
        <label>
          <span>
            Note <span className="hint">(optional)</span>
          </span>
          <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={200} placeholder="UPI transfer" />
        </label>
      </div>
      <ErrorBanner message={error} />
      <button type="submit" className="btn-primary" disabled={submitting}>
        {submitting ? 'Saving…' : 'Record payment'}
      </button>
    </form>
  );
}
