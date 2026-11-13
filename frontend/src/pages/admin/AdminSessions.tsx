import { useState } from 'react';
import { BatteryCharging, Square } from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { errorMessage } from '../../api/client';
import { useAsync, useInterval } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { BatteryBar, Button, Card, ConfirmDialog, EmptyState, ErrorState, PageHeader, Spinner } from '../../components/ui';
import { kwh, minutes, money, pct, timeOnly } from '../../utils/format';
import type { Session } from '../../types';

export default function AdminSessions() {
  const toast = useToast();
  const sessions = useAsync(() => adminApi.activeSessions(), []);
  const [stopping, setStopping] = useState<Session | null>(null);
  const [busy, setBusy] = useState(false);
  useInterval(() => { void sessions.reload(true); }, 3000);

  const stop = async () => {
    if (!stopping) return;
    setBusy(true);
    try {
      await adminApi.stopSession(stopping.id);
      toast(`Session #${stopping.id} stopped and invoiced`);
      setStopping(null);
      void sessions.reload(true);
    } catch (e) {
      toast(errorMessage(e), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="animate-fade-in">
      <PageHeader title="Live sessions" subtitle="Refreshes every 3 seconds with server-computed progress." />
      {sessions.loading ? <Spinner /> : sessions.error ? <ErrorState message={sessions.error} onRetry={sessions.reload} /> :
        sessions.data?.length === 0 ? <Card><EmptyState icon={BatteryCharging} title="No vehicles are charging right now" /></Card> : (
          <div className="grid gap-4 lg:grid-cols-2">
            {sessions.data?.map((s) => (
              <Card key={s.id} className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-ink-900">{s.vehicleName} <span className="font-normal text-ink-500">· {s.registrationNumber}</span></p>
                    <p className="text-sm text-ink-500">{s.customerName} · {s.stationName} · {s.chargerCode}</p>
                  </div>
                  <Button size="sm" variant="danger" icon={Square} onClick={() => setStopping(s)}>Stop</Button>
                </div>
                <div className="mt-4 flex items-center gap-3">
                  <BatteryBar value={s.currentBatteryPercentage} target={s.targetBatteryPercentage} />
                  <span className="tabular w-14 text-right text-sm font-semibold">{pct(s.currentBatteryPercentage)}</span>
                </div>
                <div className="mt-4 grid grid-cols-4 gap-2 text-sm">
                  <div><p className="text-xs text-ink-500">Energy</p><p className="tabular font-semibold">{kwh(s.energyKwh)}</p></div>
                  <div><p className="text-xs text-ink-500">Cost</p><p className="tabular font-semibold">{money(s.cost)}</p></div>
                  <div><p className="text-xs text-ink-500">Power</p><p className="tabular font-semibold">{s.currentPowerKw} kW</p></div>
                  <div><p className="text-xs text-ink-500">Elapsed</p><p className="tabular font-semibold">{minutes(s.simulatedMinutes)}</p></div>
                </div>
                <p className="mt-3 text-xs text-ink-400">Started {timeOnly(s.startTime)}{s.reservationEndTime && ` · slot ends ${timeOnly(s.reservationEndTime)}`}</p>
              </Card>
            ))}
          </div>
        )}
      <ConfirmDialog open={!!stopping} danger title="Stop this session?" confirmLabel="Stop session" loading={busy} onConfirm={stop} onClose={() => setStopping(null)}
        message={<>The customer will be billed for energy delivered so far ({kwh(stopping?.energyKwh)}) and notified that an operator stopped the session.</>} />
    </div>
  );
}
