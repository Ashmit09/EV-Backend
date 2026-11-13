import { Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend } from 'recharts';
import { adminApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Card, CardHeader, ErrorState, PageHeader, Spinner } from '../../components/ui';
import { humanize, kwh } from '../../utils/format';
import { axis, palette, toSeries } from './charts';

export default function AdminAnalytics() {
  const energy = useAsync(() => adminApi.energy(12), []);
  if (energy.loading) return <Spinner />;
  if (energy.error || !energy.data) return <ErrorState message={energy.error ?? 'No data'} onRetry={energy.reload} />;
  const e = energy.data;
  const byType = toSeries(e.byVehicleType).map((d) => ({ ...d, name: humanize(d.name) }));

  return (
    <div className="animate-fade-in">
      <PageHeader title="Energy analytics" subtitle="Energy delivered across the network over the last 12 months." />
      <Card>
        <CardHeader title="Energy delivered per month" />
        <div className="h-80 p-4">
          <ResponsiveContainer>
            <AreaChart data={e.monthly} margin={{ left: 0, right: 10, top: 10 }}>
              <defs><linearGradient id="g" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#16c063" stopOpacity={0.35} /><stop offset="100%" stopColor="#16c063" stopOpacity={0} /></linearGradient></defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#eceef1" vertical={false} />
              <XAxis dataKey="month" tick={axis} axisLine={false} tickLine={false} />
              <YAxis tick={axis} axisLine={false} tickLine={false} />
              <Tooltip formatter={(v) => kwh(Number(v))} />
              <Area type="monotone" dataKey="energyKwh" name="Energy" stroke="#0a9e4f" strokeWidth={2.5} fill="url(#g)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </Card>
      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Card>
          <CardHeader title="By connector" />
          <div className="h-64 p-4">
            <ResponsiveContainer>
              <BarChart data={toSeries(e.byConnector)} layout="vertical" margin={{ left: 10, right: 20 }}>
                <XAxis type="number" tick={axis} axisLine={false} tickLine={false} />
                <YAxis type="category" dataKey="name" tick={axis} axisLine={false} tickLine={false} width={70} />
                <Tooltip formatter={(v) => kwh(Number(v))} cursor={{ fill: '#f6f7f8' }} />
                <Bar dataKey="value" name="Energy" fill="#252a33" radius={[0, 6, 6, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card>
          <CardHeader title="By vehicle type" />
          <div className="h-64 p-4">
            <ResponsiveContainer>
              <PieChart>
                <Pie data={byType} dataKey="value" nameKey="name" innerRadius={45} outerRadius={80} paddingAngle={2}>
                  {byType.map((_, i) => <Cell key={i} fill={palette[i % palette.length]} />)}
                </Pie>
                <Tooltip formatter={(v) => kwh(Number(v))} />
                <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card>
          <CardHeader title="By city" />
          <div className="h-64 p-4">
            <ResponsiveContainer>
              <BarChart data={toSeries(e.byCity)} margin={{ left: 0, right: 10, top: 10 }}>
                <XAxis dataKey="name" tick={{ ...axis, fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={axis} axisLine={false} tickLine={false} />
                <Tooltip formatter={(v) => kwh(Number(v))} cursor={{ fill: '#f6f7f8' }} />
                <Bar dataKey="value" name="Energy" fill="#16c063" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>
    </div>
  );
}
