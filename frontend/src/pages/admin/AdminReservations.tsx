import { useState } from 'react';
import { XCircle } from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { errorMessage } from '../../api/client';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Button, Card, ConfirmDialog, EmptyState, ErrorState, Field, Input, PageHeader, Select, Spinner, StatusBadge, Table, Td } from '../../components/ui';
import { dateOnly, timeOnly } from '../../utils/format';
import type { Reservation, ReservationStatus } from '../../types';

const statuses: ReservationStatus[] = ['CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED'];

export default function AdminReservations() {
  const toast = useToast();
  const [status, setStatus] = useState<ReservationStatus | ''>('CONFIRMED');
  const reservations = useAsync(() => adminApi.reservations(status), [status]);
  const [cancelling, setCancelling] = useState<Reservation | null>(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);

  const cancel = async () => {
    if (!cancelling) return;
    setBusy(true);
    try {
      await adminApi.cancelReservation(cancelling.id, reason || undefined);
      toast(`Reservation #${cancelling.id} cancelled; the customer has been notified`);
      setCancelling(null);
      void reservations.reload(true);
    } catch (e) {
      toast(errorMessage(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="animate-fade-in">
      <PageHeader title="Reservations" subtitle="All bookings across the network."
        actions={<Select value={status} onChange={(e) => setStatus(e.target.value as ReservationStatus | '')} className="w-48" aria-label="Status filter">
          <option value="">All statuses</option>
          {statuses.map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
        </Select>} />
      {reservations.loading ? <Spinner /> : reservations.error ? <ErrorState message={reservations.error} onRetry={reservations.reload} /> : (
        <Card>
          {reservations.data?.length === 0 ? <EmptyState title="No reservations with this status" /> : (
            <Table head={['#', 'Slot', 'Customer', 'Vehicle', 'Station / charger', 'Status', '']}>
              {reservations.data?.map((r) => (
                <tr key={r.id} className="hover:bg-ink-50/60">
                  <Td className="tabular text-ink-500">{r.id}</Td>
                  <Td className="tabular"><p className="font-medium text-ink-900">{dateOnly(r.startTime)}</p><p className="text-xs text-ink-500">{timeOnly(r.startTime)}–{timeOnly(r.endTime)}</p></Td>
                  <Td><p className="font-medium text-ink-900">{r.customerName}</p><p className="text-xs text-ink-500">{r.customerEmail}</p></Td>
                  <Td>{r.vehicleName}<p className="text-xs text-ink-500">{r.registrationNumber}</p></Td>
                  <Td>{r.stationName}<p className="text-xs text-ink-500">{r.chargerCode}</p></Td>
                  <Td><StatusBadge status={r.status} /></Td>
                  <Td>{r.canCancel && <Button size="sm" variant="ghost" icon={XCircle} className="text-red-600 hover:bg-red-50" onClick={() => { setCancelling(r); setReason(''); }}>Cancel</Button>}</Td>
                </tr>
              ))}
            </Table>
          )}
        </Card>
      )}
      <ConfirmDialog open={!!cancelling} danger title={`Cancel reservation #${cancelling?.id}?`} confirmLabel="Cancel reservation" loading={busy} onConfirm={cancel} onClose={() => setCancelling(null)}
        message={<div className="space-y-4"><p>{cancelling?.customerName}'s booking on {cancelling?.chargerCode} will be cancelled and the charger released.</p>
          <Field label="Reason shown to the customer" htmlFor="r"><Input id="r" value={reason} placeholder="Cancelled by VoltX operations" onChange={(e) => setReason(e.target.value)} /></Field></div>} />
    </div>
  );
}
