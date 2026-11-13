import { useEffect, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from 'react';
import { AlertCircle, Inbox, Loader2, RefreshCw, X } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

const cx = (...classes: (string | false | null | undefined)[]) => classes.filter(Boolean).join(' ');

// ---------------------------------------------------------------- Button
type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'dark';
const variants: Record<Variant, string> = {
  primary: 'bg-brand-600 text-white hover:bg-brand-700 shadow-sm',
  dark: 'bg-ink-900 text-white hover:bg-ink-800 shadow-sm',
  secondary: 'bg-white text-ink-800 border border-ink-200 hover:bg-ink-50 shadow-sm',
  ghost: 'text-ink-600 hover:bg-ink-100 hover:text-ink-900',
  danger: 'bg-red-600 text-white hover:bg-red-700 shadow-sm',
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  icon?: LucideIcon;
}

export function Button({ variant = 'primary', size = 'md', loading, icon: Icon, className, children, disabled, ...rest }: ButtonProps) {
  const sizes = { sm: 'h-8 px-3 text-xs gap-1.5', md: 'h-10 px-4 text-sm gap-2', lg: 'h-12 px-6 text-base gap-2' };
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={cx('inline-flex items-center justify-center rounded-lg font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50',
        variants[variant], sizes[size], className)}
    >
      {loading ? <Loader2 className="size-4 animate-spin" /> : Icon ? <Icon className="size-4" /> : null}
      {children}
    </button>
  );
}

// ---------------------------------------------------------------- Card
export function Card({ className, children }: { className?: string; children: ReactNode }) {
  return <div className={cx('rounded-2xl border border-ink-200/70 bg-white shadow-[0_1px_2px_rgba(16,24,40,0.04)]', className)}>{children}</div>;
}

export function CardHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-ink-100 px-5 py-4">
      <div>
        <h3 className="text-sm font-semibold text-ink-900">{title}</h3>
        {subtitle && <p className="mt-0.5 text-xs text-ink-500">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

// ---------------------------------------------------------------- Badges
type Tone = 'green' | 'amber' | 'blue' | 'red' | 'gray' | 'violet' | 'dark';
const tones: Record<Tone, string> = {
  green: 'bg-brand-50 text-brand-700 ring-brand-200',
  amber: 'bg-amber-50 text-amber-700 ring-amber-200',
  blue: 'bg-sky-50 text-sky-700 ring-sky-200',
  red: 'bg-red-50 text-red-700 ring-red-200',
  gray: 'bg-ink-100 text-ink-600 ring-ink-200',
  violet: 'bg-violet-50 text-violet-700 ring-violet-200',
  dark: 'bg-ink-900 text-white ring-ink-900',
};

export function Badge({ tone = 'gray', children, dot }: { tone?: Tone; children: ReactNode; dot?: boolean }) {
  return (
    <span className={cx('inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset', tones[tone])}>
      {dot && <span className="size-1.5 rounded-full bg-current" />}
      {children}
    </span>
  );
}

const statusTones: Record<string, Tone> = {
  AVAILABLE: 'green', RESERVED: 'amber', CHARGING: 'blue', OUT_OF_SERVICE: 'red', MAINTENANCE: 'violet',
  ACTIVE: 'green', DISABLED: 'gray',
  CONFIRMED: 'amber', COMPLETED: 'gray', CANCELLED: 'red', EXPIRED: 'gray',
  IN_PROGRESS: 'blue', PAID: 'green', UNPAID: 'amber', SUCCESS: 'green', FAILED: 'red',
};

export function StatusBadge({ status }: { status: string }) {
  const label = status.toLowerCase().split('_').map((w) => w[0].toUpperCase() + w.slice(1)).join(' ');
  return <Badge tone={statusTones[status] ?? 'gray'} dot>{label}</Badge>;
}

// ---------------------------------------------------------------- Form controls
export function Field({ label, error, hint, children, htmlFor }: { label: string; error?: string; hint?: string; children: ReactNode; htmlFor?: string }) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={htmlFor} className="block text-sm font-medium text-ink-700">{label}</label>
      {children}
      {error ? <p className="text-xs text-red-600">{error}</p> : hint ? <p className="text-xs text-ink-500">{hint}</p> : null}
    </div>
  );
}

const controlClass = 'block w-full rounded-lg border border-ink-200 bg-white px-3 h-10 text-sm text-ink-900 placeholder:text-ink-400 shadow-sm transition focus:border-brand-500 focus:outline-none focus:ring-4 focus:ring-brand-100 disabled:bg-ink-50 disabled:text-ink-500';

export function Input({ className, invalid, ...rest }: InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }) {
  return <input {...rest} aria-invalid={invalid || undefined} className={cx(controlClass, invalid && 'border-red-400 focus:border-red-500 focus:ring-red-100', className)} />;
}

export function Select({ className, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...rest} className={cx(controlClass, 'pr-8', className)}>{children}</select>;
}

