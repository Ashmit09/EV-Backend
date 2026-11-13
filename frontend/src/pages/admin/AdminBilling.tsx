import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend } from 'recharts';
import { CheckCircle2, Clock, IndianRupee } from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Card, CardHeader, ErrorState, PageHeader, Spinner, StatCard, StatusBadge, Table, Td, cx } from '../../components/ui';
import { dateTime, kwh, money, moneyShort } from '../../utils/format';
import { axis, palette, toSeries } from './charts';

export default function AdminBilling() {
  const [tab, setTab] = useState<'invoices' | 'payments'>('invoices');
  const revenue = useAsync(() => adminApi.revenue(12), []);
  const invoices = useAsync(() => adminApi.invoices(), []);
  const payments = useAsync(() => adminApi.payments(), []);

  if (revenue.loading || invoices.loading) return <Spinner />;
  if (revenue.error || !revenue.data) return <ErrorState message={revenue.error ?? 'No data'} onRetry={revenue.reload} />;
  const all = invoices.data ?? [];
  const total = all.reduce((s, i) => s + Number(i.totalAmount), 0);
  const paid = all.filter((i) => i.status === 'PAID').reduce((s, i) => s + Number(i.totalAmount), 0);

  return (
    <div className="animate-fade-in">
      <PageHeader title="Billing & revenue" subtitle="Simulated revenue from generated invoices and processed payments." />
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Total invoiced" value={moneyShort(total)} icon={IndianRupee} hint={`${all.length} invoices`} accent />
        <StatCard label="Collected" value={moneyShort(paid)} icon={CheckCircle2} hint={`${total ? Math.round((paid / total) * 100) : 0}% collection rate`} />
        <StatCard label="Outstanding" value={moneyShort(total - paid)} icon={Clock} hint={`${all.filter((i) => i.status === 'UNPAID').length} unpaid`} />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader title="Revenue by month" subtitle="Last 12 months" />
          <div className="h-72 p-4">
            <ResponsiveContainer>
              <BarChart data={revenue.data.monthly} margin={{ left: 0, right: 10, top: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#eceef1" vertical={false} />
                <XAxis dataKey="month" tick={axis} axisLine={false} tickLine={false} />
                <YAxis tick={axis} axisLine={false} tickLine={false} />
                <Tooltip formatter={(v) => money(Number(v))} cursor={{ fill: '#f6f7f8' }} />
                <Bar dataKey="revenue" name="Invoiced" fill="#0a9e4f" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card>
          <CardHeader title="Revenue by pricing strategy" />
          <div className="h-72 p-4">
            <ResponsiveContainer>
              <PieChart>
                <Pie data={toSeries(revenue.data.byPricingPlan)} dataKey="value" nameKey="name" innerRadius={50} outerRadius={85} paddingAngle={2}>
                  {toSeries(revenue.data.byPricingPlan).map((_, i) => <Cell key={i} fill={palette[i % palette.length]} />)}
                </Pie>
                <Tooltip formatter={(v) => money(Number(v))} />
                <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="Collected by payment method" />
          <div className="space-y-3 p-5">
            {toSeries(revenue.data.byPaymentMethod).map((m) => (
              <div key={m.name}>
                <div className="mb-1 flex justify-between text-sm"><span className="font-medium">{m.name}</span><span className="tabular">{money(m.value)}</span></div>
                <div className="h-2 overflow-hidden rounded-full bg-ink-100"><div className="h-full bg-ink-800" style={{ width: `${paid ? (m.value / paid) * 100 : 0}%` }} /></div>
              </div>
            ))}
          </div>
        </Card>
        <Card>
          <CardHeader title="Revenue by city" />
          <div className="space-y-3 p-5">
            {toSeries(revenue.data.byCity).sort((a, b) => b.value - a.value).map((c) => (
              <div key={c.name}>
                <div className="mb-1 flex justify-between text-sm"><span className="font-medium">{c.name}</span><span className="tabular">{money(c.value)}</span></div>
                <div className="h-2 overflow-hidden rounded-full bg-ink-100"><div className="h-full bg-brand-500" style={{ width: `${total ? (c.value / total) * 100 : 0}%` }} /></div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Card className="mt-6">
        <div className="flex gap-1 border-b border-ink-100 px-3 pt-3">
          {(['invoices', 'payments'] as const).map((t) => (
            <button key={t} onClick={() => setTab(t)}
              className={cx('rounded-t-lg px-4 py-2 text-sm font-semibold capitalize', tab === t ? 'border-b-2 border-brand-600 text-ink-900' : 'text-ink-500 hover:text-ink-800')}>
              {t === 'invoices' ? 'Invoices' : 'Transactions'}
            </button>
          ))}
        </div>
        {tab === 'invoices' ? (
          <Table head={['Invoice', 'Issued', 'Customer', 'Station', 'Energy', 'Plan', 'Total', 'Status']}>
            {all.slice(0, 100).map((i) => (
              <tr key={i.id} className="hover:bg-ink-50/60">
                <Td className="font-mono text-xs font-semibold text-ink-900">{i.invoiceNumber}</Td>
                <Td>{dateTime(i.issuedAt)}</Td>
                <Td>{i.customerName}</Td>
                <Td>{i.stationName}</Td>
                <Td className="tabular">{kwh(i.energyKwh)}</Td>
                <Td>{i.pricingPlanLabel}</Td>
                <Td className="tabular font-semibold text-ink-900">{money(i.totalAmount)}</Td>
                <Td><StatusBadge status={i.status} /></Td>
              </tr>
            ))}
          </Table>
        ) : payments.error ? <div className="p-5"><ErrorState message={payments.error} /></div> : (
          <Table head={['Reference', 'Processed', 'Invoice', 'Customer', 'Method', 'Amount', 'Status']}>
            {(payments.data ?? []).slice(0, 100).map((p) => (
              <tr key={p.id} className="hover:bg-ink-50/60">
                <Td className="font-mono text-xs">{p.transactionRef}</Td>
                <Td>{dateTime(p.processedAt)}</Td>
                <Td className="font-mono text-xs">{p.invoiceNumber}</Td>
                <Td>{p.customerEmail}</Td>
                <Td>{p.method}</Td>
                <Td className="tabular font-semibold">{money(p.amount)}</Td>
                <Td><StatusBadge status={p.status} /></Td>
              </tr>
            ))}
          </Table>
        )}
      </Card>
    </div>
  );
}
