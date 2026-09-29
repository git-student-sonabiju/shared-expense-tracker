import { formatMoney, toCents } from '../money';
import type { Balances, SuggestedPayment } from '../types';
import Avatar from './Avatar';
import { ArrowRightIcon, CheckCircleIcon, ScaleIcon } from './Icons';

interface Props {
  balances: Balances;
  onRecord: (payment: SuggestedPayment) => void;
}

export default function BalancesCard({ balances, onRecord }: Props) {
  const maxAbs = Math.max(1, ...balances.balances.map((b) => Math.abs(toCents(b.balance))));

  function record(payment: SuggestedPayment) {
    onRecord(payment);
    const target = document.getElementById('record-payment');
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    target?.classList.remove('flash');
    void target?.offsetWidth; // restart the highlight animation
    target?.classList.add('flash');
  }

  return (
    <>
      <section className="card">
        <div className="card-head">
          <span className="card-icon">
            <ScaleIcon />
          </span>
          <h2>Balances</h2>
        </div>
        {balances.balances.length === 0 && <p className="empty">No members yet.</p>}
        <ul className="balance-list">
          {balances.balances.map((b) => {
            const cents = toCents(b.balance);
            const pct = (Math.abs(cents) / maxAbs) * 100;
            const tone = cents > 0 ? 'positive' : cents < 0 ? 'negative' : 'neutral';
            return (
              <li key={b.memberId} className="balance-row">
                <Avatar name={b.memberName} size="sm" />
                <div className="balance-main">
                  <div className="row">
                    <span className="strong">{b.memberName}</span>
                    <span className={`balance-amount ${tone}`}>
                      {cents > 0 ? '+' : cents < 0 ? '−' : ''}
                      {formatMoney(Math.abs(b.balance))}
                    </span>
                  </div>
                  {/* Diverging bar: debts grow left from the centre, credits grow right. */}
                  <div className="balance-bar" aria-hidden="true">
                    <span className="half left">{cents < 0 && <span className="fill negative" style={{ width: `${pct}%` }} />}</span>
                    <span className="half right">{cents > 0 && <span className="fill positive" style={{ width: `${pct}%` }} />}</span>
                  </div>
                  <span className="muted tiny">{cents > 0 ? 'gets back' : cents < 0 ? 'owes' : 'settled up'}</span>
                </div>
              </li>
            );
          })}
        </ul>
      </section>

      <section className="card card-accent">
        <div className="card-head">
          <h2>Settle up</h2>
          {balances.suggestedPayments.length > 0 && (
            <span className="count-pill">
              {balances.suggestedPayments.length} {balances.suggestedPayments.length === 1 ? 'payment' : 'payments'}
            </span>
          )}
        </div>
        {balances.suggestedPayments.length === 0 ? (
          <div className="settled">
            <CheckCircleIcon size={28} />
            <div>
              <div className="strong">Everyone is settled up</div>
              <div className="muted small">No payments needed right now.</div>
            </div>
          </div>
        ) : (
          <>
            <p className="muted small">The fewest payments that clear every debt in the group.</p>
            <ul className="payment-list">
              {balances.suggestedPayments.map((p) => (
                <li key={`${p.fromMemberId}-${p.toMemberId}`} className="payment">
                  <div className="payment-party">
                    <Avatar name={p.fromMemberName} size="sm" />
                    <span className="payment-name">{p.fromMemberName}</span>
                  </div>
                  <div className="payment-flow">
                    <span className="payment-amount">{formatMoney(p.amount)}</span>
                    <ArrowRightIcon size={16} />
                  </div>
                  <div className="payment-party">
                    <Avatar name={p.toMemberName} size="sm" />
                    <span className="payment-name">{p.toMemberName}</span>
                  </div>
                  <button type="button" className="btn-soft" onClick={() => record(p)}>
                    Record
                  </button>
                </li>
              ))}
            </ul>
          </>
        )}
      </section>
    </>
  );
}
