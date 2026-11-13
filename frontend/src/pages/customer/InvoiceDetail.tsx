import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CreditCard, Printer, Smartphone, Wallet } from 'lucide-react';
import { invoiceApi } from '../../api/endpoints';
import { errorMessage } from '../../api/client';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Logo } from '../../components/Logo';
import { Button, Card, CardHeader, ConfirmDialog, ErrorState, Spinner, StatusBadge, cx } from '../../components/ui';
import { dateTime, kwh, minutes, money, pct } from '../../utils/format';
import type { PaymentMethod } from '../../types';

const methods: { id: PaymentMethod; label: string; icon: typeof Wallet; note: string }[] = [
  { id: 'UPI', label: 'UPI', icon: Smartphone, note: 'Up to ₹1,00,000' },
  { id: 'CARD', label: 'Card', icon: CreditCard, note: 'Debit / credit' },
  { id: 'WALLET', label: 'VoltX Wallet', icon: Wallet, note: 'Up to ₹2,000' },
];

export default function InvoiceDetail() {
  const { id } = useParams();
  const toast = useToast();
  const invoice = useAsync(() => invoiceApi.get(Number(id)), [id]);
  const [method, setMethod] = useState<PaymentMethod>('UPI');
  const [confirming, setConfirming] = useState(false);
  const [paying, setPaying] = useState(false);

  const pay = async () => {
    setPaying(true);
    try {
      invoice.setData(await invoiceApi.pay(Number(id), method));
      toast('Payment successful');
    } catch (e) {
      toast(errorMessage(e), 'error');
      void invoice.reload(true);
    } finally {
      setPaying(false);
      setConfirming(false);
    }
  };

  if (invoice.loading) return <Spinner />;
  if (invoice.error || !invoice.data) return <ErrorState message={invoice.error ?? 'Invoice not found'} />;
  const i = invoice.data;

  return (
    <div className="animate-fade-in">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3 print:hidden">
        <Link to="/app/invoices" className="inline-flex items-center gap-1 text-sm font-medium text-ink-500 hover:text-ink-900"><ArrowLeft className="size-4" /> All invoices</Link>
        <Button variant="secondary" icon={Printer} onClick={() => window.print()}>Print</Button>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="p-6 sm:p-8 lg:col-span-2">
          <div className="flex flex-wrap items-start justify-between gap-4 border-b border-ink-100 pb-6">
            <div><Logo /><p className="mt-3 text-xs text-ink-500">VoltX Charging Network (simulated)</p></div>
            <div className="text-right">
              <p className="font-mono text-sm font-semibold">{i.invoiceNumber}</p>
              <p className="mt-1 text-xs text-ink-500">Issued {dateTime(i.issuedAt)}</p>
              <div className="mt-2"><StatusBadge status={i.status} /></div>
            </div>
          </div>

          <div className="grid gap-6 border-b border-ink-100 py-6 text-sm sm:grid-cols-2">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-ink-500">Charging location</p>
              <p className="mt-1 font-semibold">{i.stationName}</p>
              <p className="text-ink-500">{i.stationAddress}, {i.city}</p>
              <p className="text-ink-500">Charger {i.chargerCode} · {i.connectorLabel}</p>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-ink-500">Session</p>
              <p className="mt-1 font-semibold">{i.vehicleName} · {i.registrationNumber}</p>
              <p className="text-ink-500">{dateTime(i.sessionStart)} → {dateTime(i.sessionEnd)}</p>
              <p className="text-ink-500">Battery {pct(i.initialBatteryPercentage)} → {pct(i.finalBatteryPercentage)} · {minutes(i.simulatedDurationMinutes)}</p>
            </div>
          </div>

          <dl className="space-y-3 py-6 text-sm">
            <Line label={`Energy delivered (${kwh(i.energyKwh)} × ${money(i.ratePerKwh)}/kWh)`} value={money(i.energyCost)} />
            {i.adjustmentLabel && <Line label={`${i.pricingPlanLabel}: ${i.adjustmentLabel}`} value={money(i.adjustmentAmount)} />}
            <Line label="Subtotal" value={money(i.subtotal)} />
            <Line label={`GST (${Math.round(i.taxRate * 100)}%)`} value={money(i.taxAmount)} />
            <div className="flex justify-between border-t border-ink-200 pt-4 text-lg font-bold"><dt>Total</dt><dd className="tabular">{money(i.totalAmount)}</dd></div>
          </dl>
          <p className="text-xs text-ink-400">Energy = battery capacity × (final % − initial %) ÷ 100. Payment processing is simulated.</p>
        </Card>

        <div className="space-y-6 print:hidden">
          {i.status === 'UNPAID' ? (
            <Card>
              <CardHeader title="Pay invoice" subtitle={`Amount due ${money(i.totalAmount)}`} />
              <div className="space-y-2 p-5">
                {methods.map(({ id: m, label, icon: Icon, note }) => (
                  <button key={m} onClick={() => setMethod(m)} aria-pressed={method === m}
                    className={cx('flex w-full items-center gap-3 rounded-xl border p-3 text-left transition',
                      method === m ? 'border-brand-500 bg-brand-50 ring-2 ring-brand-100' : 'border-ink-200 hover:bg-ink-50')}>
                    <Icon className="size-5 text-ink-700" />
                    <span className="flex-1"><span className="block text-sm font-semibold">{label}</span><span className="text-xs text-ink-500">{note}</span></span>
                  </button>
                ))}
                <Button className="mt-3 w-full" size="lg" onClick={() => setConfirming(true)}>Pay {money(i.totalAmount)}</Button>
              </div>
            </Card>
          ) : (
            <Card className="p-5"><p className="text-sm font-semibold text-brand-700">Paid on {dateTime(i.paidAt)}</p></Card>
          )}
          {i.payments.length > 0 && (
            <Card>
              <CardHeader title="Payment attempts" />
              <div className="divide-y divide-ink-100">
                {i.payments.map((p) => (
                  <div key={p.id} className="px-5 py-3 text-sm">
                    <div className="flex items-center justify-between"><span className="font-medium">{p.method} · {money(p.amount)}</span><StatusBadge status={p.status} /></div>
                    <p className="mt-1 font-mono text-xs text-ink-500">{p.transactionRef}</p>
                    {p.message && <p className="mt-0.5 text-xs text-ink-500">{p.message}</p>}
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>
      </div>

      <ConfirmDialog open={confirming} title="Confirm payment" confirmLabel={`Pay ${money(i.totalAmount)}`} loading={paying} onConfirm={pay} onClose={() => setConfirming(false)}
        message={<>Pay <b>{money(i.totalAmount)}</b> for {i.invoiceNumber} using <b>{methods.find((m) => m.id === method)?.label}</b>? This is a simulated payment — no money moves.</>} />
    </div>
  );
}

function Line({ label, value }: { label: string; value: string }) {
  return <div className="flex justify-between gap-4"><dt className="text-ink-600">{label}</dt><dd className="tabular font-medium">{value}</dd></div>;
}
