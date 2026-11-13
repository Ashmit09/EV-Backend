import type { ConnectorType, VehicleType } from '../types';

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
const inrShort = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const number = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 });

export const money = (value: number | null | undefined) => inr.format(value ?? 0);
export const moneyShort = (value: number | null | undefined) => inrShort.format(value ?? 0);
export const kwh = (value: number | null | undefined) => `${number.format(value ?? 0)} kWh`;
export const num = (value: number | null | undefined) => number.format(value ?? 0);
export const pct = (value: number | null | undefined) => `${Math.round((value ?? 0) * 10) / 10}%`;

/** Backend sends ISO local date-times (IST) without an offset; parse them as local time. */
export const parseLocal = (iso: string) => new Date(iso);

export const dateTime = (iso: string | null | undefined) =>
  iso ? parseLocal(iso).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—';
export const dateOnly = (iso: string | null | undefined) =>
  iso ? parseLocal(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—';
export const timeOnly = (iso: string | null | undefined) =>
  iso ? parseLocal(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : '—';
export const hhmm = (time: string) => time.slice(0, 5);

export const minutes = (value: number | null | undefined) => {
  const total = Math.round(value ?? 0);
  const h = Math.floor(total / 60);
  const m = total % 60;
  return h > 0 ? `${h}h ${m}m` : `${m} min`;
};

export const connectorLabel: Record<ConnectorType, string> = { CCS2: 'CCS2', TYPE_2: 'Type 2', CHADEMO: 'CHAdeMO', GBT: 'GB/T' };
export const connectorOptions: ConnectorType[] = ['CCS2', 'TYPE_2', 'CHADEMO', 'GBT'];
export const vehicleTypeLabel: Record<VehicleType, string> = { CAR: 'Car', BIKE: 'Bike', SCOOTER: 'Scooter' };

export const humanize = (value: string) =>
  value.toLowerCase().split('_').map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');

/** "YYYY-MM-DDTHH:mm:ss" in the browser's local time, which is what the backend expects. */
export const toLocalIso = (date: Date) => {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
};
export const toDateInput = (date: Date) => toLocalIso(date).slice(0, 10);
