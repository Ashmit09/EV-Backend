import { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Badge, Button, Card, EmptyState, ErrorState, PageHeader, Select, Spinner, Table, Td } from '../../components/ui';
import { dateTime, humanize } from '../../utils/format';

const actions = ['USER_REGISTERED', 'USER_LOGIN', 'PROFILE_UPDATED', 'PASSWORD_CHANGED', 'USER_ENABLED', 'USER_DISABLED', 'VEHICLE_ADDED',
  'VEHICLE_UPDATED', 'VEHICLE_REMOVED', 'STATION_CREATED', 'STATION_UPDATED', 'STATION_ENABLED', 'STATION_DISABLED', 'CHARGER_CREATED',
  'CHARGER_UPDATED', 'CHARGER_STATUS_CHANGED', 'RESERVATION_CREATED', 'RESERVATION_CANCELLED', 'RESERVATION_EXPIRED', 'SESSION_STARTED',
  'SESSION_COMPLETED', 'INVOICE_GENERATED', 'PAYMENT_PROCESSED'];

export default function AdminAudit() {
  const [page, setPage] = useState(0);
  const [action, setAction] = useState('');
  const logs = useAsync(() => adminApi.auditLogs(page, action), [page, action]);

  return (
    <div className="animate-fade-in">
      <PageHeader title="Audit logs" subtitle="Append-only record of every significant action, written in the same transaction as the change."
        actions={<Select value={action} onChange={(e) => { setAction(e.target.value); setPage(0); }} className="w-64" aria-label="Filter by action">
          <option value="">All actions</option>
          {actions.map((a) => <option key={a} value={a}>{humanize(a)}</option>)}
        </Select>} />
      {logs.loading ? <Spinner /> : logs.error ? <ErrorState message={logs.error} onRetry={logs.reload} /> : (
        <Card>
          {logs.data?.content.length === 0 ? <EmptyState title="No audit entries" /> : (
            <Table head={['Time', 'Actor', 'Action', 'Entity', 'Details']}>
              {logs.data?.content.map((l) => (
                <tr key={l.id} className="hover:bg-ink-50/60">
                  <Td className="tabular">{dateTime(l.createdAt)}</Td>
                  <Td>{l.actorEmail}</Td>
                  <Td><Badge tone={l.action.includes('CANCEL') || l.action.includes('DISABLED') ? 'red' : l.action.startsWith('SESSION') ? 'blue' : 'gray'}>{humanize(l.action)}</Badge></Td>
                  <Td>{l.entityType}{l.entityId ? ` #${l.entityId}` : ''}</Td>
                  <Td className="max-w-md truncate whitespace-normal text-ink-600">{l.details}</Td>
                </tr>
              ))}
            </Table>
          )}
          {logs.data && logs.data.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-ink-100 px-5 py-3 text-sm">
              <span className="text-ink-500">Page {logs.data.page + 1} of {logs.data.totalPages} · {logs.data.totalElements} entries</span>
              <div className="flex gap-2">
                <Button size="sm" variant="secondary" icon={ChevronLeft} disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
                <Button size="sm" variant="secondary" disabled={page + 1 >= logs.data.totalPages} onClick={() => setPage(page + 1)}>Next <ChevronRight className="size-4" /></Button>
              </div>
            </div>
          )}
        </Card>
      )}
    </div>
  );
}
