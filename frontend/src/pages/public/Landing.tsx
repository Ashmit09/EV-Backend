import { Link } from 'react-router-dom';
import { ArrowRight, BatteryCharging, CalendarCheck, Gauge, MapPin, PlugZap, ReceiptIndianRupee, ShieldCheck } from 'lucide-react';
import { Logo } from '../../components/Logo';
import { Button } from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { stationApi } from '../../api/endpoints';

const features = [
  { icon: MapPin, title: 'Find a charger', text: 'Search stations by city, connector and live availability across the network.' },
  { icon: PlugZap, title: 'Know it fits', text: 'Every charger is checked against your EV’s connectors and power limits before you book.' },
  { icon: CalendarCheck, title: 'Book a slot', text: 'Reserve 15-minute to 4-hour slots. Overlapping bookings are impossible by design.' },
  { icon: Gauge, title: 'Track charging', text: 'Watch battery level, delivered energy and running cost update in real time.' },
  { icon: ReceiptIndianRupee, title: 'Transparent bills', text: 'Itemised invoices with tariff, peak/promo adjustments and GST.' },
  { icon: ShieldCheck, title: 'Secure by default', text: 'JWT authentication, hashed passwords and strict access to your own data.' },
];

export default function Landing() {
  const { user } = useAuth();
  const stations = useAsync(() => stationApi.search({}), []);
  const home = user ? (user.role === 'ADMIN' ? '/admin' : '/app') : '/register';
  const totalChargers = stations.data?.reduce((sum, s) => sum + s.totalChargers, 0);
  const cities = stations.data ? new Set(stations.data.map((s) => s.city)).size : undefined;

  return (
    <div className="min-h-screen bg-white">
      <header className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-8">
        <Logo />
        <nav className="flex items-center gap-2">
          {user ? (
            <Link to={home}><Button>Open dashboard</Button></Link>
          ) : (
            <>
              <Link to="/login"><Button variant="ghost">Sign in</Button></Link>
              <Link to="/register"><Button>Get started</Button></Link>
            </>
          )}
        </nav>
      </header>

      <section className="relative overflow-hidden bg-ink-950">
        <div className="pointer-events-none absolute -right-40 -top-40 size-[520px] rounded-full bg-brand-500/20 blur-3xl" />
        <div className="relative mx-auto grid max-w-7xl gap-12 px-4 py-20 sm:px-8 lg:grid-cols-2 lg:py-28">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium text-brand-300">
              <BatteryCharging className="size-3.5" /> Smart EV charging network
            </span>
            <h1 className="mt-6 text-4xl font-extrabold leading-tight tracking-tight text-white sm:text-5xl">
              Charge smarter.<br /><span className="text-brand-400">Never wait in line.</span>
            </h1>
            <p className="mt-5 max-w-lg text-lg text-ink-300">
              VoltX connects EV drivers with fast, compatible chargers across India — book a slot, plug in and pay
              with a clear, itemised bill.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to={home}><Button size="lg">{user ? 'Go to dashboard' : 'Create free account'} <ArrowRight className="size-4" /></Button></Link>
              {!user && <Link to="/login"><Button size="lg" variant="secondary">I have an account</Button></Link>}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4 self-center">
            {[
              { label: 'Active stations', value: stations.data?.length },
              { label: 'Charging points', value: totalChargers },
              { label: 'Cities covered', value: cities },
              { label: 'Max power', value: stations.data ? `${Math.max(0, ...stations.data.map((s) => s.maxPowerKw))} kW` : undefined },
            ].map((stat) => (
              <div key={stat.label} className="rounded-2xl border border-white/10 bg-white/5 p-6">
                <p className="tabular text-3xl font-bold text-white">{stat.value ?? '—'}</p>
                <p className="mt-1 text-sm text-ink-400">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-20 sm:px-8">
        <h2 className="text-center text-3xl font-bold tracking-tight">Everything a driver needs</h2>
        <p className="mx-auto mt-3 max-w-xl text-center text-ink-500">From discovery to invoice, one platform.</p>
        <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {features.map(({ icon: Icon, title, text }) => (
            <div key={title} className="rounded-2xl border border-ink-200/70 p-6 transition hover:border-brand-300 hover:shadow-md">
              <div className="grid size-11 place-items-center rounded-xl bg-brand-50 text-brand-600"><Icon className="size-5" /></div>
              <h3 className="mt-4 font-semibold">{title}</h3>
              <p className="mt-1.5 text-sm leading-relaxed text-ink-500">{text}</p>
            </div>
          ))}
        </div>
      </section>

      <footer className="border-t border-ink-100 py-8 text-center text-sm text-ink-500">
        VoltX · Academic project · Charging, metering and payments are simulated.
      </footer>
    </div>
  );
}
