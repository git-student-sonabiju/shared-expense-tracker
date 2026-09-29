import { useEffect, useRef, useState } from 'react';
import { Link, Route, Routes, useNavigate } from 'react-router-dom';
import GroupsPage from './pages/GroupsPage';
import GroupPage from './pages/GroupPage';
import LoginPage from './pages/LoginPage';
import { useAuth } from './auth';
import Avatar from './components/Avatar';
import { WalletIcon } from './components/Icons';

export default function App() {
  const { user } = useAuth();

  if (user === undefined) {
    return <div className="loading">Loading…</div>;
  }
  if (user === null) {
    return <LoginPage />;
  }

  return (
    <>
      <header className="app-header">
        <div className="app-header-inner">
          <Link to="/" className="brand">
            <span className="brand-mark">
              <WalletIcon size={18} />
            </span>
            <span>
              Split<span className="brand-accent">Ease</span>
            </span>
          </Link>
          <UserMenu />
        </div>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<GroupsPage />} />
          <Route path="/groups/:groupId" element={<GroupPage />} />
          <Route
            path="*"
            element={
              <div className="empty-state">
                <h2>Page not found</h2>
                <Link to="/">Go to your groups</Link>
              </div>
            }
          />
        </Routes>
      </main>
    </>
  );
}

function UserMenu() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const close = (e: MouseEvent) => {
      if (!ref.current?.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  if (!user) return null;

  return (
    <div className="user-menu" ref={ref}>
      <button type="button" className="user-button" onClick={() => setOpen((v) => !v)} aria-expanded={open}>
        <Avatar name={user.displayName} size="sm" />
        <span className="user-button-name">{user.displayName}</span>
        <span className="caret" aria-hidden="true">▾</span>
      </button>
      {open && (
        <div className="user-dropdown" role="menu">
          <div className="user-dropdown-head">
            <Avatar name={user.displayName} />
            <div>
              <div className="strong">{user.displayName}</div>
              <div className="muted small">@{user.username}</div>
            </div>
          </div>
          <button
            type="button"
            role="menuitem"
            className="logout-button"
            disabled={loggingOut}
            onClick={async () => {
              setLoggingOut(true);
              navigate('/');
              await logout();
            }}
          >
            {loggingOut ? 'Logging out…' : 'Log out'}
          </button>
        </div>
      )}
    </div>
  );
}
