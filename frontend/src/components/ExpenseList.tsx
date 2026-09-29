import { useState } from 'react';
import { errorMessage } from '../api';
import { formatMoney } from '../money';
import type { Expense } from '../types';
import ErrorBanner from './ErrorBanner';
import Avatar from './Avatar';
import { ReceiptIcon, TrashIcon } from './Icons';

interface Props {
  expenses: Expense[];
  onDelete: (expenseId: number) => Promise<void>;
}

export default function ExpenseList({ expenses, onDelete }: Props) {
  const [error, setError] = useState<string | null>(null);

  if (expenses.length === 0) {
    return (
      <div className="empty-inline">
        <ReceiptIcon size={22} />
        <span>No expenses yet. Add the first one above.</span>
      </div>
    );
  }

  async function handleDelete(expense: Expense) {
    if (!window.confirm(`Delete "${expense.description}"? Balances will be recalculated.`)) {
      return;
    }
    try {
      setError(null);
      await onDelete(expense.id);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <>
      <ErrorBanner message={error} />
      <ul className="feed">
        {expenses.map((e) => (
          <li key={e.id} className="feed-item">
            <Avatar name={e.paidBy.name} />
            <div className="feed-body">
              <div className="row">
                <span className="feed-title">{e.description}</span>
                <span className="feed-amount">{formatMoney(e.amount)}</span>
              </div>
              <div className="muted small">
                <span className="strong-muted">{e.paidBy.name}</span> paid ·{' '}
                <span className={`tag ${e.splitType === 'EQUAL' ? 'tag-equal' : 'tag-exact'}`}>
                  {e.splitType === 'EQUAL' ? 'Equal split' : 'Exact split'}
                </span>{' '}
                · {new Date(e.createdAt).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}
              </div>
              <div className="chip-row">
                {e.shares.map((s) => (
                  <span key={s.memberId} className="chip">
                    <Avatar name={s.memberName} size="xs" />
                    {s.memberName} <span className="chip-amount">{formatMoney(s.amount)}</span>
                  </span>
                ))}
              </div>
            </div>
            <button type="button" className="icon-button" onClick={() => handleDelete(e)} title="Delete expense">
              <TrashIcon size={16} />
            </button>
          </li>
        ))}
      </ul>
    </>
  );
}
