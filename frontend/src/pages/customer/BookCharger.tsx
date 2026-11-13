import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ArrowLeft, BatteryCharging, CalendarCheck, Clock, Info, Zap } from 'lucide-react';
import { reservationApi, stationApi, vehicleApi } from '../../api/endpoints';
import { errorMessage } from '../../api/client';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Button, Card, CardHeader, ConfirmDialog, ErrorState, Field, Input, PageHeader, Select, Spinner, StatusBadge } from '../../components/ui';
import { kwh, money, pct, timeOnly, toDateInput, toLocalIso } from '../../utils/format';
import type { CostEstimate } from '../../types';

const durations = Array.from({ length: 16 }, (_, i) => (i + 1) * 15);

function nextQuarter() {
  const d = new Date();
  d.setSeconds(0, 0);
  d.setMinutes(Math.ceil((d.getMinutes() + 1) / 15) * 15);
  return d;
}

export default function BookCharger() {
  const { chargerId } = useParams();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const toast = useToast();

  const charger = useAsync(() => stationApi.charger(Number(chargerId)), [chargerId]);
  const vehicles = useAsync(() => vehicleApi.list(), []);
  const initial = nextQuarter();
  const [vehicleId, setVehicleId] = useState<number>(Number(params.get('vehicle')) || 0);
  const [startNow, setStartNow] = useState(true);
  const [date, setDate] = useState(toDateInput(initial));
  const [time, setTime] = useState(toLocalIso(initial).slice(11, 16));
  const [duration, setDuration] = useState(60);
  const [estimate, setEstimate] = useState<CostEstimate | null>(null);
  const [estimateError, setEstimateError] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [booking, setBooking] = useState(false);

  useEffect(() => {
    if (!vehicleId && vehicles.data?.length) setVehicleId(vehicles.data[0].id);
  }, [vehicles.data, vehicleId]);

  const slotDate = startNow ? toDateInput(new Date()) : date;
  const slots = useAsync(() => stationApi.bookedSlots(Number(chargerId), slotDate), [chargerId, slotDate]);

  const startIso = useMemo(() => (startNow ? toLocalIso(new Date()) : `${date}T${time}:00`), [startNow, date, time]);

  useEffect(() => {
    if (!vehicleId) return;
    const handle = window.setTimeout(() => {
      reservationApi.estimate({ vehicleId, chargingUnitId: Number(chargerId), startTime: startIso, durationMinutes: duration })
        .then((e) => { setEstimate(e); setEstimateError(null); })
        .catch((e) => { setEstimate(null); setEstimateError(errorMessage(e)); });
    }, 250);
    return () => window.clearTimeout(handle);
  }, [vehicleId, chargerId, startIso, duration]);

  const book = async () => {
    setBooking(true);
    try {
      const reservation = await reservationApi.create({
        vehicleId, chargingUnitId: Number(chargerId), startTime: startNow ? toLocalIso(new Date()) : startIso, durationMinutes: duration,
      });
      toast(`Booked ${reservation.chargerCode} for ${timeOnly(reservation.startTime)}–${timeOnly(reservation.endTime)}`);
      navigate('/app/reservations');
    } catch (e) {
      toast(errorMessage(e), 'error');
      setConfirming(false);
      void slots.reload(true);
    } finally {
      setBooking(false);
    }
  };

  if (charger.loading || vehicles.loading) return <Spinner />;
  if (charger.error || !charger.data) return <ErrorState message={charger.error ?? 'Charger not found'} />;
  const c = charger.data;
  const vehicle = vehicles.data?.find((v) => v.id === vehicleId);

  return (
    <div className="animate-fade-in">
      <Link to={`/app/stations/${c.stationId}`} className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-ink-500 hover:text-ink-900"><ArrowLeft className="size-4" /> {c.stationName}</Link>
      <PageHeader title={`Book ${c.chargerCode}`} subtitle={`${c.connectorLabel} · ${c.maxPowerKw} kW ${c.dcFast ? 'DC fast' : 'AC'} · ${money(c.pricePerKwh)}/kWh · ${c.pricingPlanLabel} pricing`}
        actions={<StatusBadge status={c.status} />} />

      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <CardHeader title="Slot details" />
          <div className="grid gap-5 p-5 sm:grid-cols-2">
            <Field label="Vehicle" htmlFor="v">
              {vehicles.data?.length ? (
                <Select id="v" value={vehicleId} onChange={(e) => setVehicleId(Number(e.target.value))}>
                  {vehicles.data.map((v) => <option key={v.id} value={v.id}>{v.displayName} · {v.currentBatteryPercentage}%</option>)}
                </Select>
              ) : <p className="text-sm text-ink-500"><Link to="/app/vehicles" className="font-semibold text-brand-700">Add a vehicle</Link> first.</p>}
            </Field>
            <Field label="Duration" htmlFor="d" hint="15 minutes to 4 hours">
              <Select id="d" value={duration} onChange={(e) => setDuration(Number(e.target.value))}>
                {durations.map((d) => <option key={d} value={d}>{d < 60 ? `${d} min` : `${Math.floor(d / 60)}h${d % 60 ? ` ${d % 60}m` : ''}`}</option>)}
              </Select>
            </Field>
            <label className="flex cursor-pointer items-center gap-2 text-sm font-medium text-ink-700 sm:col-span-2">
              <input type="checkbox" className="size-4 accent-brand-600" checked={startNow} onChange={(e) => setStartNow(e.target.checked)} />
              Start now (charging can begin immediately after booking)
            </label>
            {!startNow && (<>
              <Field label="Date" htmlFor="date"><Input id="date" type="date" value={date} min={toDateInput(new Date())} onChange={(e) => setDate(e.target.value)} /></Field>
              <Field label="Start time" htmlFor="time" hint="15-minute steps"><Input id="time" type="time" step={900} value={time} onChange={(e) => setTime(e.target.value)} /></Field>
            </>)}
          </div>
          <div className="border-t border-ink-100 p-5">
            <p className="mb-3 flex items-center gap-2 text-sm font-semibold text-ink-800"><Clock className="size-4 text-ink-400" />Already booked on {slotDate}</p>
            {slots.data?.length ? (
              <div className="flex flex-wrap gap-2">
                {slots.data.map((s) => <span key={s.startTime} className="rounded-lg bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-800 ring-1 ring-amber-200">{timeOnly(s.startTime)} – {timeOnly(s.endTime)}</span>)}
              </div>
            ) : <p className="text-sm text-ink-500">No bookings yet — the whole day is open.</p>}
          </div>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader title="Charging estimate" subtitle="Simulated on the server for this vehicle and charger" />
          <div className="p-5">
            {estimateError ? (
              <div className="flex gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700"><Info className="mt-0.5 size-4 shrink-0" />{estimateError}</div>
            ) : !estimate ? <Spinner label="Calculating…" /> : (
              <div className="space-y-4">
                <div className="flex items-center gap-4 rounded-xl bg-ink-900 p-4 text-white">
                  <BatteryCharging className="size-8 text-brand-400" />
                  <div>
                    <p className="tabular text-2xl font-bold">{pct(estimate.startBatteryPercentage)} → {pct(estimate.estimatedEndBatteryPercentage)}</p>
                    <p className="text-xs text-ink-300">Target {estimate.targetBatteryPercentage}% · {estimate.simulationModel} · {estimate.effectivePowerKw} kW effective</p>
                  </div>
                </div>
                <dl className="space-y-2 text-sm">
                  <Row label="Estimated energy" value={kwh(estimate.estimatedEnergyKwh)} />
                  <Row label={`Energy × ${money(c.pricePerKwh)}/kWh`} value={money(estimate.energyCost)} />
                  {estimate.adjustmentLabel && <Row label={estimate.adjustmentLabel} value={money(estimate.adjustmentAmount)} />}
                  <Row label="GST (18%)" value={money(estimate.taxAmount)} />
                  <div className="flex justify-between border-t border-ink-100 pt-3 text-base font-bold"><dt>Estimated total</dt><dd className="tabular">{money(estimate.estimatedTotal)}</dd></div>
                </dl>
                <p className="flex gap-2 text-xs text-ink-500"><Zap className="size-3.5 shrink-0 text-brand-600" />You pay only for energy actually delivered; the final bill is calculated when charging stops.</p>
              </div>
            )}
            <Button className="mt-5 w-full" size="lg" icon={CalendarCheck} disabled={!vehicle || !!estimateError || !estimate || !c.bookable}
              onClick={() => setConfirming(true)}>Review booking</Button>
          </div>
        </Card>
      </div>

      <ConfirmDialog open={confirming} title="Confirm booking" confirmLabel="Confirm booking" loading={booking} onConfirm={book} onClose={() => setConfirming(false)}
        message={<>Book <b>{c.chargerCode}</b> at <b>{c.stationName}</b> for <b>{vehicle?.displayName}</b>, {startNow ? 'starting now' : `on ${date} at ${time}`} for <b>{duration} minutes</b>?
          <br /><br />Estimated total: <b>{money(estimate?.estimatedTotal)}</b>. You can cancel any time before charging starts.</>} />
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return <div className="flex justify-between gap-3"><dt className="text-ink-500">{label}</dt><dd className="tabular font-medium text-ink-900">{value}</dd></div>;
}
