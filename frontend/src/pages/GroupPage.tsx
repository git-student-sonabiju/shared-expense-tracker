import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError, api, errorMessage } from '../api';
import { formatCents, toCents } from '../money';
import type { Balances, Expense, Group, Settlement, SuggestedPayment } from '../types';
import ErrorBanner from '../components/ErrorBanner';
import MembersCard from '../components/MembersCard';
import ExpenseForm from '../components/ExpenseForm';
import ExpenseList from '../components/ExpenseList';
import BalancesCard from '../components/BalancesCard';
import SettlementForm from '../components/SettlementForm';
import SettlementList from '../components/SettlementList';
import { AvatarStack } from '../components/Avatar';
import { ArrowLeftIcon, CheckCircleIcon, ReceiptIcon, ScaleIcon, SendIcon, TrashIcon, UsersIcon } from '../components/Icons';

interface GroupData {
  group: Group;
  expenses: Expense[];
  settlements: Settlement[];
  balances: Balances;
}

export default function GroupPage() {
  const groupId = Number(useParams().groupId);
  const navigate = useNavigate();
  const [deleting, setDeleting] = useState(false);
  const [data, setData] = useState<GroupData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [prefill, setPrefill] = useState<SuggestedPayment | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const toastTimer = useRef<number | undefined>(undefined);

  // Every mutation re-fetches the whole group so balances and suggestions always reflect the server.
  const reload = useCallback(async () => {
    try {
      const [group, expenses, settlements, balances] = await Promise.all([
        api.getGroup(groupId),
        api.listExpenses(groupId),
        api.listSettlements(groupId),
        api.getBalances(groupId),
      ]);
      setData({ group, expenses, settlements, balances });
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && (e.status === 404 || e.status === 400)) {
        setNotFound(true);
      } else {
        setError(errorMessage(e));
      }
    }
  }, [groupId]);

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => () => window.clearTimeout(toastTimer.current), []);

  function notify(message: string) {
    setToast(message);
    window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setToast(null), 2600);
  }

  if (notFound) {
    return (
      <div className="empty-state card">
        <h3>This group does not exist</h3>
        <Link to="/">Back to your groups</Link>
      </div>
    );
  }
  if (!data) {
    return error ? <ErrorBanner message={error} /> : <div className="loading">Loading group…</div>;
  }

  const { group, expenses, settlements, balances } = data;
  const totalSpent = expenses.reduce((sum, e) => sum + toCents(e.amount), 0);
  const outstanding = balances.balances.reduce((sum, b) => sum + Math.max(0, toCents(b.balance)), 0);

  async function deleteExpense(expenseId: number) {
    await api.deleteExpense(groupId, expenseId);
    await reload();
    notify('Expense deleted');
  }

  async function deleteGroup() {
    const details = [
      `${group.members.length} ${group.members.length === 1 ? 'member' : 'members'}`,
      `${expenses.length} ${expenses.length === 1 ? 'expense' : 'expenses'}`,
      `${settlements.length} ${settlements.length === 1 ? 'payment' : 'payments'}`,
    ].join(', ');
    if (!window.confirm(`Delete "${group.name}"?

This permanently removes ${details}. This cannot be undone.`)) {
      return;
    }
    setDeleting(true);
    try {
      await api.deleteGroup(groupId);
      navigate('/', { replace: true });
    } catch (e) {
      setError(errorMessage(e));
      setDeleting(false);
    }
  }

  async function deleteSettlement(settlementId: number) {
    await api.deleteSettlement(groupId, settlementId);
    await reload();
    notify('Payment deleted');
  }

  return (
    <>
      <Link to="/" className="back-link">
        <ArrowLeftIcon size={16} /> All groups
      </Link>

      <section className="group-hero">
        <div className="group-hero-top">
          <div>
            <h1>{group.name}</h1>
            <div className="group-hero-members">
              <AvatarStack names={group.members.map((m) => m.name)} />
              <span>
                {group.members.length} {group.members.length === 1 ? 'member' : 'members'}
              </span>
            </div>
          </div>
          <div className="hero-actions">
            {outstanding === 0 && expenses.length > 0 && (
              <span className="settled-badge">
                <CheckCircleIcon size={16} /> All settled
              </span>
            )}
            <button type="button" className="hero-danger" onClick={deleteGroup} disabled={deleting}>
              <TrashIcon size={15} /> {deleting ? 'Deleting…' : 'Delete group'}
            </button>
          </div>
        </div>
        <div className="stats">
          <Stat icon={<ReceiptIcon />} label="Total spent" value={formatCents(totalSpent)} />
          <Stat icon={<UsersIcon />} label="Expenses" value={String(expenses.length)} />
          <Stat icon={<ScaleIcon />} label="Outstanding" value={formatCents(outstanding)} />
          <Stat icon={<SendIcon />} label="Payments" value={String(settlements.length)} />
        </div>
      </section>

      <ErrorBanner message={error} />

      <div className="grid">
        <div className="stack-lg">
          <section className="card">
            <div className="card-head">
              <span className="card-icon">
                <ReceiptIcon />
              </span>
              <h2>Add an expense</h2>
            </div>
            {group.members.length === 0 ? (
              <p className="empty">Add members to the group before recording expenses.</p>
            ) : (
              <ExpenseForm
                members={group.members}
                onSubmit={async (expense) => {
                  await api.addExpense(groupId, expense);
                  await reload();
                  notify(`Added "${expense.description}"`);
                }}
              />
            )}
          </section>

          <section className="card">
            <div className="card-head">
              <h2>Expenses</h2>
              {expenses.length > 0 && <span className="count-pill">{expenses.length}</span>}
            </div>
            <ExpenseList expenses={expenses} onDelete={deleteExpense} />
          </section>

          <section className="card">
            <div className="card-head">
              <h2>Payment history</h2>
              {settlements.length > 0 && <span className="count-pill">{settlements.length}</span>}
            </div>
            <SettlementList settlements={settlements} onDelete={deleteSettlement} />
          </section>
        </div>

        <div className="stack-lg">
          <BalancesCard balances={balances} onRecord={setPrefill} />

          <section className="card" id="record-payment">
            <div className="card-head">
              <span className="card-icon">
                <SendIcon />
              </span>
              <h2>Record a payment</h2>
            </div>
            {group.members.length < 2 ? (
              <p className="empty">A group needs at least two members to record a payment.</p>
            ) : (
              <SettlementForm
                members={group.members}
                prefill={prefill}
                onSubmit={async (settlement) => {
                  await api.recordSettlement(groupId, settlement);
                  setPrefill(null);
                  await reload();
                  notify('Payment recorded');
                }}
              />
            )}
          </section>

          <MembersCard
            members={group.members}
            onAdd={async (name) => {
              await api.addMember(groupId, name);
              await reload();
              notify(`${name} joined the group`);
            }}
            onRename={async (memberId, name) => {
              await api.renameMember(groupId, memberId, name);
              await reload();
              notify(`Renamed to ${name}`);
            }}
            onRemove={async (memberId) => {
              await api.removeMember(groupId, memberId);
              await reload();
              notify('Member removed');
            }}
          />
        </div>
      </div>

      {toast && (
        <div className="toast" role="status">
          <CheckCircleIcon size={18} /> {toast}
        </div>
      )}
    </>
  );
}

function Stat({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="stat">
      <span className="stat-icon">{icon}</span>
      <span className="stat-text">
        <span className="stat-label">{label}</span>
        <span className="stat-value">{value}</span>
      </span>
    </div>
  );
}
