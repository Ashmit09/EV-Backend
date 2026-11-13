import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Clock, MapPin, Search, SlidersHorizontal, Zap } from 'lucide-react';
import { stationApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Badge, Card, EmptyState, ErrorState, Input, PageHeader, Select, Spinner } from '../../components/ui';
import { connectorLabel, connectorOptions, hhmm, money } from '../../utils/format';
import type { ConnectorType } from '../../types';

export default function Stations() {
  const [city, setCity] = useState('');
  const [keyword, setKeyword] = useState('');
  const [query, setQuery] = useState('');
  const [connector, setConnector] = useState<ConnectorType | ''>('');
  const [availableOnly, setAvailableOnly] = useState(false);

  const cities = useAsync(() => stationApi.cities(), []);
  const stations = useAsync(() => stationApi.search({ city: city || undefined, q: query || undefined, connector, availableOnly }),
    [city, query, connector, availableOnly]);

  return (
    <div className="animate-fade-in">
      <PageHeader title="Find charging stations" subtitle="Search the VoltX network and check live charger availability." />

      <Card className="mb-6 p-4">
        <form className="grid gap-3 md:grid-cols-[1.5fr_1fr_1fr_auto]" onSubmit={(e) => { e.preventDefault(); setQuery(keyword.trim()); }}>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-400" />
            <Input className="pl-9" placeholder="Search name, area or address — press Enter" value={keyword}
              onChange={(e) => setKeyword(e.target.value)} onBlur={() => setQuery(keyword.trim())} aria-label="Search stations" />
          </div>
          <Select value={city} onChange={(e) => setCity(e.target.value)} aria-label="City">
            <option value="">All cities</option>
            {cities.data?.map((c) => <option key={c} value={c}>{c}</option>)}
          </Select>
          <Select value={connector} onChange={(e) => setConnector(e.target.value as ConnectorType | '')} aria-label="Connector">
            <option value="">Any connector</option>
            {connectorOptions.map((c) => <option key={c} value={c}>{connectorLabel[c]}</option>)}
          </Select>
          <label className="flex h-10 cursor-pointer items-center gap-2 rounded-lg border border-ink-200 px-3 text-sm font-medium text-ink-700">
            <input type="checkbox" className="size-4 accent-brand-600" checked={availableOnly} onChange={(e) => setAvailableOnly(e.target.checked)} />
            Available now
          </label>
        </form>
      </Card>

      {stations.loading ? <Spinner label="Searching stations…" /> :
        stations.error ? <ErrorState message={stations.error} onRetry={stations.reload} /> :
        stations.data?.length === 0 ? (
          <Card><EmptyState icon={SlidersHorizontal} title="No stations match your filters" description="Try another city or connector type." /></Card>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {stations.data?.map((s) => (
              <Link key={s.id} to={`/app/stations/${s.id}`}>
                <Card className="group h-full p-5 transition hover:-translate-y-0.5 hover:border-brand-300 hover:shadow-lg">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <h3 className="truncate font-semibold text-ink-900 group-hover:text-brand-700">{s.name}</h3>
                      <p className="mt-1 flex items-center gap-1 text-sm text-ink-500"><MapPin className="size-3.5 shrink-0" /><span className="truncate">{s.address}, {s.city}</span></p>
                    </div>
                    <Badge tone={s.availableChargers > 0 ? 'green' : 'amber'} dot>{s.availableChargers}/{s.totalChargers} free</Badge>
                  </div>
                  <div className="mt-4 flex flex-wrap gap-1.5">
                    {s.connectorTypes.map((c) => <Badge key={c} tone="gray">{connectorLabel[c]}</Badge>)}
                  </div>
                  <div className="mt-5 grid grid-cols-3 gap-2 border-t border-ink-100 pt-4 text-sm">
                    <div><p className="text-xs text-ink-500">Up to</p><p className="flex items-center gap-1 font-semibold"><Zap className="size-3.5 text-brand-600" />{s.maxPowerKw} kW</p></div>
                    <div><p className="text-xs text-ink-500">From</p><p className="font-semibold">{money(s.minPricePerKwh)}/kWh</p></div>
                    <div><p className="text-xs text-ink-500">Hours</p><p className="flex items-center gap-1 font-semibold"><Clock className="size-3.5 text-ink-400" />{s.open24Hours ? '24 × 7' : `${hhmm(s.openingTime)}–${hhmm(s.closingTime)}`}</p></div>
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        )}
    </div>
  );
}
