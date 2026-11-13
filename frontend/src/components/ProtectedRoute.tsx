import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Spinner } from './ui';
import type { Role } from '../types';

export function ProtectedRoute({ role }: { role: Role }) {
  const { user, initializing } = useAuth();
  const location = useLocation();
  if (initializing) return <div className="min-h-screen"><Spinner label="Checking your session…" /></div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (user.role !== role) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/app'} replace />;
  return <Outlet />;
}
