import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserPlus } from 'lucide-react';
import { AuthShell } from './AuthShell';
import { Button, Field, Input } from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { toApiError } from '../../api/client';
import { useToast } from '../../context/ToastContext';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '', confirm: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [key]: e.target.value });

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const local: Record<string, string> = {};
    if (form.password !== form.confirm) local.confirm = 'Passwords do not match';
    if (form.password.length < 8) local.password = 'must be 8-64 characters';
    setErrors(local);
    if (Object.keys(local).length) return;

    setLoading(true);
    setFormError(null);
    try {
      await register({ fullName: form.fullName, email: form.email, password: form.password, phone: form.phone || undefined });
      toast('Account created. Add your EV to start booking.');
      navigate('/app/vehicles', { replace: true });
    } catch (e) {
      const apiError = toApiError(e);
      setErrors(apiError.fieldErrors);
      setFormError(apiError.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title="Create your account" subtitle="Start booking compatible chargers in minutes."
      footer={<>Already registered? <Link to="/login" className="font-semibold text-brand-700 hover:underline">Sign in</Link></>}>
      <form onSubmit={submit} className="space-y-4" noValidate>
        {formError && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700" role="alert">{formError}</div>}
        <Field label="Full name" htmlFor="name" error={errors.fullName}>
          <Input id="name" required value={form.fullName} onChange={set('fullName')} invalid={!!errors.fullName} />
        </Field>
        <Field label="Email" htmlFor="email" error={errors.email}>
          <Input id="email" type="email" required value={form.email} onChange={set('email')} invalid={!!errors.email} />
        </Field>
        <Field label="Phone (optional)" htmlFor="phone" error={errors.phone} hint="Used for SMS charging alerts">
          <Input id="phone" inputMode="tel" value={form.phone} onChange={set('phone')} invalid={!!errors.phone} />
        </Field>
        <Field label="Password" htmlFor="password" error={errors.password} hint="At least 8 characters with a letter and a digit">
          <Input id="password" type="password" autoComplete="new-password" required value={form.password} onChange={set('password')} invalid={!!errors.password} />
        </Field>
        <Field label="Confirm password" htmlFor="confirm" error={errors.confirm}>
          <Input id="confirm" type="password" autoComplete="new-password" required value={form.confirm} onChange={set('confirm')} invalid={!!errors.confirm} />
        </Field>
        <Button type="submit" className="w-full" loading={loading} icon={UserPlus}>Create account</Button>
      </form>
    </AuthShell>
  );
}
