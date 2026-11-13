import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Logo } from '../../components/Logo';

export function AuthShell({ title, subtitle, children, footer }: { title: string; subtitle: string; children: ReactNode; footer: ReactNode }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-ink-950 lg:block">
        <div className="absolute -left-24 bottom-0 size-[460px] rounded-full bg-brand-500/25 blur-3xl" />
        <div className="relative flex h-full flex-col justify-between p-12">
          <Link to="/"><Logo dark /></Link>
          <div>
            <p className="text-3xl font-bold leading-snug text-white">“Booked a 60 kW slot on the way to Pune, plugged in, and the bill matched the estimate to the rupee.”</p>
            <p className="mt-4 text-sm text-ink-400">— A happy (simulated) VoltX driver</p>
          </div>
        </div>
      </div>
      <div className="flex items-center justify-center px-4 py-12 sm:px-8">
        <div className="w-full max-w-sm">
          <Link to="/" className="mb-8 inline-block lg:hidden"><Logo /></Link>
          <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
          <p className="mt-1 text-sm text-ink-500">{subtitle}</p>
          <div className="mt-8">{children}</div>
          <div className="mt-6 text-center text-sm text-ink-500">{footer}</div>
        </div>
      </div>
    </div>
  );
}
