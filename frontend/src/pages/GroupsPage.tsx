import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, errorMessage } from '../api';
import type { GroupSummary } from '../types';
import ErrorBanner from '../components/ErrorBanner';
import { colorFor } from '../components/Avatar';
import { ArrowRightIcon, PlusIcon, TrashIcon, UsersIcon } from '../components/Icons';

export default function GroupsPage() {
  const navigate = useNavigate();
  const [groups, setGroups] = useState<GroupSummary[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [name, setName] = useState('');
  const [membersText, setMembersText] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    api.listGroups().then(setGroups).catch((e) => setLoadError(errorMessage(e)));
  }, []);

  async function handleDelete(group: GroupSummary) {
    if (!window.confirm(`Delete "${group.name}"?

All its members, expenses and payments will be permanently removed.`)) {
      return;
    }
    try {
      setLoadError(null);
      await api.deleteGroup(group.id);
      setGroups((prev) => prev?.filter((g) => g.id !== group.id) ?? null);
    } catch (e) {
      setLoadError(errorMessage(e));
    }
  }

  const memberNames = membersText
    .split(/[,\n]/)
    .map((m) => m.trim())
    .filter(Boolean);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    if (!name.trim()) {
      setFormError('Group name is required');
      return;
    }
    const lower = memberNames.map((m) => m.toLowerCase());
    if (new Set(lower).size !== lower.length) {
      setFormError('Member names must be unique');
      return;
    }

    setSubmitting(true);
    setFormError(null);
    try {
      const group = await api.createGroup(name.trim(), memberNames);
      navigate(`/groups/${group.id}`);
    } catch (e) {
      setFormError(errorMessage(e));
      setSubmitting(false);
    }
  }

  return (
    <>
      <section className="hero">
        <div>
          <p className="eyebrow">Group expenses</p>
          <h1>Split expenses, not friendships.</h1>
          <p className="hero-sub">
            Track who paid for what, see who owes whom, and settle up in the fewest possible payments.
          </p>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="bubble b1">₹</div>
          <div className="bubble b2">÷</div>
          <div className="bubble b3">✓</div>
        </div>
      </section>

      <div className="grid grid-home">
        <section className="card">
          <div className="card-head">
            <span className="card-icon">
              <PlusIcon />
            </span>
            <h2>Create a group</h2>
          </div>
          <form onSubmit={handleCreate} className="stack">
            <label>
              Group name
              <input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} placeholder="e.g. Goa trip" />
            </label>
            <label>
              <span>
                Members <span className="hint">· comma or newline separated</span>
              </span>
              <textarea
                value={membersText}
                onChange={(e) => setMembersText(e.target.value)}
                rows={3}
                placeholder="Asha, Ben, Chitra"
              />
            </label>
            {memberNames.length > 0 && (
              <div className="chip-row">
                {memberNames.map((m, i) => (
                  <span key={`${m}-${i}`} className="chip">
                    <span className="dot" style={{ background: colorFor(m) }} />
                    {m}
                  </span>
                ))}
              </div>
            )}
            <ErrorBanner message={formError} />
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create group'}
            </button>
          </form>
        </section>

        <section>
          <div className="section-head">
            <h2>Your groups</h2>
            {groups && groups.length > 0 && <span className="count-pill">{groups.length}</span>}
          </div>
          <ErrorBanner message={loadError} />
          {groups === null && !loadError && (
            <div className="group-grid">
              {[0, 1, 2].map((i) => (
                <div key={i} className="group-tile skeleton" />
              ))}
            </div>
          )}
          {groups?.length === 0 && (
            <div className="empty-state card">
              <span className="empty-icon">
                <UsersIcon size={28} />
              </span>
              <h3>No groups yet</h3>
              <p>Create your first group to start tracking shared expenses.</p>
            </div>
          )}
          <div className="group-grid">
            {groups?.map((g) => (
              <div key={g.id} className="group-tile-wrap">
                <Link to={`/groups/${g.id}`} className="group-tile">
                  <span className="group-badge" style={{ background: colorFor(g.name) }}>
                    {g.name.trim()[0]?.toUpperCase()}
                  </span>
                  <span className="group-tile-body">
                    <span className="group-tile-name">{g.name}</span>
                    <span className="muted small">
                      <UsersIcon size={13} /> {g.memberCount} {g.memberCount === 1 ? 'member' : 'members'} · since{' '}
                      {new Date(g.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                    </span>
                  </span>
                  <span className="group-tile-arrow">
                    <ArrowRightIcon />
                  </span>
                </Link>
                <button
                  type="button"
                  className="tile-delete"
                  onClick={() => handleDelete(g)}
                  title="Delete group"
                  aria-label={`Delete ${g.name}`}
                >
                  <TrashIcon size={16} />
                </button>
              </div>
            ))}
          </div>
        </section>
      </div>
    </>
  );
}
