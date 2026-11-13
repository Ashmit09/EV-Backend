import { useState, type FormEvent } from 'react';
import { Bike, Car, Pencil, Plus, Trash2, Zap } from 'lucide-react';
import { vehicleApi } from '../../api/endpoints';
import { toApiError, errorMessage } from '../../api/client';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { BatteryBar, Badge, Button, Card, ConfirmDialog, EmptyState, ErrorState, Field, Input, Modal, PageHeader, Select, Spinner, cx } from '../../components/ui';
import { connectorLabel, connectorOptions, vehicleTypeLabel } from '../../utils/format';
import type { ConnectorType, Vehicle, VehicleRequest, VehicleType } from '../../types';

const empty: VehicleRequest = {
  vehicleType: 'CAR', manufacturer: '', model: '', registrationNumber: '', batteryCapacityKwh: 40,
  maxChargingPowerKw: 50, currentBatteryPercentage: 50, supportedConnectors: ['CCS2', 'TYPE_2'],
};

const typeHints: Record<VehicleType, string> = {
  CAR: 'Battery 15–200 kWh, up to 350 kW. Cars stop DC charging at 80% by default.',
  BIKE: 'Battery 2–25 kWh, up to 30 kW. Cannot use ultra-fast chargers above 60 kW.',
  SCOOTER: 'Battery 1–8 kWh, up to 7.4 kW. AC charging only.',
};

