import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './layouts/AppLayout';
import Landing from './pages/public/Landing';
import Login from './pages/public/Login';
import Register from './pages/public/Register';
import CustomerDashboard from './pages/customer/Dashboard';
import Stations from './pages/customer/Stations';
import StationDetail from './pages/customer/StationDetail';
import Vehicles from './pages/customer/Vehicles';
import BookCharger from './pages/customer/BookCharger';
import Reservations from './pages/customer/Reservations';
import ActiveSession from './pages/customer/ActiveSession';
import History from './pages/customer/History';
import Invoices from './pages/customer/Invoices';
import InvoiceDetail from './pages/customer/InvoiceDetail';
import Profile from './pages/customer/Profile';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminStations from './pages/admin/AdminStations';
import AdminChargers from './pages/admin/AdminChargers';
import AdminReservations from './pages/admin/AdminReservations';
import AdminSessions from './pages/admin/AdminSessions';
import AdminCustomers from './pages/admin/AdminCustomers';
import AdminBilling from './pages/admin/AdminBilling';
import AdminAnalytics from './pages/admin/AdminAnalytics';
import AdminAudit from './pages/admin/AdminAudit';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<ProtectedRoute role="CUSTOMER" />}>
        <Route path="/app" element={<AppLayout />}>
          <Route index element={<CustomerDashboard />} />
          <Route path="stations" element={<Stations />} />
          <Route path="stations/:id" element={<StationDetail />} />
          <Route path="book/:chargerId" element={<BookCharger />} />
          <Route path="reservations" element={<Reservations />} />
          <Route path="session" element={<ActiveSession />} />
          <Route path="vehicles" element={<Vehicles />} />
          <Route path="history" element={<History />} />
          <Route path="invoices" element={<Invoices />} />
          <Route path="invoices/:id" element={<InvoiceDetail />} />
          <Route path="profile" element={<Profile />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute role="ADMIN" />}>
        <Route path="/admin" element={<AppLayout />}>
          <Route index element={<AdminDashboard />} />
          <Route path="stations" element={<AdminStations />} />
          <Route path="chargers" element={<AdminChargers />} />
          <Route path="reservations" element={<AdminReservations />} />
          <Route path="sessions" element={<AdminSessions />} />
          <Route path="customers" element={<AdminCustomers />} />
          <Route path="billing" element={<AdminBilling />} />
          <Route path="analytics" element={<AdminAnalytics />} />
          <Route path="audit" element={<AdminAudit />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
