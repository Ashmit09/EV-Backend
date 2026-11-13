import { Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { BatteryCharging, CalendarClock, Cpu, IndianRupee, MapPin, Users, Zap } from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Card, CardHeader, ErrorState, PageHeader, Spinner, StatCard, Table, Td } from '../../components/ui';
import { humanize, kwh, money, moneyShort, num } from '../../utils/format';
import { axis, statusColors } from './charts';

export default function AdminDashboard() {
  const overview = useAsync(() => adminApi.overview(), []);
  const revenue = useAsync(() => adminApi.revenue(6), []);
  const energy = useAsync(() => adminApi.energy(6), []);

  if (overview.loading) return <Spinner />;
  if (overview.error || !overview.data) return <ErrorState message={overview.error ?? 'No data'} onRetry={overview.reload} />;
  const o = overview.data;
  const distribution = Object.entries(o.chargerStatusDistribution).map(([status, value]) => ({ status, name: humanize(status), value }));

  return (
    <div className="animate-fade-in">
      <PageHeader title="Platform overview" subtitle="Live figures computed from the VoltX database." />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Simulated revenue" value={moneyShort(o.totalRevenue)} icon={IndianRupee} hint={`${moneyShort(o.collectedRevenue)} collected · ${moneyShort(o.pendingRevenue)} pending`} accent />
        <StatCard label="Energy delivered" value={kwh(o.totalEnergyKwh)} icon={Zap} />
        <StatCard label="Registered users" value={num(o.totalUsers)} icon={Users} hint={`${o.totalCustomers} customers`} />
        <StatCard label="Active sessions" value={o.activeSessions} icon={BatteryCharging} hint={`${o.reservationsToday} bookings today`} />
        <StatCard label="Stations" value={o.totalStations} icon={MapPin} hint={`${o.activeStations} active`} />
        <StatCard label="Charging units" value={o.totalChargers} icon={Cpu} hint={`${o.availableChargers} available now`} />
        <StatCard label="Network availability" value={`${o.totalChargers ? Math.round((o.availableChargers / o.totalChargers) * 100) : 0}%`} icon={CalendarClock} hint="Available ÷ total units" />
        <StatCard label="Avg. revenue / kWh" value={money(o.totalEnergyKwh ? o.totalRevenue / o.totalEnergyKwh : 0)} icon={IndianRupee} hint="incl. GST" />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader title="Monthly revenue" subtitle="Invoiced vs collected (₹)" />
          <div className="h-72 p-4">
            {revenue.data && (
              <ResponsiveContainer>
                <BarChart data={revenue.data.monthly} margin={{ left: 0, right: 10, top: 10 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#eceef1" vertical={false} />
                  <XAxis dataKey="month" tick={axis} axisLine={false} tickLine={false} />
                  <YAxis tick={axis} axisLine={false} tickLine={false} />
                  <Tooltip formatter={(v) => money(Number(v))} cursor={{ fill: '#f6f7f8' }} />
                  <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
                  <Bar dataKey="revenue" name="Invoiced" fill="#252a33" radius={[6, 6, 0, 0]} />
                  <Bar dataKey="collected" name="Collected" fill="#16c063" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </Card>
        <Card>
          <CardHeader title="Charger status" subtitle={`${o.totalChargers} units`} />
          <div className="h-72 p-4">
            <ResponsiveContainer>
              <PieChart>
                <Pie data={distribution} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={2}>
                  {distribution.map((d) => <Cell key={d.status} fill={statusColors[d.status]} />)}
                </Pie>
                <Tooltip />
                <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <Card className="mt-6">
        <CardHeader title="Energy consumption trend" subtitle="kWh delivered and sessions per month" />
        <div className="h-64 p-4">
          {energy.data && (
            <ResponsiveContainer>
              <LineChart data={energy.data.monthly} margin={{ left: 0, right: 10, top: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#eceef1" vertical={false} />
                <XAxis dataKey="month" tick={axis} axisLine={false} tickLine={false} />
                <YAxis yAxisId="e" tick={axis} axisLine={false} tickLine={false} />
                <YAxis yAxisId="s" orientation="right" tick={axis} axisLine={false} tickLine={false} />
                <Tooltip />
                <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
                <Line yAxisId="e" type="monotone" dataKey="energyKwh" name="Energy (kWh)" stroke="#0a9e4f" strokeWidth={2.5} dot={{ r: 3 }} />
                <Line yAxisId="s" type="monotone" dataKey="sessions" name="Sessions" stroke="#252a33" strokeWidth={2} strokeDasharray="5 4" dot={{ r: 3 }} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </Card>

      <Card className="mt-6">
        <CardHeader title="Station utilisation" subtitle="Live occupancy and the last 30 days of activity" />
        <Table head={['Station', 'City', 'Units', 'Occupied now', 'Sessions (30d)', 'Energy (30d)', 'Revenue (30d)']}>
          {o.stationUtilization.map((s) => (
            <tr key={s.stationId} className="hover:bg-ink-50/60">
              <Td className="font-medium text-ink-900">{s.stationName}</Td>
              <Td>{s.city}</Td>
              <Td className="tabular">{s.totalChargers}</Td>
              <Td>
                <div className="flex items-center gap-2">
                  <div className="h-1.5 w-20 overflow-hidden rounded-full bg-ink-100"><div className="h-full bg-brand-500" style={{ width: `${s.occupancyPercent}%` }} /></div>
                  <span className="tabular text-xs">{s.busyChargers}/{s.totalChargers}</span>
                </div>
              </Td>
              <Td className="tabular">{s.sessionsLast30Days}</Td>
              <Td className="tabular">{kwh(s.energyLast30DaysKwh)}</Td>
              <Td className="tabular">{money(s.revenueLast30Days)}</Td>
            </tr>
          ))}
        </Table>
      </Card>
    </div>
  );
}