export default function Vehicles() {
  const toast = useToast();
  const vehicles = useAsync(() => vehicleApi.list(), []);
  const [editing, setEditing] = useState<Vehicle | null>(null);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<VehicleRequest>(empty);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState<Vehicle | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  const openCreate = () => { setEditing(null); setForm(empty); setErrors({}); setFormError(null); setOpen(true); };
  const openEdit = (v: Vehicle) => {
    setEditing(v);
    setForm({ vehicleType: v.vehicleType, manufacturer: v.manufacturer, model: v.model, registrationNumber: v.registrationNumber,
      batteryCapacityKwh: v.batteryCapacityKwh, maxChargingPowerKw: v.maxChargingPowerKw,
      currentBatteryPercentage: v.currentBatteryPercentage, supportedConnectors: v.supportedConnectors });
    setErrors({}); setFormError(null); setOpen(true);
  };

  const toggleConnector = (c: ConnectorType) => setForm((f) => ({
    ...f, supportedConnectors: f.supportedConnectors.includes(c) ? f.supportedConnectors.filter((x) => x !== c) : [...f.supportedConnectors, c],
  }));

  const save = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true); setErrors({}); setFormError(null);
    try {
      if (editing) await vehicleApi.update(editing.id, form);
      else await vehicleApi.create(form);
      toast(editing ? 'Vehicle updated' : 'Vehicle added');
      setOpen(false);
      void vehicles.reload(true);
    } catch (e) {
      const apiError = toApiError(e);
      setErrors(apiError.fieldErrors);
      setFormError(apiError.message);
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    if (!deleting) return;
    setDeleteBusy(true);
    try {
      await vehicleApi.remove(deleting.id);
      toast('Vehicle removed');
      setDeleting(null);
      void vehicles.reload(true);
    } catch (e) {
      toast(errorMessage(e), 'error');
      setDeleting(null);
    } finally {
      setDeleteBusy(false);
    }
  };

  return (
    <div className="animate-fade-in">
      <PageHeader title="My vehicles" subtitle="Your EVs are used to check charger compatibility and estimate charging."
        actions={<Button icon={Plus} onClick={openCreate}>Add vehicle</Button>} />

      {vehicles.loading ? <Spinner /> : vehicles.error ? <ErrorState message={vehicles.error} onRetry={vehicles.reload} /> :
        vehicles.data?.length === 0 ? (
          <Card><EmptyState icon={Car} title="No vehicles yet" description="Add your EV to see which chargers it can use." action={<Button icon={Plus} onClick={openCreate}>Add vehicle</Button>} /></Card>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {vehicles.data?.map((v) => (
              <Card key={v.id} className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="grid size-11 place-items-center rounded-xl bg-ink-900 text-brand-400">
                      {v.vehicleType === 'CAR' ? <Car className="size-5" /> : <Bike className="size-5" />}
                    </div>
                    <div>
                      <p className="font-semibold text-ink-900">{v.displayName}</p>
                      <p className="text-sm text-ink-500">{v.registrationNumber}</p>
                    </div>
                  </div>
                  <Badge tone="dark">{vehicleTypeLabel[v.vehicleType]}</Badge>
                </div>
                <div className="mt-5">
                  <div className="mb-1.5 flex justify-between text-sm"><span className="text-ink-500">Battery</span><span className="tabular font-semibold">{v.currentBatteryPercentage}%</span></div>
                  <BatteryBar value={v.currentBatteryPercentage} />
                </div>
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                  <div><p className="text-xs text-ink-500">Capacity</p><p className="font-semibold">{v.batteryCapacityKwh} kWh</p></div>
                  <div><p className="text-xs text-ink-500">Max charging</p><p className="flex items-center gap-1 font-semibold"><Zap className="size-3.5 text-brand-600" />{v.maxChargingPowerKw} kW</p></div>
                </div>
                <div className="mt-4 flex flex-wrap gap-1.5">{v.supportedConnectors.map((c) => <Badge key={c}>{connectorLabel[c]}</Badge>)}</div>
                <div className="mt-5 flex gap-2 border-t border-ink-100 pt-4">
                  <Button variant="secondary" size="sm" icon={Pencil} onClick={() => openEdit(v)}>Edit</Button>
                  <Button variant="ghost" size="sm" icon={Trash2} className="text-red-600 hover:bg-red-50 hover:text-red-700" onClick={() => setDeleting(v)}>Remove</Button>
                </div>
              </Card>
            ))}
          </div>
        )}

      <Modal open={open} title={editing ? 'Edit vehicle' : 'Add a vehicle'} onClose={() => setOpen(false)} wide
        footer={<><Button variant="secondary" onClick={() => setOpen(false)}>Cancel</Button><Button type="submit" form="vehicle-form" loading={saving}>{editing ? 'Save changes' : 'Add vehicle'}</Button></>}>
        <form id="vehicle-form" onSubmit={save} className="grid gap-4 sm:grid-cols-2" noValidate>
          {formError && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700 sm:col-span-2" role="alert">{formError}</div>}
          <Field label="Vehicle type" htmlFor="type" hint={typeHints[form.vehicleType]}>
            <Select id="type" value={form.vehicleType} disabled={!!editing} onChange={(e) => setForm({ ...form, vehicleType: e.target.value as VehicleType })}>
              <option value="CAR">Electric car</option><option value="BIKE">Electric bike</option><option value="SCOOTER">Electric scooter</option>
            </Select>
          </Field>
          <Field label="Registration number" htmlFor="reg" error={errors.registrationNumber} hint={editing ? 'Cannot be changed' : 'e.g. KA01AB1234'}>
            <Input id="reg" value={form.registrationNumber} disabled={!!editing} invalid={!!errors.registrationNumber}
              onChange={(e) => setForm({ ...form, registrationNumber: e.target.value.toUpperCase() })} />
          </Field>
          <Field label="Manufacturer" htmlFor="make" error={errors.manufacturer}>
            <Input id="make" value={form.manufacturer} invalid={!!errors.manufacturer} onChange={(e) => setForm({ ...form, manufacturer: e.target.value })} />
          </Field>
          <Field label="Model" htmlFor="model" error={errors.model}>
            <Input id="model" value={form.model} invalid={!!errors.model} onChange={(e) => setForm({ ...form, model: e.target.value })} />
          </Field>
          <Field label="Battery capacity (kWh)" htmlFor="cap" error={errors.batteryCapacityKwh}>
            <Input id="cap" type="number" step="0.1" min="1" value={form.batteryCapacityKwh} onChange={(e) => setForm({ ...form, batteryCapacityKwh: Number(e.target.value) })} />
          </Field>
          <Field label="Max charging power (kW)" htmlFor="pow" error={errors.maxChargingPowerKw}>
            <Input id="pow" type="number" step="0.1" min="0.5" value={form.maxChargingPowerKw} onChange={(e) => setForm({ ...form, maxChargingPowerKw: Number(e.target.value) })} />
          </Field>
          <Field label={`Current battery: ${form.currentBatteryPercentage}%`} htmlFor="bat" error={errors.currentBatteryPercentage}>
            <input id="bat" type="range" min={0} max={100} value={form.currentBatteryPercentage} className="w-full accent-brand-600"
              onChange={(e) => setForm({ ...form, currentBatteryPercentage: Number(e.target.value) })} />
          </Field>
          <Field label="Supported connectors" error={errors.supportedConnectors}>
            <div className="flex flex-wrap gap-2">
              {connectorOptions.map((c) => (
                <button type="button" key={c} onClick={() => toggleConnector(c)} aria-pressed={form.supportedConnectors.includes(c)}
                  className={cx('rounded-lg border px-3 py-1.5 text-sm font-medium transition', form.supportedConnectors.includes(c)
                    ? 'border-brand-500 bg-brand-50 text-brand-800' : 'border-ink-200 text-ink-600 hover:bg-ink-50')}>
                  {connectorLabel[c]}
                </button>
              ))}
            </div>
          </Field>
        </form>
      </Modal>

      <ConfirmDialog open={!!deleting} danger title="Remove vehicle?" confirmLabel="Remove" loading={deleteBusy}
        message={<>Remove <b>{deleting?.displayName}</b> ({deleting?.registrationNumber})? Vehicles with bookings or charging history are kept for your records and cannot be removed.</>}
        onConfirm={remove} onClose={() => setDeleting(null)} />
    </div>
  );
}
