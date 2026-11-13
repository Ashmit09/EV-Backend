import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, Clock, MapPin, Navigation, XCircle, Zap } from 'lucide-react';
import { stationApi, vehicleApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Badge, Button, Card, CardHeader, ErrorState, PageHeader, Select, Spinner, StatusBadge } from '../../components/ui';
import { hhmm, money } from '../../utils/format';

export default function StationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const vehicles = useAsync(() => vehicleApi.list(), []);
  const [vehicleId, setVehicleId] = useState<number | undefined>();

  useEffect(() => {
    if (vehicleId === undefined && vehicles.data?.length) setVehicleId(vehicles.data[0].id);
  }, [vehicles.data, vehicleId]);

  const station = useAsync(() => stationApi.get(Number(id), vehicleId), [id, vehicleId]);

  if (station.loading && !station.data) return <Spinner />;
  if (station.error || !station.data) return <ErrorState message={station.error ?? 'Station not found'} onRetry={station.reload} />;
  const s = station.data;

  return (
    <div className="animate-fade-in">
      <Link to="/app/stations" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-ink-500 hover:text-ink-900"><ArrowLeft className="size-4" /> All stations</Link>
      <PageHeader title={s.name} subtitle={`${s.address}, ${s.city}`}
        actions={<a href={`https://www.google.com/maps/search/?api=1&query=${s.latitude},${s.longitude}`} target="_blank" rel="noreferrer">
          <Button variant="secondary" icon={Navigation}>Directions</Button></a>} />

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="space-y-4 p-5 lg:col-span-1">
          <div className="flex items-center justify-between"><span className="text-sm text-ink-500">Status</span><StatusBadge status={s.status} /></div>
          <div className="flex items-center justify-between"><span className="flex items-center gap-2 text-sm text-ink-500"><Clock className="size-4" />Hours</span>
            <span className="text-sm font-semibold">{s.open24Hours ? 'Open 24 hours' : `${hhmm(s.openingTime)} – ${hhmm(s.closingTime)}`}</span></div>
          <div className="flex items-center justify-between"><span className="flex items-center gap-2 text-sm text-ink-500"><Zap className="size-4" />Chargers</span>
            <span className="text-sm font-semibold">{s.availableChargers} of {s.totalChargers} available</span></div>
          <div className="flex items-center justify-between"><span className="flex items-center gap-2 text-sm text-ink-500"><MapPin className="size-4" />Coordinates</span>
            <span className="tabular text-sm font-semibold">{Number(s.latitude).toFixed(4)}, {Number(s.longitude).toFixed(4)}</span></div>
          <div className="border-t border-ink-100 pt-4">
            <label className="mb-1.5 block text-sm font-medium text-ink-700" htmlFor="vehicle">Check compatibility for</label>
            {vehicles.data?.length ? (
              <Select id="vehicle" value={vehicleId ?? ''} onChange={(e) => setVehicleId(Number(e.target.value))}>
                {vehicles.data.map((v) => <option key={v.id} value={v.id}>{v.displayName} · {v.registrationNumber}</option>)}
              </Select>
            ) : (
              <p className="text-sm text-ink-500">Add a vehicle to check compatibility. <Link className="font-semibold text-brand-700" to="/app/vehicles">Add vehicle</Link></p>
            )}
          </div>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader title="Charging units" subtitle="Compatibility is validated on the server against your vehicle’s connectors and limits." />
          <div className="divide-y divide-ink-100">
            {s.chargers.map((c) => {
              const canBook = c.bookable && c.compatible !== false && !!vehicleId;
              return (
                <div key={c.id} className="flex flex-wrap items-center gap-4 px-5 py-4">
                  <div className={`grid size-11 place-items-center rounded-xl ${c.dcFast ? 'bg-ink-900 text-brand-400' : 'bg-brand-50 text-brand-700'}`}><Zap className="size-5" /></div>
                  <div className="min-w-[180px] flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-semibold text-ink-900">{c.chargerCode}</p>
                      <StatusBadge status={c.status} />
                      {c.pricingPlan !== 'STANDARD' && <Badge tone={c.pricingPlan === 'PROMOTIONAL' ? 'green' : 'amber'}>{c.pricingPlanLabel}</Badge>}
                    </div>
                    <p className="mt-1 text-sm text-ink-500">{c.connectorLabel} · {c.maxPowerKw} kW {c.dcFast ? 'DC fast' : 'AC'} · {money(c.pricePerKwh)}/kWh</p>
                    {c.compatible === true && <p className="mt-1 flex items-center gap-1 text-xs font-medium text-brand-700"><CheckCircle2 className="size-3.5" />Compatible with your vehicle</p>}
                    {c.compatible === false && <p className="mt-1 flex items-start gap-1 text-xs font-medium text-red-600"><XCircle className="mt-px size-3.5 shrink-0" />{c.incompatibilityReason}</p>}
                  </div>
                  <Button disabled={!canBook} onClick={() => navigate(`/app/book/${c.id}?vehicle=${vehicleId}`)}
                    title={!c.bookable ? 'This charger is not accepting bookings' : undefined}>
                    Book slot
                  </Button>
                </div>
              );
            })}
          </div>
        </Card>
      </div>
    </div>
  );
}
