import { useState, type FormEvent } from 'react';
import { errorMessage } from '../api';
import type { Member } from '../types';
import ErrorBanner from './ErrorBanner';
import Avatar from './Avatar';
import { CheckIcon, PencilIcon, PlusIcon, UsersIcon, XIcon } from './Icons';

interface Props {
  members: Member[];
  onAdd: (name: string) => Promise<void>;
  onRename: (memberId: number, name: string) => Promise<void>;
  onRemove: (memberId: number) => Promise<void>;
}

export default function MembersCard({ members, onAdd, onRename, onRemove }: Props) {
  const [name, setName] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function isTaken(candidate: string, exceptId?: number) {
    return members.some((m) => m.id !== exceptId && m.name.toLowerCase() === candidate.toLowerCase());
  }

  async function handleAdd(event: FormEvent) {
    event.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Name is required');
      return;
    }
    if (isTaken(trimmed)) {
      setError(`A member named '${trimmed}' already exists in this group`);
      return;
    }
    await run(async () => {
      await onAdd(trimmed);
      setName('');
    });
  }

  function startEdit(member: Member) {
    setEditingId(member.id);
    setEditName(member.name);
    setError(null);
  }

  async function saveEdit(event: FormEvent, member: Member) {
    event.preventDefault();
    const trimmed = editName.trim();
    if (!trimmed) {
      setError('Name is required');
      return;
    }
    if (trimmed === member.name) {
      setEditingId(null);
      return;
    }
    if (isTaken(trimmed, member.id)) {
      setError(`A member named '${trimmed}' already exists in this group`);
      return;
    }
    await run(async () => {
      await onRename(member.id, trimmed);
      setEditingId(null);
    });
  }

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card">
      <div className="card-head">
        <span className="card-icon">
          <UsersIcon />
        </span>
        <h2>Members</h2>
        {members.length > 0 && <span className="count-pill">{members.length}</span>}
      </div>
      {members.length === 0 && <p className="empty">No members yet. Add someone below.</p>}
      <div className="member-grid">
        {members.map((m) =>
          editingId === m.id ? (
            <form key={m.id} className="member-chip editing" onSubmit={(e) => saveEdit(e, m)}>
              <Avatar name={editName.trim() || m.name} size="sm" />
              <input
                className="member-edit-input"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                onKeyDown={(e) => e.key === 'Escape' && setEditingId(null)}
                maxLength={60}
                autoFocus
                aria-label={`New name for ${m.name}`}
              />
              <button type="submit" className="member-action save" disabled={busy} aria-label="Save name">
                <CheckIcon size={14} />
              </button>
              <button type="button" className="member-action" onClick={() => setEditingId(null)} aria-label="Cancel">
                <XIcon size={14} />
              </button>
            </form>
          ) : (
            <span key={m.id} className="member-chip">
              <Avatar name={m.name} size="sm" />
              <span className="member-name">{m.name}</span>
              <button
                type="button"
                className="member-action"
                disabled={busy}
                onClick={() => startEdit(m)}
                title="Rename"
                aria-label={`Rename ${m.name}`}
              >
                <PencilIcon size={13} />
              </button>
              <button
                type="button"
                className="member-action danger"
                disabled={busy}
                onClick={() => run(() => onRemove(m.id))}
                title="Remove (only members without expenses or payments)"
                aria-label={`Remove ${m.name}`}
              >
                <XIcon size={14} />
              </button>
            </span>
          ),
        )}
      </div>
      <form onSubmit={handleAdd} className="inline-form">
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Add a member…" maxLength={60} />
        <button type="submit" className="btn-primary btn-icon" disabled={busy} aria-label="Add member">
          <PlusIcon size={18} />
        </button>
      </form>
      <ErrorBanner message={error} />
    </section>
  );
}
