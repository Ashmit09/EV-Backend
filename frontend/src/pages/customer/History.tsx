import { Link } from 'react-router-dom';
import { History as HistoryIcon } from 'lucide-react';
import { sessionApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Card, EmptyState, ErrorState, PageHeader, Spinner, StatusBadge, Table, Td } from '../../components/ui';
import { dateTime, kwh, minutes, money, pct } from '../../utils/format';

export default function History() {
  const sessions = useAsync(() => sessionApi.history(), []);
  const completed = sessions.data?.filter((s) => s.status === 'COMPLETED') ?? [];
  const totalEnergy = completed.reduce((sum, s) => sum + Number(s.energyKwh ?? 0), 0);
  const totalCost = completed.reduce((sum, s) => sum + Number(s.cost ?? 0), 0);

  return (
    <div className="animate-fade-in">
      <PageHeader title="Charging history" subtitle={sessions.data ? `${completed.length} completed sessions · ${kwh(totalEnergy)} · ${money(totalCost)} before GST` : undefined} />
      {sessions.loading ? <Spinner /> : sessions.error ? <ErrorState message={sessions.error} onRetry={sessions.reload} /> : (
        <Card>
          {sessions.data?.length === 0 ? <EmptyState icon={HistoryIcon} title="No charging sessions yet" /> : (
            <Table head={['Started', 'Station', 'Charger', 'Vehicle', 'Battery', 'Energy', 'Duration', 'Cost', 'Status', '']}>
              {sessions.data?.map((s) => (
                <tr key={s.id} className="hover:bg-ink-50/60">
                  <Td>{dateTime(s.startTime)}</Td>
                  <Td className="font-medium text-ink-900">{s.stationName}</Td>
                  <Td>{s.chargerCode}</Td>
                  <Td>{s.vehicleName}</Td>
                  <Td className="tabular">{pct(s.initialBatteryPercentage)} → {pct(s.currentBatteryPercentage)}</Td>
                  <Td className="tabular">{kwh(s.energyKwh)}</Td>
                  <Td className="tabular">{minutes(s.simulatedMinutes)}</Td>
                  <Td className="tabular">{money(s.cost)}</Td>
                  <Td><StatusBadge status={s.status} /></Td>
                  <Td>{s.status === 'IN_PROGRESS'
                    ? <Link to="/app/session" className="font-semibold text-brand-700 hover:underline">Live</Link>
                    : s.invoiceId && <Link to={`/app/invoices/${s.invoiceId}`} className="font-semibold text-brand-700 hover:underline">Invoice</Link>}</Td>
                </tr>
              ))}
            </Table>
          )}
        </Card>
      )}
    </div>
  );
}
