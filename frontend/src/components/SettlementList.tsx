import { useState } from 'react';
import { errorMessage } from '../api';
import { formatMoney } from '../money';
import type { Settlement } from '../types';
import ErrorBanner from './ErrorBanner';
import Avatar from './Avatar';
import { ArrowRightIcon, SendIcon, TrashIcon } from './Icons';

interface Props {
  settlements: Settlement[];
  onDelete: (settlementId: number) => Promise<void>;
}

export default function SettlementList({ settlements, onDelete }: Props) {
  const [error, setError] = useState<string | null>(null);

  if (settlements.length === 0) {
    return (
      <div className="empty-inline">
        <SendIcon size={20} />
        <span>No payments recorded yet.</span>
      </div>
    );
  }

  async function handleDelete(s: Settlement) {
    if (!window.confirm(`Delete the payment from ${s.from.name} to ${s.to.name}?`)) {
      return;
    }
    try {
      setError(null);
      await onDelete(s.id);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <>
      <ErrorBanner message={error} />
      <ul className="feed">
        {settlements.map((s) => (
          <li key={s.id} className="feed-item">
            <span className="pair">
              <Avatar name={s.from.name} size="sm" />
              <span className="pair-arrow">
                <ArrowRightIcon size={12} />
              </span>
              <Avatar name={s.to.name} size="sm" />
            </span>
            <div className="feed-body">
              <div className="row">
                <span>
                  <span className="strong">{s.from.name}</span> paid <span className="strong">{s.to.name}</span>
                </span>
                <span className="feed-amount positive">{formatMoney(s.amount)}</span>
              </div>
              <div className="muted small">
                {new Date(s.createdAt).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}
                {s.note && <> · “{s.note}”</>}
              </div>
            </div>
            <button type="button" className="icon-button" onClick={() => handleDelete(s)} title="Delete payment">
              <TrashIcon size={16} />
            </button>
          </li>
        ))}
      </ul>
    </>
  );
}
