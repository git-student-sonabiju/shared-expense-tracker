import { useState, type FormEvent } from 'react';
import { errorMessage } from '../api';
import { useAuth } from '../auth';
import ErrorBanner from '../components/ErrorBanner';
import { CheckCircleIcon, WalletIcon } from '../components/Icons';

type Mode = 'login' | 'register';

export default function LoginPage() {
  const { login, register, notice } = useAuth();
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function switchMode(next: Mode) {
    setMode(next);
    setError(null);
  }

  function validate(): string | null {
    if (!username.trim()) return 'Username is required';
    if (!password) return 'Password is required';
    if (mode === 'register') {
      if (!displayName.trim()) return 'Your name is required';
      if (password.length < 8) return 'Password must be at least 8 characters';
    }
    return null;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const problem = validate();
    if (problem) {
      setError(problem);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      if (mode === 'login') {
        await login(username.trim(), password);
      } else {
        await register(username.trim(), displayName.trim(), password);
      }
    } catch (e) {
      setError(errorMessage(e));
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <aside className="auth-aside">
        <div className="brand brand-light">
          <span className="brand-mark brand-mark-light">
            <WalletIcon size={18} />
          </span>
          <span>SplitEase</span>
        </div>
        <div>
          <h1>Split expenses, not friendships.</h1>
          <ul className="auth-points">
            <li>
              <CheckCircleIcon size={18} /> Equal or exact splits, accurate to the paisa
            </li>
            <li>
              <CheckCircleIcon size={18} /> Live balances for everyone in the group
            </li>
            <li>
              <CheckCircleIcon size={18} /> Settle up in the fewest possible payments
            </li>
          </ul>
        </div>
        <p className="auth-foot">Your groups are private to your account.</p>
      </aside>

      <main className="auth-main">
        <div className="auth-card">
          <h2>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h2>
          <p className="muted">{mode === 'login' ? 'Log in to see your groups.' : 'It only takes a few seconds.'}</p>

          <div className="auth-tabs" role="tablist">
            <button type="button" role="tab" aria-selected={mode === 'login'} className={mode === 'login' ? 'active' : ''} onClick={() => switchMode('login')}>
              Log in
            </button>
            <button type="button" role="tab" aria-selected={mode === 'register'} className={mode === 'register' ? 'active' : ''} onClick={() => switchMode('register')}>
              Sign up
            </button>
          </div>

          {notice && mode === 'login' && <div className="notice">{notice}</div>}

          <form onSubmit={handleSubmit} className="stack" noValidate>
            {mode === 'register' && (
              <label>
                Your name
                <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} maxLength={60} placeholder="Sona Biju" autoComplete="name" />
              </label>
            )}
            <label>
              Username
              <input value={username} onChange={(e) => setUsername(e.target.value)} maxLength={30} placeholder="sona" autoComplete="username" autoFocus />
            </label>
            <label>
              Password
              <span className="password-field">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  maxLength={100}
                  placeholder={mode === 'register' ? 'At least 8 characters' : '••••••••'}
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                />
                <button type="button" className="password-toggle" onClick={() => setShowPassword((v) => !v)}>
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </span>
            </label>
            <ErrorBanner message={error} />
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Create account'}
            </button>
          </form>

          <p className="auth-switch muted small">
            {mode === 'login' ? "Don't have an account? " : 'Already have an account? '}
            <button type="button" className="text-link" onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}>
              {mode === 'login' ? 'Sign up' : 'Log in'}
            </button>
          </p>
        </div>
      </main>
    </div>
  );
}
