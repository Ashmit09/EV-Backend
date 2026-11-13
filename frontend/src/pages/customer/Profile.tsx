import { useState, type FormEvent } from 'react';
import { KeyRound, Save } from 'lucide-react';
import { userApi } from '../../api/endpoints';
import { toApiError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Badge, Button, Card, CardHeader, Field, Input, PageHeader } from '../../components/ui';
import { dateOnly } from '../../utils/format';

export default function Profile() {
  const { user, setUser } = useAuth();
  const toast = useToast();
  const [profile, setProfile] = useState({ fullName: user?.fullName ?? '', phone: user?.phone ?? '' });
  const [profileErrors, setProfileErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [passwordErrors, setPasswordErrors] = useState<Record<string, string>>({});
  const [changing, setChanging] = useState(false);

  const saveProfile = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true); setProfileErrors({});
    try {
      setUser(await userApi.update(profile));
      toast('Profile updated');
    } catch (err) {
      const apiError = toApiError(err);
      setProfileErrors(apiError.fieldErrors);
      toast(apiError.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  const changePassword = async (e: FormEvent) => {
    e.preventDefault();
    if (passwords.newPassword !== passwords.confirm) {
      setPasswordErrors({ confirm: 'Passwords do not match' });
      return;
    }
    setChanging(true); setPasswordErrors({});
    try {
      await userApi.changePassword({ currentPassword: passwords.currentPassword, newPassword: passwords.newPassword });
      setPasswords({ currentPassword: '', newPassword: '', confirm: '' });
      toast('Password changed');
    } catch (err) {
      const apiError = toApiError(err);
      setPasswordErrors(apiError.fieldErrors);
      toast(apiError.message, 'error');
    } finally {
      setChanging(false);
    }
  };

  return (
    <div className="animate-fade-in max-w-3xl">
      <PageHeader title="Profile" subtitle={`Member since ${dateOnly(user?.createdAt)}`} actions={<Badge tone="dark">{user?.role}</Badge>} />
      <Card className="mb-6">
        <CardHeader title="Personal details" />
        <form onSubmit={saveProfile} className="grid gap-4 p-5 sm:grid-cols-2">
          <Field label="Full name" htmlFor="fn" error={profileErrors.fullName}><Input id="fn" value={profile.fullName} onChange={(e) => setProfile({ ...profile, fullName: e.target.value })} /></Field>
          <Field label="Phone" htmlFor="ph" error={profileErrors.phone}><Input id="ph" value={profile.phone} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} /></Field>
          <Field label="Email" htmlFor="em" hint="Email is your login and cannot be changed"><Input id="em" value={user?.email ?? ''} disabled /></Field>
          <div className="flex items-end justify-end"><Button type="submit" icon={Save} loading={saving}>Save details</Button></div>
        </form>
      </Card>
      <Card>
        <CardHeader title="Change password" subtitle="Passwords are stored only as BCrypt hashes." />
        <form onSubmit={changePassword} className="grid gap-4 p-5 sm:grid-cols-3">
          <Field label="Current password" htmlFor="cp" error={passwordErrors.currentPassword}><Input id="cp" type="password" autoComplete="current-password" value={passwords.currentPassword} onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })} /></Field>
          <Field label="New password" htmlFor="np" error={passwordErrors.newPassword}><Input id="np" type="password" autoComplete="new-password" value={passwords.newPassword} onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })} /></Field>
          <Field label="Confirm" htmlFor="cf" error={passwordErrors.confirm}><Input id="cf" type="password" autoComplete="new-password" value={passwords.confirm} onChange={(e) => setPasswords({ ...passwords, confirm: e.target.value })} /></Field>
          <div className="flex justify-end sm:col-span-3"><Button type="submit" variant="dark" icon={KeyRound} loading={changing} disabled={!passwords.currentPassword || !passwords.newPassword}>Change password</Button></div>
        </form>
      </Card>
    </div>
  );
}
