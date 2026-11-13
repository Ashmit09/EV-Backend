import { useMemo, useState } from 'react';
import { Search, UserCheck, UserX } from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { errorMessage } from '../../api/client';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Badge, Button, Card, ConfirmDialog, ErrorState, Input, PageHeader, Spinner, StatusBadge, Table, Td } from '../../components/ui';
import { dateOnly } from '../../utils/format';
import type { User } from '../../types';

export default function AdminCustomers() {
  const toast = useToast();
  const users = useAsync(() => adminApi.users(), []);
  const [query, setQuery] = useState('');
  const [toggling, setToggling] = useState<User | null>(null);
  const [busy, setBusy] = useState(false);

  const filtered = useMemo(() => (users.data ?? []).filter((u) =>
    `${u.fullName} ${u.email} ${u.phone ?? ''}`.toLowerCase().includes(query.toLowerCase())), [users.data, query]);

  const toggle = async () => {
    if (!toggling) return;
    setBusy(true);
    try {
      await adminApi.setUserEnabled(toggling.id, !toggling.enabled);
      toast(`${toggling.fullName} ${toggling.enabled ? 'disabled' : 'enabled'}`);
      setToggling(null);
      void users.reload(true);
    } catch (e) {
      toast(errorMessage(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="animate-fade-in">
      <PageHeader title="Customers" subtitle="Disabled accounts cannot sign in, and their existing tokens stop working immediately."
        actions={<div className="relative w-72"><Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-400" />
          <Input className="pl-9" placeholder="Search name, email or phone" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Search users" /></div>} />
      {users.loading ? <Spinner /> : users.error ? <ErrorState message={users.error} onRetry={users.reload} /> : (
        <Card>
          <Table head={['Name', 'Email', 'Phone', 'Role', 'Joined', 'Status', '']}>
            {filtered.map((u) => (
              <tr key={u.id} className="hover:bg-ink-50/60">
                <Td className="font-medium text-ink-900">{u.fullName}</Td>
                <Td>{u.email}</Td>
                <Td>{u.phone ?? '—'}</Td>
                <Td><Badge tone={u.role === 'ADMIN' ? 'dark' : 'gray'}>{u.role}</Badge></Td>
                <Td>{dateOnly(u.createdAt)}</Td>
                <Td><StatusBadge status={u.enabled ? 'ACTIVE' : 'DISABLED'} /></Td>
                <Td>{u.role === 'CUSTOMER' && (
                  <Button size="sm" variant={u.enabled ? 'ghost' : 'secondary'} icon={u.enabled ? UserX : UserCheck}
                    className={u.enabled ? 'text-red-600 hover:bg-red-50' : ''} onClick={() => setToggling(u)}>{u.enabled ? 'Disable' : 'Enable'}</Button>
                )}</Td>
              </tr>
            ))}
          </Table>
        </Card>
      )}
      <ConfirmDialog open={!!toggling} danger={toggling?.enabled} loading={busy} onConfirm={toggle} onClose={() => setToggling(null)}
        title={toggling?.enabled ? 'Disable account?' : 'Enable account?'} confirmLabel={toggling?.enabled ? 'Disable account' : 'Enable account'}
        message={toggling?.enabled ? <>{toggling?.fullName} will be signed out and unable to log in.</> : <>{toggling?.fullName} will be able to sign in again.</>} />
    </div>
  );
}
