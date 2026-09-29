// A circular avatar with the member's initials. The colour is derived from the name,
// so the same person always gets the same colour everywhere in the UI.

const PALETTE = ['#6366f1', '#0ea5e9', '#14b8a6', '#22c55e', '#f59e0b', '#f97316', '#ef4444', '#ec4899', '#a855f7', '#64748b'];

export function colorFor(name: string): string {
  let hash = 0;
  for (const ch of name) {
    hash = (hash * 31 + ch.charCodeAt(0)) | 0;
  }
  return PALETTE[Math.abs(hash) % PALETTE.length];
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  const letters = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : name.slice(0, 2);
  return letters.toUpperCase();
}

interface Props {
  name: string;
  size?: 'xs' | 'sm' | 'md' | 'lg';
}

export default function Avatar({ name, size = 'md' }: Props) {
  return (
    <span className={`avatar avatar-${size}`} style={{ background: colorFor(name) }} title={name} aria-hidden="true">
      {initials(name)}
    </span>
  );
}

export function AvatarStack({ names, max = 5 }: { names: string[]; max?: number }) {
  const shown = names.slice(0, max);
  const extra = names.length - shown.length;
  return (
    <span className="avatar-stack">
      {shown.map((n) => (
        <Avatar key={n} name={n} size="sm" />
      ))}
      {extra > 0 && <span className="avatar avatar-sm avatar-more">+{extra}</span>}
    </span>
  );
}
