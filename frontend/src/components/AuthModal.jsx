import { useState } from 'react';
import { Loader2, LogIn, UserPlus, X } from 'lucide-react';
import { useAuth } from '../auth/useAuth';
import T from './T';

/**
 * Login / register modal. Toggles between the two modes; on success it calls onSuccess
 * (the shell closes it and may navigate to the watchlist).
 */
export default function AuthModal({ onClose, onSuccess }) {
  const { login, register } = useAuth();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ firstname: '', lastname: '', email: '', password: '' });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const isLogin = mode === 'login';
  const set = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      if (isLogin) {
        await login(form.email, form.password);
      } else {
        await register({
          firstname: form.firstname,
          lastname: form.lastname,
          email: form.email,
          password: form.password,
        });
      }
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.message || 'Something went wrong');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
      onMouseDown={onClose}
    >
      <div
        className="w-full max-w-md bg-surface-container rounded-2xl shadow-2xl border border-outline-variant/20 p-8 relative"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button
          type="button"
          onClick={onClose}
          aria-label="Close"
          className="absolute top-4 right-4 text-on-surface-variant hover:text-on-surface transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <h2 className="font-headline text-2xl font-bold mb-1">
          <T>{isLogin ? 'Welcome back' : 'Create account'}</T>
        </h2>
        <p className="text-sm text-on-surface-variant mb-6">
          <T>{isLogin ? 'Log in to view your watchlist.' : 'Sign up to start tracking stocks.'}</T>
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          {!isLogin && (
            <div className="flex gap-3">
              <Field placeholder="First name" value={form.firstname} onChange={set('firstname')} required />
              <Field placeholder="Last name" value={form.lastname} onChange={set('lastname')} required />
            </div>
          )}
          <Field type="email" placeholder="Email" value={form.email} onChange={set('email')} required />
          <Field
            type="password"
            placeholder="Password"
            value={form.password}
            onChange={set('password')}
            required
            minLength={isLogin ? undefined : 8}
          />

          {error && <p className="text-error text-sm">{error}</p>}

          <button
            type="submit"
            disabled={submitting}
            className="mt-2 flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-primary text-on-primary font-bold text-sm uppercase tracking-wider hover:brightness-110 active:scale-95 transition-all disabled:opacity-60"
          >
            {submitting ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : isLogin ? (
              <LogIn className="w-4 h-4" />
            ) : (
              <UserPlus className="w-4 h-4" />
            )}
            <T>{isLogin ? 'Log in' : 'Sign up'}</T>
          </button>
        </form>

        <p className="text-sm text-on-surface-variant mt-5 text-center">
          {isLogin ? 'No account yet?' : 'Already have an account?'}{' '}
          <button
            type="button"
            onClick={() => { setMode(isLogin ? 'register' : 'login'); setError(null); }}
            className="text-primary font-semibold hover:underline"
          >
            {isLogin ? 'Sign up' : 'Log in'}
          </button>
        </p>
      </div>
    </div>
  );
}

function Field(props) {
  return (
    <input
      {...props}
      className="w-full bg-surface-container-lowest rounded-lg px-4 py-3 text-sm text-on-surface placeholder:text-on-surface-variant border border-outline/40 focus:border-primary outline-none transition-colors"
    />
  );
}
