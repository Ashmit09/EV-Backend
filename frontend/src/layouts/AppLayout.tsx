import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  Bell, BarChart3, BatteryCharging, CalendarClock, Car, ClipboardList, Cpu, FileText, History, LayoutDashboard,
  LogOut, MapPin, Menu, ReceiptIndianRupee, ScrollText, Users, UserCircle, X,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { Logo } from '../components/Logo';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../api/endpoints';
import { dateTime } from '../utils/format';
import type { AppNotification } from '../types';
import { useInterval } from '../hooks/useAsync';

interface NavItem { to: string; label: string; icon: LucideIcon; end?: boolean }

const customerNav: NavItem[] = [
  { to: '/app', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/app/stations', label: 'Find stations', icon: MapPin },
  { to: '/app/reservations', label: 'My bookings', icon: CalendarClock },
  { to: '/app/session', label: 'Live charging', icon: BatteryCharging },
  { to: '/app/vehicles', label: 'My vehicles', icon: Car },
  { to: '/app/history', label: 'Charging history', icon: History },
  { to: '/app/invoices', label: 'Invoices', icon: FileText },
  { to: '/app/profile', label: 'Profile', icon: UserCircle },
];

const adminNav: NavItem[] = [
  { to: '/admin', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/admin/stations', label: 'Stations', icon: MapPin },
  { to: '/admin/chargers', label: 'Charging units', icon: Cpu },
  { to: '/admin/reservations', label: 'Reservations', icon: ClipboardList },
  { to: '/admin/sessions', label: 'Live sessions', icon: BatteryCharging },
  { to: '/admin/customers', label: 'Customers', icon: Users },
  { to: '/admin/billing', label: 'Billing & revenue', icon: ReceiptIndianRupee },
  { to: '/admin/analytics', label: 'Energy analytics', icon: BarChart3 },
  { to: '/admin/audit', label: 'Audit logs', icon: ScrollText },
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const nav = user?.role === 'ADMIN' ? adminNav : customerNav;

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const sidebar = (
    <div className="flex h-full flex-col bg-ink-950 text-ink-300">
      <div className="flex h-16 items-center justify-between px-5">
        <Logo dark />
        <button className="lg:hidden text-ink-400" onClick={() => setMobileOpen(false)} aria-label="Close menu"><X className="size-5" /></button>
      </div>
      <p className="px-5 pb-2 pt-3 text-[11px] font-semibold uppercase tracking-wider text-ink-500">
        {user?.role === 'ADMIN' ? 'Operations console' : 'Charging'}
      </p>
      <nav className="flex-1 space-y-0.5 overflow-y-auto px-3">
        {nav.map(({ to, label, icon: Icon, end }) => (
          <NavLink key={to} to={to} end={end} onClick={() => setMobileOpen(false)}
            className={({ isActive }) => `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
              isActive ? 'bg-white/10 text-white' : 'hover:bg-white/5 hover:text-white'}`}>
            {({ isActive }) => (<><Icon className={`size-4.5 ${isActive ? 'text-brand-400' : ''}`} />{label}</>)}
          </NavLink>
        ))}
      </nav>
      <div className="border-t border-white/10 p-4">
        <div className="mb-3 min-w-0">
          <p className="truncate text-sm font-semibold text-white">{user?.fullName}</p>
          <p className="truncate text-xs text-ink-400">{user?.email}</p>
        </div>
        <button onClick={handleLogout} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-ink-300 hover:bg-white/5 hover:text-white">
          <LogOut className="size-4" /> Sign out
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen lg:pl-64">
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 lg:block">{sidebar}</aside>
      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="absolute inset-0 bg-ink-950/60" onClick={() => setMobileOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-72">{sidebar}</aside>
        </div>
      )}
      <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-ink-200/70 bg-white/85 px-4 backdrop-blur sm:px-8">
        <button className="rounded-lg p-2 text-ink-600 hover:bg-ink-100 lg:hidden" onClick={() => setMobileOpen(true)} aria-label="Open menu">
          <Menu className="size-5" />
        </button>
        <div className="hidden text-sm text-ink-500 lg:block">
          {user?.role === 'ADMIN' ? 'VoltX network operations' : `Welcome back, ${user?.fullName.split(' ')[0]}`}
        </div>
        <NotificationBell />
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-8 sm:py-8">
        <Outlet />
      </main>
    </div>
  );
}

function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<AppNotification[]>([]);
  const [seen, setSeen] = useState(0);
  const ref = useRef<HTMLDivElement>(null);

  const load = () => userApi.notifications().then(setItems).catch(() => undefined);
  useEffect(() => { void load(); }, []);
  useInterval(load, 15000);

  useEffect(() => {
    const close = (e: MouseEvent) => ref.current && !ref.current.contains(e.target as Node) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const unread = Math.max(0, items.length - seen);
  return (
    <div className="relative" ref={ref}>
      <button onClick={() => { setOpen(!open); setSeen(items.length); void load(); }}
        className="relative rounded-lg p-2 text-ink-600 hover:bg-ink-100" aria-label="Notifications">
        <Bell className="size-5" />
        {unread > 0 && <span className="absolute right-1 top-1 grid size-4 place-items-center rounded-full bg-brand-500 text-[10px] font-bold text-ink-950">{unread}</span>}
      </button>
      {open && (
        <div className="animate-fade-in absolute right-0 mt-2 w-[min(92vw,360px)] overflow-hidden rounded-xl border border-ink-200 bg-white shadow-xl">
          <p className="border-b border-ink-100 px-4 py-3 text-sm font-semibold">Notifications</p>
          <div className="max-h-96 divide-y divide-ink-100 overflow-y-auto">
            {items.length === 0 && <p className="px-4 py-8 text-center text-sm text-ink-500">You're all caught up.</p>}
            {items.map((n, i) => (
              <div key={i} className="px-4 py-3">
                <p className="text-sm font-medium text-ink-900">{n.subject}</p>
                <p className="mt-0.5 text-sm text-ink-600">{n.message}</p>
                <p className="mt-1 text-xs text-ink-400">{dateTime(n.createdAt)}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
