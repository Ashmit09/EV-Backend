import { Link } from 'react-router-dom';
import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ArrowRight, BatteryCharging, CalendarClock, Clock, IndianRupee, MapPin, Zap } from 'lucide-react';
import { analyticsApi, sessionApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { BatteryBar, Button, Card, CardHeader, EmptyState, ErrorState, PageHeader, Spinner, StatCard, Table, Td } from '../../components/ui';
import { dateTime, kwh, minutes, money, moneyShort, pct } from '../../utils/format';

export default function CustomerDashboard() {
  const analytics = useAsync(() => analyticsApi.customer(), []);
  const active = useAsync(() => sessionApi.active(), []);

  if (analytics.loading) return <Spinner />;
  if (analytics.error || !analytics.data) return <ErrorState message={analytics.error ?? 'No data'} onRetry={analytics.reload} />;
  const a = analytics.data;

  return (
    <div className="animate-fade-in">
      <PageHeader title="Dashboard" subtitle="Your charging activity at a glance."
        actions={<Link to="/app/stations"><Button icon={MapPin}>Find a charger</Button></Link>} />

      {active.data && (
        <Link to="/app/session" className="mb-6 block">
          <Card className="flex flex-wrap items-center gap-5 border-brand-300 bg-brand-50/60 p-5 transition hover:shadow-md">
            <div className="grid size-12 place-items-center rounded-2xl bg-brand-500 text-ink-950"><BatteryCharging className="size-6 animate-pulse" /></div>
            <div className="min-w-[180px] flex-1">
              <p className="text-sm font-semibold text-ink-900">{active.data.vehicleName} is charging at {active.data.stationName}</p>
              <div className="mt-2 flex items-center gap-3"><BatteryBar value={active.data.currentBatteryPercentage} target={active.data.targetBatteryPercentage} />
                <span className="tabular text-sm font-semibold">{pct(active.data.currentBatteryPercentage)}</span></div>
            </div>
            <span className="flex items-center gap-1 text-sm font-semibold text-brand-700">Open live view <ArrowRight className="size-4" /></span>
          </Card>
        </Link>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Charging sessions" value={a.totalSessions} icon={Zap} hint={`${a.vehicles} vehicle${a.vehicles === 1 ? '' : 's'} registered`} accent />
        <StatCard label="Energy consumed" value={kwh(a.totalEnergyKwh)} icon={BatteryCharging} />
        <StatCard label="Total expenses" value={moneyShort(a.totalSpent)} icon={IndianRupee} hint={a.outstandingAmount > 0 ? `${money(a.outstandingAmount)} unpaid` : 'All invoices settled'} />
        <StatCard label="Avg. session" value={minutes(a.averageSessionMinutes)} icon={Clock} hint={`${a.upcomingReservations} upcoming booking${a.upcomingReservations === 1 ? '' : 's'}`} />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <CardHeader title="Monthly energy consumption" subtitle="kWh delivered over the last 6 months" />
          <div className="h-72 p-4">
            <ResponsiveContainer>
              <AreaChart data={a.monthly} margin={{ left: -10, right: 10, top: 10 }}>
                <defs>
                  <linearGradient id="energy" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#16c063" stopOpacity={0.35} />
                    <stop offset="100%" stopColor="#16c063" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#eceef1" vertical={false} />
                <XAxis dataKey="month" tick={{ fontSize: 12, fill: '#677084' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 12, fill: '#677084' }} axisLine={false} tickLine={false} />
                <Tooltip formatter={(v) => kwh(Number(v))} />
                <Area type="monotone" dataKey="energyKwh" name="Energy" stroke="#0a9e4f" strokeWidth={2.5} fill="url(#energy)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card className="lg:col-span-2">
          <CardHeader title="Monthly spend" subtitle="Invoice totals incl. GST" />
          <div className="h-72 p-4">
            <ResponsiveContainer>
              <BarChart data={a.monthly} margin={{ left: -6, right: 10, top: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#eceef1" vertical={false} />
                <XAxis dataKey="month" tick={{ fontSize: 12, fill: '#677084' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 12, fill: '#677084' }} axisLine={false} tickLine={false} />
                <Tooltip formatter={(v) => money(Number(v))} cursor={{ fill: '#f6f7f8' }} />
                <Bar dataKey="revenue" name="Spend" fill="#252a33" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <Card className="mt-6">
        <CardHeader title="Recent charging activity" action={<Link to="/app/history" className="text-sm font-semibold text-brand-700 hover:underline">View all</Link>} />
        {a.recentSessions.length === 0 ? (
          <EmptyState icon={CalendarClock} title="No charging sessions yet" description="Book a slot at a nearby station to get started."
            action={<Link to="/app/stations"><Button size="sm">Find stations</Button></Link>} />
        ) : (
          <Table head={['Date', 'Station', 'Vehicle', 'Battery', 'Energy', 'Cost', '']}>
            {a.recentSessions.map((s) => (
              <tr key={s.id} className="hover:bg-ink-50/60">
                <Td>{dateTime(s.startTime)}</Td>
                <Td className="font-medium text-ink-900">{s.stationName}</Td>
                <Td>{s.vehicleName}</Td>
                <Td className="tabular">{pct(s.initialBatteryPercentage)} → {pct(s.currentBatteryPercentage)}</Td>
                <Td className="tabular">{kwh(s.energyKwh)}</Td>
                <Td className="tabular">{money(s.cost)}</Td>
                <Td>{s.invoiceId && <Link className="font-semibold text-brand-700 hover:underline" to={`/app/invoices/${s.invoiceId}`}>Invoice</Link>}</Td>
              </tr>
            ))}
          </Table>
        )}
      </Card>
    </div>
  );
}
