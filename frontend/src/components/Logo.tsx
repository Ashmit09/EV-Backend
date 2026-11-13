import { Zap } from 'lucide-react';

export function Logo({ dark = false }: { dark?: boolean }) {
  return (
    <span className="flex items-center gap-2">
      <span className="grid size-8 place-items-center rounded-lg bg-brand-500 text-ink-950 shadow-sm">
        <Zap className="size-4.5 fill-current" />
      </span>
      <span className={`text-lg font-extrabold tracking-tight ${dark ? 'text-white' : 'text-ink-900'}`}>
        Volt<span className="text-brand-500">X</span>
      </span>
    </span>
  );
}
