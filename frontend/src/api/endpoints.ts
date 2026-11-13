import { api } from './client';
import type {
  AdminOverview, AppNotification, AuditLog, AuthResponse, BookedSlot, Charger, ChargerAdminAction, ConnectorType,
  CostEstimate, CustomerAnalytics, EnergyAnalytics, Invoice, Page, Payment, PaymentMethod, PricingPlan, Reservation,
  ReservationRequest, ReservationStatus, RevenueAnalytics, Session, Station, StationRequest, User, Vehicle, VehicleRequest,
} from '../types';

const data = <T>(promise: Promise<{ data: T }>) => promise.then((response) => response.data);

export const authApi = {
  login: (email: string, password: string) => data<AuthResponse>(api.post('/auth/login', { email, password })),
  register: (body: { fullName: string; email: string; password: string; phone?: string }) =>
    data<AuthResponse>(api.post('/auth/register', body)),
};

export const userApi = {
  me: () => data<User>(api.get('/users/me')),
  update: (body: { fullName: string; phone: string }) => data<User>(api.put('/users/me', body)),
  changePassword: (body: { currentPassword: string; newPassword: string }) => api.put('/users/me/password', body),
  notifications: () => data<AppNotification[]>(api.get('/notifications')),
};

export const vehicleApi = {
  list: () => data<Vehicle[]>(api.get('/vehicles')),
  get: (id: number) => data<Vehicle>(api.get(`/vehicles/${id}`)),
  create: (body: VehicleRequest) => data<Vehicle>(api.post('/vehicles', body)),
  update: (id: number, body: VehicleRequest) => data<Vehicle>(api.put(`/vehicles/${id}`, body)),
  remove: (id: number) => api.delete(`/vehicles/${id}`),
};

export const stationApi = {
  search: (params: { city?: string; q?: string; connector?: ConnectorType | ''; availableOnly?: boolean }) =>
    data<Station[]>(api.get('/stations', { params: { ...params, connector: params.connector || undefined } })),
  cities: () => data<string[]>(api.get('/stations/cities')),
  get: (id: number, vehicleId?: number) => data<Station>(api.get(`/stations/${id}`, { params: { vehicleId } })),
  charger: (id: number) => data<Charger>(api.get(`/chargers/${id}`)),
  bookedSlots: (id: number, date: string) => data<BookedSlot[]>(api.get(`/chargers/${id}/booked-slots`, { params: { date } })),
};

export const reservationApi = {
  create: (body: ReservationRequest) => data<Reservation>(api.post('/reservations', body)),
  estimate: (body: ReservationRequest) => data<CostEstimate>(api.post('/reservations/estimate', body)),
  mine: () => data<Reservation[]>(api.get('/reservations/my')),
  cancel: (id: number, reason?: string) => data<Reservation>(api.patch(`/reservations/${id}/cancel`, { reason })),
};

export const sessionApi = {
  start: (reservationId: number, targetBatteryPercentage?: number) =>
    data<Session>(api.post('/sessions/start', { reservationId, targetBatteryPercentage })),
  active: () => api.get<Session>('/sessions/active').then((response) => (response.status === 204 ? null : response.data)),
  get: (id: number) => data<Session>(api.get(`/sessions/${id}`)),
  stop: (id: number) => data<Session>(api.post(`/sessions/${id}/stop`)),
  history: () => data<Session[]>(api.get('/sessions')),
};

export const invoiceApi = {
  list: () => data<Invoice[]>(api.get('/invoices')),
  get: (id: number) => data<Invoice>(api.get(`/invoices/${id}`)),
  pay: (id: number, method: PaymentMethod) => data<Invoice>(api.post(`/invoices/${id}/pay`, { method })),
};

export const analyticsApi = {
  customer: () => data<CustomerAnalytics>(api.get('/analytics/customer')),
};

export const adminApi = {
  overview: () => data<AdminOverview>(api.get('/admin/analytics/overview')),
  revenue: (months = 6) => data<RevenueAnalytics>(api.get('/admin/analytics/revenue', { params: { months } })),
  energy: (months = 6) => data<EnergyAnalytics>(api.get('/admin/analytics/energy', { params: { months } })),
  stations: () => data<Station[]>(api.get('/admin/stations')),
  createStation: (body: StationRequest) => data<Station>(api.post('/admin/stations', body)),
  updateStation: (id: number, body: StationRequest) => data<Station>(api.put(`/admin/stations/${id}`, body)),
  setStationEnabled: (id: number, enabled: boolean) =>
    data<Station>(api.patch(`/admin/stations/${id}/${enabled ? 'enable' : 'disable'}`)),
  chargers: (stationId?: number) => data<Charger[]>(api.get('/admin/chargers', { params: { stationId } })),
  createCharger: (body: { stationId: number; chargerCode: string; connectorType: ConnectorType; maxPowerKw: number; pricePerKwh: number; pricingPlan: PricingPlan }) =>
    data<Charger>(api.post('/admin/chargers', body)),
  updateCharger: (id: number, body: { connectorType: ConnectorType; maxPowerKw: number; pricePerKwh: number; pricingPlan: PricingPlan }) =>
    data<Charger>(api.put(`/admin/chargers/${id}`, body)),
  chargerAction: (id: number, action: ChargerAdminAction) => data<Charger>(api.patch(`/admin/chargers/${id}/status`, { action })),
  users: () => data<User[]>(api.get('/admin/users')),
  setUserEnabled: (id: number, enabled: boolean) => data<User>(api.patch(`/admin/users/${id}/${enabled ? 'enable' : 'disable'}`)),
  reservations: (status?: ReservationStatus | '') =>
    data<Reservation[]>(api.get('/admin/reservations', { params: { status: status || undefined } })),
  cancelReservation: (id: number, reason?: string) => data<Reservation>(api.patch(`/admin/reservations/${id}/cancel`, { reason })),
  activeSessions: () => data<Session[]>(api.get('/admin/sessions/active')),
  stopSession: (id: number) => data<Session>(api.post(`/admin/sessions/${id}/stop`)),
  invoices: () => data<Invoice[]>(api.get('/admin/invoices')),
  payments: () => data<Payment[]>(api.get('/admin/payments')),
  auditLogs: (page: number, action?: string) =>
    data<Page<AuditLog>>(api.get('/admin/audit-logs', { params: { page, size: 25, action: action || undefined } })),
};