// ---------------------------------------------------------------- Feedback states
export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-16 text-sm text-ink-500" role="status">
      <Loader2 className="size-5 animate-spin text-brand-600" /> {label}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-2xl border border-red-200 bg-red-50/60 px-6 py-10 text-center">
      <AlertCircle className="size-8 text-red-500" />
      <p className="max-w-md text-sm text-red-800">{message}</p>
      {onRetry && <Button variant="secondary" size="sm" icon={RefreshCw} onClick={onRetry}>Try again</Button>}
    </div>
  );
}

export function EmptyState({ icon: Icon = Inbox, title, description, action }: { icon?: LucideIcon; title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 px-6 py-14 text-center">
      <div className="grid size-12 place-items-center rounded-2xl bg-ink-100 text-ink-500"><Icon className="size-6" /></div>
      <div>
        <p className="font-semibold text-ink-900">{title}</p>
        {description && <p className="mt-1 max-w-sm text-sm text-ink-500">{description}</p>}
      </div>
      {action}
    </div>
  );
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-ink-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-ink-500">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function StatCard({ label, value, icon: Icon, hint, accent = false }: { label: string; value: ReactNode; icon: LucideIcon; hint?: string; accent?: boolean }) {
  return (
    <Card className={cx('p-5', accent && 'border-ink-900 bg-ink-900 text-white')}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className={cx('text-xs font-medium uppercase tracking-wide', accent ? 'text-ink-300' : 'text-ink-500')}>{label}</p>
          <p className={cx('tabular mt-2 truncate text-2xl font-bold tracking-tight', accent ? 'text-white' : 'text-ink-900')}>{value}</p>
          {hint && <p className={cx('mt-1 text-xs', accent ? 'text-ink-300' : 'text-ink-500')}>{hint}</p>}
        </div>
        <div className={cx('grid size-10 shrink-0 place-items-center rounded-xl', accent ? 'bg-brand-500 text-ink-950' : 'bg-brand-50 text-brand-600')}>
          <Icon className="size-5" />
        </div>
      </div>
    </Card>
  );
}

// ---------------------------------------------------------------- Modal & confirm
export function Modal({ open, title, onClose, children, footer, wide }: { open: boolean; title: string; onClose: () => void; children: ReactNode; footer?: ReactNode; wide?: boolean }) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-ink-950/50 p-0 backdrop-blur-[2px] sm:items-center sm:p-4" onMouseDown={onClose}>
      <div role="dialog" aria-modal="true" aria-label={title}
        className={cx('animate-fade-in flex max-h-[92vh] w-full flex-col rounded-t-2xl bg-white shadow-2xl sm:rounded-2xl', wide ? 'sm:max-w-2xl' : 'sm:max-w-lg')}
        onMouseDown={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between border-b border-ink-100 px-5 py-4">
          <h2 className="text-base font-semibold text-ink-900">{title}</h2>
          <button onClick={onClose} className="rounded-lg p-1 text-ink-400 hover:bg-ink-100 hover:text-ink-700" aria-label="Close"><X className="size-5" /></button>
        </div>
        <div className="overflow-y-auto px-5 py-5">{children}</div>
        {footer && <div className="flex justify-end gap-2 border-t border-ink-100 px-5 py-4">{footer}</div>}
      </div>
    </div>
  );
}

export function ConfirmDialog({ open, title, message, confirmLabel = 'Confirm', danger, loading, onConfirm, onClose }: {
  open: boolean; title: string; message: ReactNode; confirmLabel?: string; danger?: boolean; loading?: boolean; onConfirm: () => void; onClose: () => void;
}) {
  return (
    <Modal open={open} title={title} onClose={onClose}
      footer={<>
        <Button variant="secondary" onClick={onClose} disabled={loading}>Keep it</Button>
        <Button variant={danger ? 'danger' : 'primary'} onClick={onConfirm} loading={loading}>{confirmLabel}</Button>
      </>}>
      <div className="text-sm leading-relaxed text-ink-600">{message}</div>
    </Modal>
  );
}

// ---------------------------------------------------------------- Table & misc
export function Table({ head, children }: { head: string[]; children: ReactNode }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] text-left text-sm">
        <thead>
          <tr className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-500">
            {head.map((h) => <th key={h} className="whitespace-nowrap px-5 py-3 font-medium">{h}</th>)}
          </tr>
        </thead>
        <tbody className="divide-y divide-ink-100">{children}</tbody>
      </table>
    </div>
  );
}

export function Td({ children, className }: { children: ReactNode; className?: string }) {
  return <td className={cx('whitespace-nowrap px-5 py-3.5 text-ink-700', className)}>{children}</td>;
}

export function BatteryBar({ value, target }: { value: number; target?: number }) {
  return (
    <div className="relative h-2.5 w-full overflow-hidden rounded-full bg-ink-100">
      <div className="h-full rounded-full bg-gradient-to-r from-brand-500 to-brand-400 transition-all duration-700" style={{ width: `${Math.min(100, value)}%` }} />
      {target !== undefined && <div className="absolute top-0 h-full w-0.5 bg-ink-800" style={{ left: `${target}%` }} title={`Target ${target}%`} />}
    </div>
  );
}

export { cx };
