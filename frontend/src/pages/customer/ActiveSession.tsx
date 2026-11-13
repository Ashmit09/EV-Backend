import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BatteryCharging, Clock, Gauge, IndianRupee, MapPin, Square, Zap } from 'lucide-react';
import { sessionApi } from '../../api/endpoints';
import { errorMessage } from '../../api/client';
import { useAsync, useInterval } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Button, Card, ConfirmDialog, EmptyState, ErrorState, PageHeader, Spinner } from '../../components/ui';
import { kwh, minutes, money, pct, timeOnly } from '../../utils/format';

/**
 * Live view. The page only polls GET /sessions/active every 2 s; battery, energy and cost are
 * computed by the backend simulator — the browser never calculates charging cost itself.
 */
export default function ActiveSession() {
  const toast = useToast();
  const navigate = useNavigate();
  const session = useAsync(() => sessionApi.active(), []);
  const [confirmStop, setConfirmStop] = useState(false);
  const [stopping, setStopping] = useState(false);

  useInterval(() => { void session.reload(true); }, session.data ? 2000 : null);

  const stop = async () => {
    if (!session.data) return;
    setStopping(true);
    try {
      const done = await sessionApi.stop(session.data.id);
      toast(`Charging complete: ${kwh(done.energyKwh)} for ${money(done.cost)} (before GST)`);
      navigate(done.invoiceId ? `/app/invoices/${done.invoiceId}` : '/app/history');
    } catch (e) {
      toast(errorMessage(e), 'error');
      setConfirmStop(false);
      void session.reload(true);
    } finally {
      setStopping(false);
    }
  };

  if (session.loading) return <Spinner />;
  if (session.error) return <ErrorState message={session.error} onRetry={session.reload} />;
  if (!session.data) {
    return (
      <div className="animate-fade-in">
        <PageHeader title="Live charging" />
        <Card><EmptyState icon={BatteryCharging} title="No active charging session"
          description="Start charging from one of your bookings. Finished sessions appear in your history."
          action={<div className="flex gap-2"><Link to="/app/reservations"><Button>My bookings</Button></Link><Link to="/app/history"><Button variant="secondary">History</Button></Link></div>} /></Card>
      </div>
    );
  }

  const s = session.data;
  const progress = Math.min(100, s.currentBatteryPercentage);
  const radius = 92;
  const circumference = 2 * Math.PI * radius;

  return (
    <div className="animate-fade-in">
      <PageHeader title="Live charging" subtitle={`${s.vehicleName} · ${s.registrationNumber}`}
        actions={<Button variant="danger" icon={Square} onClick={() => setConfirmStop(true)}>Stop charging</Button>} />

      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="flex flex-col items-center justify-center bg-ink-950 p-8 text-white lg:col-span-2">
          <div className="relative">
            <svg width="220" height="220" viewBox="0 0 220 220" className="-rotate-90">
              <circle cx="110" cy="110" r={radius} stroke="#252a33" strokeWidth="16" fill="none" />
              <circle cx="110" cy="110" r={radius} stroke="#3ed882" strokeWidth="16" fill="none" strokeLinecap="round"
                strokeDasharray={circumference} strokeDashoffset={circumference * (1 - progress / 100)} style={{ transition: 'stroke-dashoffset 1s ease' }} />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <BatteryCharging className={`size-7 text-brand-400 ${s.targetReached ? '' : 'animate-pulse'}`} />
              <p className="tabular mt-1 text-4xl font-extrabold">{pct(s.currentBatteryPercentage)}</p>
              <p className="text-xs text-ink-400">target {pct(s.targetBatteryPercentage)}</p>
            </div>
          </div>
          <p className="mt-6 text-sm text-ink-300">
            {s.targetReached ? 'Target reached — stop charging to get your invoice.' : `Charging at ${s.currentPowerKw} kW`}
          </p>
        </Card>

        <div className="grid gap-4 sm:grid-cols-2 lg:col-span-3">
          <Metric icon={Zap} label="Energy delivered" value={kwh(s.energyKwh)} />
          <Metric icon={IndianRupee} label="Running cost (pre-GST)" value={money(s.cost)} />
          <Metric icon={Clock} label="Charging time (simulated)" value={minutes(s.simulatedMinutes)} />
          <Metric icon={Gauge} label="Effective power" value={`${s.effectivePowerKw} kW`} />
          <Card className="p-5 sm:col-span-2">
            <div className="grid gap-3 text-sm sm:grid-cols-2">
              <p className="flex items-center gap-2 text-ink-600"><MapPin className="size-4 text-ink-400" />{s.stationName} · {s.chargerCode}</p>
              <p className="text-ink-600">Started at {timeOnly(s.startTime)}{s.reservationEndTime && ` · slot ends ${timeOnly(s.reservationEndTime)}`}</p>
              <p className="text-ink-600">Battery {pct(s.initialBatteryPercentage)} → {pct(s.currentBatteryPercentage)} of {s.batteryCapacityKwh} kWh</p>
              <p className="text-ink-600">Tariff {money(s.pricePerKwh)}/kWh · {s.pricingPlan.replace('_', ' ').toLowerCase()}</p>
            </div>
            <p className="mt-4 rounded-lg bg-ink-50 p-3 text-xs text-ink-500">
              Simulation: time is accelerated (1 real second ≈ 1 minute of charging). Progress is computed by the VoltX server; the session completes automatically at the target or when the slot ends.
            </p>
          </Card>
        </div>
      </div>

      <ConfirmDialog open={confirmStop} danger title="Stop charging?" confirmLabel="Stop and generate invoice" loading={stopping} onConfirm={stop} onClose={() => setConfirmStop(false)}
        message={<>Charging will stop at about <b>{pct(s.currentBatteryPercentage)}</b>. You will be billed for <b>{kwh(s.energyKwh)}</b> delivered so far (≈ {money(s.cost)} + GST).</>} />
    </div>
  );
}

function Metric({ icon: Icon, label, value }: { icon: typeof Zap; label: string; value: string }) {
  return (
    <Card className="p-5">
      <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wide text-ink-500"><Icon className="size-4 text-brand-600" />{label}</div>
      <p className="tabular mt-2 text-2xl font-bold text-ink-900">{value}</p>
    </Card>
  );
}
