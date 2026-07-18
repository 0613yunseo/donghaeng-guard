import type { ReactNode, ElementType } from "react";
import { Filter } from "lucide-react";
import { useApp } from "../../contexts/AppContext";
import { C } from "../../constants/colors";
import type { ThemeTokens } from "../../types/admin";

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  const { t } = useApp();
  return (
    <div className={`rounded-2xl overflow-hidden ${className}`}
      style={{ background: t.card, border: `1px solid ${t.border}`, boxShadow: t.shadow }}>
      {children}
    </div>
  );
}

export function CardHead({ title, sub, right }: { title: string; sub?: string; right?: ReactNode }) {
  const { t } = useApp();
  return (
    <div className="flex items-center justify-between px-5 py-4"
      style={{ borderBottom: `1px solid ${t.border}` }}>
      <div>
        <p className="text-sm font-semibold" style={{ color: t.text }}>{title}</p>
        {sub && <p className="text-xs mt-0.5" style={{ color: t.muted }}>{sub}</p>}
      </div>
      {right}
    </div>
  );
}

export function PrimaryBtn({
  children, onClick, icon: Icon,
}: { children: ReactNode; onClick?: () => void; icon?: ElementType }) {
  return (
    <button onClick={onClick}
      className="h-8 px-3 rounded-xl text-xs font-semibold inline-flex items-center gap-1.5 transition-opacity hover:opacity-80 text-white"
      style={{ background: C.blue }}>
      {Icon && <Icon size={13} />}{children}
    </button>
  );
}

export function OutlineBtn({
  children, onClick, icon: Icon,
}: { children: ReactNode; onClick?: () => void; icon?: ElementType }) {
  const { t } = useApp();
  return (
    <button onClick={onClick}
      className="h-8 px-3 rounded-xl text-xs font-medium inline-flex items-center gap-1.5 transition-opacity hover:opacity-75"
      style={{ border: `1px solid ${t.border}`, color: t.muted, background: "transparent" }}>
      {Icon && <Icon size={13} />}{children}
    </button>
  );
}

export function Toggle({ on, onChange }: { on: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className="relative shrink-0 cursor-pointer" style={{ width: 40, height: 22 }}
      onClick={() => onChange(!on)}>
      <div className="absolute inset-0 rounded-full transition-colors duration-200"
        style={{ background: on ? C.blue : "#CBD5E1" }} />
      <div className="absolute top-0.5 rounded-full bg-white shadow transition-all duration-200"
        style={{ width: 18, height: 18, left: on ? 20 : 2 }} />
    </div>
  );
}

export function FilterBar({ children }: { children: ReactNode }) {
  const { t } = useApp();
  return (
    <div className="rounded-2xl px-4 py-3 flex flex-wrap items-center gap-2.5"
      style={{ background: t.card, border: `1px solid ${t.border}`, boxShadow: t.shadow }}>
      <span className="text-xs font-medium flex items-center gap-1.5" style={{ color: t.muted }}>
        <Filter size={13} />필터
      </span>
      {children}
    </div>
  );
}

export function SelectInput({
  value, onChange, children,
}: { value: string; onChange: (v: string) => void; children: ReactNode }) {
  const { t } = useApp();
  return (
    <select value={value} onChange={e => onChange(e.target.value)}
      className="h-8 px-2.5 rounded-xl text-xs outline-none"
      style={{ background: t.input, border: `1px solid ${t.border}`, color: t.text }}>
      {children}
    </select>
  );
}

export function chartTooltipStyle(t: ThemeTokens) {
  return {
    background: t.card,
    border: `1px solid ${t.border}`,
    borderRadius: 12,
    fontSize: 11,
    color: t.text,
    boxShadow: t.shadowLg,
  };
}
