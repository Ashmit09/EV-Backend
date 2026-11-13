import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Pencil, Plus, Power, PowerOff } from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { errorMessage, toApiError } from '../../api/client';
import { useAsync } from '../../hooks/useAsync';
import { useToast } from '../../context/ToastContext';
import { Button, Card, ConfirmDialog, ErrorState, Field, Input, Modal, PageHeader, Spinner, StatusBadge, Table, Td } from '../../components/ui';
import { hhmm } from '../../utils/format';
import type { Station, StationRequest } from '../../types';

const blank: StationRequest = { name: '', address: '', city: '', latitude: 12.97, longitude: 77.59, openingTime: '06:00', closingTime: '22:00', open24Hours: false };

export default function AdminStations() {
  const toast = useToast();
  const stations = useAsync(() => adminApi.stations(), []);
  const [editing, setEditing] = useState<Station | null>(null);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<StationRequest>(blank);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [toggling, setToggling] = useState<Station | null>(null);
  const [busy, setBusy] = useState(false);

  const openForm = (s?: Station) => {
    setEditing(s ?? null);
    setForm(s ? { name: s.name, address: s.address, city: s.city, latitude: s.latitude, longitude: s.longitude,
      openingTime: hhmm(s.openingTime), closingTime: hhmm(s.closingTime), open24Hours: s.open24Hours } : blank);
    setErrors({}); setFormError(null); setOpen(true);
  };

  const save = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true); setErrors({}); setFormError(null);
    try {
      const body = { ...form, openingTime: form.open24Hours ? null : form.openingTime, closingTime: form.open24Hours ? null : form.closingTime };
      if (editing) await adminApi.updateStation(editing.id, body); else await adminApi.createStation(body);
      toast(editing ? 'Station updated' : 'Station registered');
      setOpen(false);
      void stations.reload(true);
    } catch (err) {
      const apiError = toApiError(err);
      setErrors(apiError.fieldErrors); setFormError(apiError.message);
    } finally {
      setSaving(false);
    }
  };

  const toggle = async () => {
    if (!toggling) return;
    setBusy(true);
    try {
      await adminApi.setStationEnabled(toggling.id, toggling.status !== 'ACTIVE');
      toast(toggling.status === 'ACTIVE' ? 'Station disabled' : 'Station enabled');
      setToggling(null);
      void stations.reload(true);
    } catch (err) {
      toast(errorMessage(err), 'error');
      setToggling(null);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="animate-fade-in">
      <PageHeader title="Charging stations" subtitle="Register, edit and enable or disable network locations." actions={<Button icon={Plus} onClick={() => openForm()}>Add station</Button>} />
      {stations.loading ? <Spinner /> : stations.error ? <ErrorState message={stations.error} onRetry={stations.reload} /> : (
        <Card>
          <Table head={['Station', 'City', 'Hours', 'Units', 'Available', 'Status', '']}>
            {stations.data?.map((s) => (
              <tr key={s.id} className="hover:bg-ink-50/60">
                <Td><p className="font-medium text-ink-900">{s.name}</p><p className="text-xs text-ink-500">{s.address}</p></Td>
                <Td>{s.city}</Td>
                <Td>{s.open24Hours ? '24 × 7' : `${hhmm(s.openingTime)}–${hhmm(s.closingTime)}`}</Td>
                <Td className="tabular"><Link className="font-semibold text-brand-700 hover:underline" to={`/admin/chargers?station=${s.id}`}>{s.totalChargers}</Link></Td>
                <Td className="tabular">{s.availableChargers}</Td>
                <Td><StatusBadge status={s.status} /></Td>
                <Td>
                  <div className="flex justify-end gap-1">
                    <Button size="sm" variant="ghost" icon={Pencil} onClick={() => openForm(s)}>Edit</Button>
                    <Button size="sm" variant={s.status === 'ACTIVE' ? 'ghost' : 'secondary'} icon={s.status === 'ACTIVE' ? PowerOff : Power}
                      className={s.status === 'ACTIVE' ? 'text-red-600 hover:bg-red-50' : ''} onClick={() => setToggling(s)}>
                      {s.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                    </Button>
                  </div>
                </Td>
              </tr>
            ))}
          </Table>
        </Card>
      )}

      <Modal open={open} title={editing ? `Edit ${editing.name}` : 'Register a station'} onClose={() => setOpen(false)} wide
        footer={<><Button variant="secondary" onClick={() => setOpen(false)}>Cancel</Button><Button type="submit" form="station-form" loading={saving}>Save station</Button></>}>
        <form id="station-form" onSubmit={save} className="grid gap-4 sm:grid-cols-2" noValidate>
          {formError && <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700 sm:col-span-2">{formError}</div>}
          <Field label="Name" htmlFor="n" error={errors.name}><Input id="n" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
          <Field label="City" htmlFor="c" error={errors.city}><Input id="c" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} /></Field>
          <div className="sm:col-span-2"><Field label="Address" htmlFor="a" error={errors.address}><Input id="a" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} /></Field></div>
          <Field label="Latitude" htmlFor="lat" error={errors.latitude}><Input id="lat" type="number" step="0.000001" value={form.latitude} onChange={(e) => setForm({ ...form, latitude: Number(e.target.value) })} /></Field>
          <Field label="Longitude" htmlFor="lng" error={errors.longitude}><Input id="lng" type="number" step="0.000001" value={form.longitude} onChange={(e) => setForm({ ...form, longitude: Number(e.target.value) })} /></Field>
          <label className="flex items-center gap-2 text-sm font-medium sm:col-span-2"><input type="checkbox" className="size-4 accent-brand-600" checked={form.open24Hours} onChange={(e) => setForm({ ...form, open24Hours: e.target.checked })} />Open 24 hours</label>
          {!form.open24Hours && (<>
            <Field label="Opens" htmlFor="o"><Input id="o" type="time" value={form.openingTime ?? ''} onChange={(e) => setForm({ ...form, openingTime: e.target.value })} /></Field>
            <Field label="Closes" htmlFor="cl"><Input id="cl" type="time" value={form.closingTime ?? ''} onChange={(e) => setForm({ ...form, closingTime: e.target.value })} /></Field>
          </>)}
        </form>
      </Modal>

      <ConfirmDialog open={!!toggling} danger={toggling?.status === 'ACTIVE'} loading={busy} onConfirm={toggle} onClose={() => setToggling(null)}
        title={toggling?.status === 'ACTIVE' ? 'Disable station?' : 'Enable station?'}
        confirmLabel={toggling?.status === 'ACTIVE' ? 'Disable station' : 'Enable station'}
        message={toggling?.status === 'ACTIVE'
          ? <>Customers will no longer find <b>{toggling?.name}</b>. All upcoming reservations there will be cancelled and customers notified. Stations with active sessions cannot be disabled.</>
          : <>Make <b>{toggling?.name}</b> visible and bookable again?</>} />
    </div>
  );
}
