export type Role = 'CUSTOMER' | 'ADMIN';
export type ConnectorType = 'CCS2' | 'TYPE_2' | 'CHADEMO' | 'GBT';
export type VehicleType = 'CAR' | 'BIKE' | 'SCOOTER';
export type ChargerStatus = 'AVAILABLE' | 'RESERVED' | 'CHARGING' | 'OUT_OF_SERVICE' | 'MAINTENANCE';
export type StationStatus = 'ACTIVE' | 'DISABLED';
export type ReservationStatus = 'CONFIRMED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED';
export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED';
export type InvoiceStatus = 'UNPAID' | 'PAID';
export type PaymentMethod = 'UPI' | 'CARD' | 'WALLET';
export type PricingPlan = 'STANDARD' | 'PEAK_HOUR' | 'PROMOTIONAL';
export type ChargerAdminAction = 'MARK_OUT_OF_SERVICE' | 'MARK_MAINTENANCE' | 'RESTORE';

export interface User {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export interface Vehicle {
  id: number;
  vehicleType: VehicleType;
  manufacturer: string;
  model: string;
  displayName: string;
  registrationNumber: string;
  batteryCapacityKwh: number;
  maxChargingPowerKw: number;
  currentBatteryPercentage: number;
  supportedConnectors: ConnectorType[];
}

export interface VehicleRequest {
  vehicleType: VehicleType;
  manufacturer: string;
  model: string;
  registrationNumber: string;
  batteryCapacityKwh: number;
  maxChargingPowerKw: number;
  currentBatteryPercentage: number;
  supportedConnectors: ConnectorType[];
}

export interface Charger {
  id: number;
  stationId: number;
  stationName: string;
  city: string;
  chargerCode: string;
  connectorType: ConnectorType;
  connectorLabel: string;
  dcFast: boolean;
  maxPowerKw: number;
  pricePerKwh: number;
  pricingPlan: PricingPlan;
  pricingPlanLabel: string;
  status: ChargerStatus;
  bookable: boolean;
  compatible: boolean | null;
  incompatibilityReason: string | null;
}

export interface Station {
  id: number;
  name: string;
  address: string;
  city: string;
  latitude: number;
  longitude: number;
  openingTime: string;
  closingTime: string;
  open24Hours: boolean;
  status: StationStatus;
  totalChargers: number;
  availableChargers: number;
  connectorTypes: ConnectorType[];
  maxPowerKw: number;
  minPricePerKwh: number;
  chargers: Charger[];
}

export interface StationRequest {
  name: string;
  address: string;
  city: string;
  latitude: number;
  longitude: number;
  openingTime: string | null;
  closingTime: string | null;
  open24Hours: boolean;
}

export interface BookedSlot {
  startTime: string;
  endTime: string;
}

export interface ReservationRequest {
  vehicleId: number;
  chargingUnitId: number;
  startTime: string;
  durationMinutes: number;
}

export interface Reservation {
  id: number;
  status: ReservationStatus;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  vehicleId: number;
  vehicleName: string;
  registrationNumber: string;
  chargerId: number;
  chargerCode: string;
  connectorType: ConnectorType;
  maxPowerKw: number;
  pricePerKwh: number;
  stationId: number;
  stationName: string;
  city: string;
  canStart: boolean;
  canCancel: boolean;
  cancellationReason: string | null;
  createdAt: string;
  customerName: string | null;
  customerEmail: string | null;
}

export interface CostEstimate {
  effectivePowerKw: number;
  startBatteryPercentage: number;
  estimatedEndBatteryPercentage: number;
  targetBatteryPercentage: number;
  estimatedEnergyKwh: number;
  pricingPlan: PricingPlan;
  energyCost: number;
  adjustmentLabel: string | null;
  adjustmentAmount: number;
  subtotal: number;
  taxAmount: number;
  estimatedTotal: number;
  simulationModel: string;
}

export interface Session {
  id: number;
  status: SessionStatus;
  reservationId: number | null;
  vehicleId: number;
  vehicleName: string;
  registrationNumber: string;
  batteryCapacityKwh: number;
  chargerId: number;
  chargerCode: string;
  connectorType: ConnectorType;
  stationName: string;
  city: string;
  startTime: string;
  endTime: string | null;
  reservationEndTime: string | null;
  initialBatteryPercentage: number;
  currentBatteryPercentage: number;
  targetBatteryPercentage: number;
  energyKwh: number;
  simulatedMinutes: number;
  currentPowerKw: number;
  effectivePowerKw: number;
  pricePerKwh: number;
  pricingPlan: PricingPlan;
  cost: number;
  targetReached: boolean;
  invoiceId: number | null;
  customerName: string | null;
  customerEmail: string | null;
}

export interface Payment {
  id: number;
  invoiceId: number;
  invoiceNumber: string;
  customerEmail: string;
  amount: number;
  method: PaymentMethod;
  status: 'SUCCESS' | 'FAILED';
  transactionRef: string;
  message: string | null;
  processedAt: string;
}

export interface Invoice {
  id: number;
  invoiceNumber: string;
  status: InvoiceStatus;
  issuedAt: string;
  paidAt: string | null;
  sessionId: number;
  stationName: string;
  stationAddress: string;
  city: string;
  chargerCode: string;
  connectorLabel: string;
  vehicleName: string;
  registrationNumber: string;
  sessionStart: string;
  sessionEnd: string | null;
  simulatedDurationMinutes: number | null;
  initialBatteryPercentage: number | null;
  finalBatteryPercentage: number | null;
  energyKwh: number;
  ratePerKwh: number;
  pricingPlan: PricingPlan;
  pricingPlanLabel: string;
  energyCost: number;
  adjustmentLabel: string | null;
  adjustmentAmount: number;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  totalAmount: number;
  customerName: string | null;
  customerEmail: string | null;
  payments: Payment[];
}

export interface MonthlyPoint {
  month: string;
  energyKwh: number;
  revenue: number;
  collected: number;
  sessions: number;
}

export interface CustomerAnalytics {
  totalSessions: number;
  totalEnergyKwh: number;
  totalSpent: number;
  outstandingAmount: number;
  averageSessionMinutes: number;
  upcomingReservations: number;
  vehicles: number;
  monthly: MonthlyPoint[];
  recentSessions: Session[];
}

export interface StationUtilization {
  stationId: number;
  stationName: string;
  city: string;
  totalChargers: number;
  busyChargers: number;
  occupancyPercent: number;
  sessionsLast30Days: number;
  energyLast30DaysKwh: number;
  revenueLast30Days: number;
}

export interface AdminOverview {
  totalUsers: number;
  totalCustomers: number;
  totalStations: number;
  activeStations: number;
  totalChargers: number;
  availableChargers: number;
  activeSessions: number;
  reservationsToday: number;
  totalEnergyKwh: number;
  totalRevenue: number;
  collectedRevenue: number;
  pendingRevenue: number;
  chargerStatusDistribution: Record<ChargerStatus, number>;
  stationUtilization: StationUtilization[];
}

export interface RevenueAnalytics {
  monthly: MonthlyPoint[];
  byPricingPlan: Record<string, number>;
  byPaymentMethod: Record<string, number>;
  byCity: Record<string, number>;
}

export interface EnergyAnalytics {
  monthly: MonthlyPoint[];
  byConnector: Record<string, number>;
  byVehicleType: Record<string, number>;
  byCity: Record<string, number>;
}

export interface AuditLog {
  id: number;
  actorEmail: string;
  action: string;
  entityType: string;
  entityId: number | null;
  details: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AppNotification {
  subject: string;
  message: string;
  createdAt: string;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  fieldErrors: Record<string, string>;
}
