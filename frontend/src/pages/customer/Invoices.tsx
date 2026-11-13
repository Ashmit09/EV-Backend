import { Link } from 'react-router-dom';
import { FileText } from 'lucide-react';
import { invoiceApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Card, EmptyState, ErrorState, PageHeader, Spinner, StatCard, StatusBadge, Table, Td } from '../../components/ui';
import { dateOnly, kwh, money } from '../../utils/format';
import { CheckCircle2, Clock } from 'lucide-react';

export default function Invoices() {
  const invoices = useAsync(() => invoiceApi.list(), []);
  const unpaid = invoices.data?.filter((i) => i.status === 'UNPAID') ?? [];
  const paidTotal = invoices.data?.filter((i) => i.status === 'PAID').reduce((s, i) => s + Number(i.totalAmount), 0) ?? 0;

  return (
    <div className="animate-fade-in">
      <PageHeader title="Invoices" subtitle="One invoice is generated automatically for every completed charging session." />
      {invoices.loading ? <Spinner /> : invoices.error ? <ErrorState message={invoices.error} onRetry={invoices.reload} /> : (
        <>
          <div className="mb-6 grid gap-4 sm:grid-cols-3">
            <StatCard label="Invoices" value={invoices.data?.length ?? 0} icon={FileText} />
            <StatCard label="Paid" value={money(paidTotal)} icon={CheckCircle2} />
            <StatCard label="Awaiting payment" value={money(unpaid.reduce((s, i) => s + Number(i.totalAmount), 0))} icon={Clock} hint={`${unpaid.length} invoice${unpaid.length === 1 ? '' : 's'}`} accent={unpaid.length > 0} />
          </div>
          <Card>
            {invoices.data?.length === 0 ? <EmptyState icon={FileText} title="No invoices yet" /> : (
              <Table head={['Invoice', 'Issued', 'Station', 'Vehicle', 'Energy', 'Total', 'Status', '']}>
                {invoices.data?.map((i) => (
                  <tr key={i.id} className="hover:bg-ink-50/60">
                    <Td className="font-mono text-xs font-semibold text-ink-900">{i.invoiceNumber}</Td>
                    <Td>{dateOnly(i.issuedAt)}</Td>
                    <Td>{i.stationName}</Td>
                    <Td>{i.vehicleName}</Td>
                    <Td className="tabular">{kwh(i.energyKwh)}</Td>
                    <Td className="tabular font-semibold text-ink-900">{money(i.totalAmount)}</Td>
                    <Td><StatusBadge status={i.status} /></Td>
                    <Td><Link to={`/app/invoices/${i.id}`} className="font-semibold text-brand-700 hover:underline">{i.status === 'UNPAID' ? 'Pay' : 'View'}</Link></Td>
                  </tr>
                ))}
              </Table>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
