import { useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Plus, Settings2, Wrench, Ban, RotateCcw } from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { errorMessage, toApiError } from '../../api/client';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Badge, Button, Card, ConfirmDialog, ErrorState, Field, Input, Modal, PageHeader, Select, Spinner, StatusBadge, Table, Td } from '../../components/ui';
import { connectorLabel, connectorOptions, money } from '../../utils/format';
import type { Charger, ChargerAdminAction, ConnectorType, PricingPlan } from '../../types';

const plans: { id: PricingPlan; label: string }[] = [
  { id: 'STANDARD', label: 'Standard – flat rate' },
  { id: 'PEAK_HOUR', label: 'Peak hour – +25% 18:00–22:00' },
  { id: 'PROMOTIONAL', label: 'Promotional – 15% off' },
];
const actionLabels: Record<ChargerAdminAction, string> = { MARK_OUT_OF_SERVICE: 'Mark out of service', MARK_MAINTENANCE: 'Start maintenance', RESTORE: 'Restore to service' };

export default function AdminChargers() {
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const stationFilter = params.get('station') ? Number(params.get('station')) : undefined;
  const stations = useAsync(() => adminApi.stations(), []);
  const chargers = useAsync(() => adminApi.chargers(stationFilter), [stationFilter]);

  const [editing, setEditing] = useState<Charger | null>(null);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ stationId: 0, chargerCode: '', connectorType: 'CCS2' as ConnectorType, maxPowerKw: 60, pricePerKwh: 18, pricingPlan: 'STANDARD' as PricingPlan });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [pending, setPending] = useState<{ charger: Charger; action: ChargerAdminAction } | null>(null);
  const [busy, setBusy] = useState(false);

  const openForm = (c?: Charger) => {
    setEditing(c ?? null);
    setForm(c ? { stationId: c.stationId, chargerCode: c.chargerCode, connectorType: c.connectorType, maxPowerKw: c.maxPowerKw, pricePerKwh: c.pricePerKwh, pricingPlan: c.pricingPlan }
      : { stationId: stationFilter ?? stations.data?.[0]?.id ?? 0, chargerCode: '', connectorType: 'CCS2', maxPowerKw: 60, pricePerKwh: 18, pricingPlan: 'STANDARD' });
    setErrors({}); setFormError(null); setOpen(true);
  };

  const save = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true); setErrors({}); setFormError(null);
    try {
      const specs = { connectorType: form.connectorType, maxPowerKw: form.maxPowerKw, pricePerKwh: form.pricePerKwh, pricingPlan: form.pricingPlan };
      if (editing) await adminApi.updateCharger(editing.id, specs); else await adminApi.createCharger({ ...specs, stationId: form.stationId, chargerCode: form.chargerCode });
      toast(editing ? 'Charger updated' : 'Charger added');
      setOpen(false);
      void chargers.reload(true);
    } catch (err) {
      const apiError = toApiError(err);
      setErrors(apiError.fieldErrors); setFormError(apiError.message);
    } finally {
      setSaving(false);
    }
  };

  const runAction = async () => {
    if (!pending) return;
    setBusy(true);
    try {
      const updated = await adminApi.chargerAction(pending.charger.id, pending.action);
      toast(`${updated.chargerCode} is now ${updated.status.replace(/_/g, ' ').toLowerCase()}`);
      void chargers.reload(true);
    } catch (err) {
      toast(errorMessage(err), 'error');
    } finally {
      setBusy(false);
      setPending(null);
    }
  };

  return (
    <div className="animate-fade-in">
      <PageHeader title="Charging units" subtitle="Configure hardware, pricing and operational status. Status changes follow the charger state machine."
        actions={<>
          <Select value={stationFilter ?? ''} onChange={(e) => setParams(e.target.value ? { station: e.target.value } : {})} aria-label="Filter by station" className="w-60">
            <option value="">All stations</option>
            {stations.data?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </Select>
          <Button icon={Plus} onClick={() => openForm()} disabled={!stations.data?.length}>Add charger</Button>
        </>} />

      {chargers.loading ? <Spinner /> : chargers.error ? <ErrorState message={chargers.error} onRetry={chargers.reload} /> : (
        <Card>
          <Table head={['Charger', 'Station', 'Connector', 'Power', 'Price', 'Pricing', 'Status', 'Actions']}>
            {chargers.data?.map((c) => (
              <tr key={c.id} className="hover:bg-ink-50/60">
                <Td className="font-mono text-xs font-semibold text-ink-900">{c.chargerCode}</Td>
                <Td>{c.stationName}</Td>
                <Td>{c.connectorLabel} {c.dcFast ? <Badge tone="dark">DC</Badge> : <Badge>AC</Badge>}</Td>
                <Td className="tabular">{c.maxPowerKw} kW</Td>
                <Td className="tabular">{money(c.pricePerKwh)}</Td>
                <Td>{c.pricingPlanLabel}</Td>
                <Td><StatusBadge status={c.status} /></Td>
                <Td>
                  <div className="flex gap-1">
                    <Button size="sm" variant="ghost" icon={Settings2} onClick={() => openForm(c)}>Configure</Button>
                    {c.status === 'AVAILABLE' && <>
                      <Button size="sm" variant="ghost" icon={Wrench} onClick={() => setPending({ charger: c, action: 'MARK_MAINTENANCE' })}>Maintain</Button>
                      <Button size="sm" variant="ghost" icon={Ban} className="text-red-600 hover:bg-red-50" onClick={() => setPending({ charger: c, action: 'MARK_OUT_OF_SERVICE' })}>Out of service</Button>
                    </>}
                    {(c.status === 'MAINTENANCE' || c.status === 'OUT_OF_SERVICE') &&
                      <Button size="sm" variant="secondary" icon={RotateCcw} onClick={() => setPending({ charger: c, action: 'RESTORE' })}>Restore</Button>}
                  </div>
                </Td>
              </tr>
            ))}
          </Table>
        </Card>
      )}

      <Modal open={open} title={editing ? `Configure ${editing.chargerCode}` : 'Add charging unit'} onClose={() => setOpen(false)} wide
        footer={<><Button variant="secondary" onClick={() => setOpen(false)}>Cancel</Button><Button type="submit" form="charger-form" loading={saving}>Save</Button></>}>
        <form id="charger-form" onSubmit={save} className="grid gap-4 sm:grid-cols-2" noValidate>
          {formError && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700 sm:col-span-2">{formError}</div>}
          <Field label="Station" htmlFor="st">
            <Select id="st" value={form.stationId} disabled={!!editing} onChange={(e) => setForm({ ...form, stationId: Number(e.target.value) })}>
              {stations.data?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </Select>
          </Field>
          <Field label="Charger code" htmlFor="code" error={errors.chargerCode} hint={editing ? 'Codes are permanent' : 'e.g. VX-BLR-01-E'}>
            <Input id="code" value={form.chargerCode} disabled={!!editing} onChange={(e) => setForm({ ...form, chargerCode: e.target.value.toUpperCase() })} />
          </Field>
          <Field label="Connector" htmlFor="con">
            <Select id="con" value={form.connectorType} onChange={(e) => setForm({ ...form, connectorType: e.target.value as ConnectorType })}>
              {connectorOptions.map((c) => <option key={c} value={c}>{connectorLabel[c]}</option>)}
            </Select>
          </Field>
          <Field label="Max power (kW)" htmlFor="pw" error={errors.maxPowerKw} hint="Type 2 (AC) is limited to 43 kW">
            <Input id="pw" type="number" step="0.1" value={form.maxPowerKw} onChange={(e) => setForm({ ...form, maxPowerKw: Number(e.target.value) })} />
          </Field>
          <Field label="Price per kWh (₹)" htmlFor="pr" error={errors.pricePerKwh}>
            <Input id="pr" type="number" step="0.5" value={form.pricePerKwh} onChange={(e) => setForm({ ...form, pricePerKwh: Number(e.target.value) })} />
          </Field>
          <Field label="Pricing strategy" htmlFor="pl" hint="Running sessions keep the tariff they started with">
            <Select id="pl" value={form.pricingPlan} onChange={(e) => setForm({ ...form, pricingPlan: e.target.value as PricingPlan })}>
              {plans.map((p) => <option key={p.id} value={p.id}>{p.label}</option>)}
            </Select>
          </Field>
        </form>
      </Modal>

      <ConfirmDialog open={!!pending} danger={pending?.action !== 'RESTORE'} loading={busy} onConfirm={runAction} onClose={() => setPending(null)}
        title={pending ? `${actionLabels[pending.action]}?` : ''} confirmLabel={pending ? actionLabels[pending.action] : ''}
        message={pending?.action === 'RESTORE'
          ? <>{pending?.charger.chargerCode} will become available for bookings again.</>
          : <>{pending?.charger.chargerCode} will stop accepting bookings, and its upcoming reservations will be cancelled with a notification to each customer.</>} />
    </div>
  );
}
