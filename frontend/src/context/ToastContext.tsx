import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { CheckCircle2, AlertTriangle, X } from 'lucide-react';

type Tone = 'success' | 'error';
interface Toast { id: number; tone: Tone; message: string }

const ToastContext = createContext<{ notify: (message: string, tone?: Tone) => void } | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = (id: number) => setToasts((all) => all.filter((t) => t.id !== id));

  const notify = useCallback((message: string, tone: Tone = 'success') => {
    const id = Date.now() + Math.random();
    setToasts((all) => [...all, { id, tone, message }]);
    window.setTimeout(() => dismiss(id), 4500);
  }, []);

  return (
    <ToastContext.Provider value={{ notify }}>
      {children}
      <div className="fixed bottom-4 right-4 z-[60] flex w-[min(92vw,380px)] flex-col gap-2" aria-live="polite">
        {toasts.map((toast) => (
          <div key={toast.id}
            className={`animate-fade-in flex items-start gap-3 rounded-xl border bg-white p-3.5 shadow-lg ${toast.tone === 'success' ? 'border-brand-200' : 'border-red-200'}`}>
            {toast.tone === 'success'
              ? <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-brand-600" />
              : <AlertTriangle className="mt-0.5 size-5 shrink-0 text-red-600" />}
            <p className="flex-1 text-sm text-ink-800">{toast.message}</p>
            <button onClick={() => dismiss(toast.id)} className="text-ink-400 hover:text-ink-700" aria-label="Dismiss">
              <X className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used inside ToastProvider');
  return context.notify;
}
