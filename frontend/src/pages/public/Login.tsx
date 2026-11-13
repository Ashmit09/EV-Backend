import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { LogIn } from 'lucide-react';
import { AuthShell } from './AuthShell';
import { Button, Field, Input } from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { errorMessage } from '../../api/client';

const demoAccounts = [
  { label: 'Customer demo', email: 'aarav@voltx.in', password: 'Customer@123' },
  { label: 'Admin demo', email: 'admin@voltx.in', password: 'Admin@123' },
];

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const user = await login(email, password);
      const from = (location.state as { from?: string } | null)?.from;
      const home = user.role === 'ADMIN' ? '/admin' : '/app';
      navigate(from && from.startsWith(home) ? from : home, { replace: true });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title="Welcome back" subtitle="Sign in to manage your charging."
      footer={<>New to VoltX? <Link to="/register" className="font-semibold text-brand-700 hover:underline">Create an account</Link></>}>
      <form onSubmit={submit} className="space-y-4" noValidate>
        {error && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700" role="alert">{error}</div>}
        <Field label="Email" htmlFor="email">
          <Input id="email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </Field>
        <Field label="Password" htmlFor="password">
          <Input id="password" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <Button type="submit" className="w-full" loading={loading} icon={LogIn}>Sign in</Button>
      </form>
      <div className="mt-6 rounded-xl border border-dashed border-ink-200 p-4">
        <p className="text-xs font-semibold uppercase tracking-wide text-ink-500">Demo accounts</p>
        <div className="mt-3 grid grid-cols-2 gap-2">
          {demoAccounts.map((account) => (
            <Button key={account.email} type="button" variant="secondary" size="sm"
              onClick={() => { setEmail(account.email); setPassword(account.password); }}>
              {account.label}
            </Button>
          ))}
        </div>
      </div>
    </AuthShell>
  );
}
