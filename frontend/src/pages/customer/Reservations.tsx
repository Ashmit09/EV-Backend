import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CalendarClock, MapPin, Play, XCircle, Zap } from 'lucide-react';
import { reservationApi, sessionApi } from '../../api/endpoints';
import { errorMessage } from '../../api/client';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Button, Card, ConfirmDialog, EmptyState, ErrorState, Field, Input, PageHeader, Spinner, StatusBadge } from '../../components/ui';
import { connectorLabel, dateOnly, timeOnly } from '../../utils/format';
import type { Reservation } from '../../types';

export default function Reservations() {
  const toast = useToast();
  const navigate = useNavigate();
  const reservations = useAsync(() => reservationApi.mine(), []);
  const [cancelling, setCancelling] = useState<Reservation | null>(null);
  const [reason, setReason] = useState('');
  const [starting, setStarting] = useState<Reservation | null>(null);
  const [target, setTarget] = useState(80);
  const [busy, setBusy] = useState(false);

  const cancel = async () => {
    if (!cancelling) return;
    setBusy(true);
    try {
      await reservationApi.cancel(cancelling.id, reason || undefined);
      toast('Reservation cancelled and charger released');
      setCancelling(null);
      void reservations.reload(true);
    } catch (e) {
      toast(errorMessage(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  const start = async () => {
    if (!starting) return;
    setBusy(true);
    try {
      await sessionApi.start(starting.id, target);
      toast('Charging started');
      navigate('/app/session');
    } catch (e) {
      toast(errorMessage(e), 'error');
      setStarting(null);
      void reservations.reload(true);
    } finally {
      setBusy(false);
    }
  };

  const upcoming = reservations.data?.filter((r) => r.status === 'CONFIRMED' || r.status === 'ACTIVE') ?? [];
  const past = reservations.data?.filter((r) => r.status !== 'CONFIRMED' && r.status !== 'ACTIVE') ?? [];

  return (
    <div className="animate-fade-in">
      <PageHeader title="My bookings" subtitle="Start charging within 15 minutes of your slot, or cancel to free the charger."
        actions={<Link to="/app/stations"><Button icon={CalendarClock}>New booking</Button></Link>} />

      {reservations.loading ? <Spinner /> : reservations.error ? <ErrorState message={reservations.error} onRetry={reservations.reload} /> : (
        <>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-ink-500">Upcoming & active</h2>
          {upcoming.length === 0 ? (
            <Card className="mb-8"><EmptyState icon={CalendarClock} title="No upcoming bookings" description="Find a station and reserve a compatible charger." /></Card>
          ) : (
            <div className="mb-8 grid gap-4 lg:grid-cols-2">
              {upcoming.map((r) => (
                <Card key={r.id} className="p-5">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-xs font-medium text-ink-500">Booking #{r.id}</p>
                      <p className="mt-0.5 text-lg font-bold tabular">{dateOnly(r.startTime)} · {timeOnly(r.startTime)}–{timeOnly(r.endTime)}</p>
                    </div>
                    <StatusBadge status={r.status} />
                  </div>
                  <div className="mt-4 grid gap-2 text-sm sm:grid-cols-2">
                    <p className="flex items-center gap-2 text-ink-600"><MapPin className="size-4 text-ink-400" />{r.stationName}, {r.city}</p>
                    <p className="flex items-center gap-2 text-ink-600"><Zap className="size-4 text-ink-400" />{r.chargerCode} · {connectorLabel[r.connectorType]} · {r.maxPowerKw} kW</p>
                    <p className="text-ink-600 sm:col-span-2">{r.vehicleName} ({r.registrationNumber})</p>
                  </div>
                  <div className="mt-5 flex flex-wrap gap-2 border-t border-ink-100 pt-4">
                    {r.status === 'ACTIVE' && <Link to="/app/session"><Button icon={Zap}>View live session</Button></Link>}
                    {r.canStart && <Button icon={Play} onClick={() => { setStarting(r); setTarget(80); }}>Start charging</Button>}
                    {!r.canStart && r.status === 'CONFIRMED' && <p className="self-center text-xs text-ink-500">Charging can start 15 minutes before the slot.</p>}
                    {r.canCancel && <Button variant="secondary" icon={XCircle} onClick={() => { setCancelling(r); setReason(''); }}>Cancel</Button>}
                  </div>
                </Card>
              ))}
            </div>
          )}

          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-ink-500">Past bookings</h2>
          <Card>
            {past.length === 0 ? <EmptyState title="Nothing here yet" /> : (
              <div className="divide-y divide-ink-100">
                {past.slice(0, 30).map((r) => (
                  <div key={r.id} className="flex flex-wrap items-center gap-x-6 gap-y-1 px-5 py-3.5 text-sm">
                    <span className="w-40 font-medium tabular text-ink-900">{dateOnly(r.startTime)} {timeOnly(r.startTime)}</span>
                    <span className="min-w-[160px] flex-1 text-ink-600">{r.stationName} · {r.chargerCode}</span>
                    <span className="text-ink-500">{r.vehicleName}</span>
                    {r.cancellationReason && <span className="text-xs text-ink-400">“{r.cancellationReason}”</span>}
                    <StatusBadge status={r.status} />
                  </div>
                ))}
              </div>
            )}
          </Card>
        </>
      )}

      <ConfirmDialog open={!!cancelling} danger title="Cancel this booking?" confirmLabel="Cancel booking" loading={busy} onConfirm={cancel} onClose={() => setCancelling(null)}
        message={<div className="space-y-4">
          <p>{cancelling?.chargerCode} on {dateOnly(cancelling?.startTime)} at {timeOnly(cancelling?.startTime)} will be released for other drivers.</p>
          <Field label="Reason (optional)" htmlFor="reason"><Input id="reason" value={reason} maxLength={255} onChange={(e) => setReason(e.target.value)} /></Field>
        </div>} />

      <ConfirmDialog open={!!starting} title="Start charging" confirmLabel="Start charging" loading={busy} onConfirm={start} onClose={() => setStarting(null)}
        message={<div className="space-y-4">
          <p>Plug <b>{starting?.vehicleName}</b> into <b>{starting?.chargerCode}</b>. Charging stops automatically at your target or when the slot ends.</p>
          <Field label={`Target battery: ${target}%`} htmlFor="target" hint="Cars charging on DC usually stop at 80% to protect the battery.">
            <input id="target" type="range" min={10} max={100} step={5} value={target} className="w-full accent-brand-600" onChange={(e) => setTarget(Number(e.target.value))} />
          </Field>
        </div>} />
    </div>
  );
}
