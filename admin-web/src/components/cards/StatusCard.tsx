import type { ElementType } from "react";
import { useApp } from "../../contexts/AppContext";

interface StatusCardProps {
  label: string;
  count: number;
  Icon: ElementType;
  accent: string;
  onClick?: () => void;
}

export function StatusCard({ label, count, Icon, accent, onClick }: StatusCardProps) {
  const { t } = useApp();
  return (
    <div
      className="rounded-2xl overflow-hidden p-5 cursor-pointer transition-opacity hover:opacity-80"
      style={{ background: t.card, border: `1px solid ${t.border}`, boxShadow: t.shadow }}
      onClick={onClick}>
      <div className="flex items-center justify-between mb-3">
        <p className="text-xs font-medium" style={{ color: t.muted }}>{label} 디바이스</p>
        <div className="w-9 h-9 rounded-xl flex items-center justify-center" style={{ background: `${accent}1a` }}>
          <Icon size={18} style={{ color: accent }} />
        </div>
      </div>
      <p className="text-3xl font-bold" style={{ color: t.text }}>{count}</p>
      <p className="text-xs mt-1" style={{ color: t.muted }}>클릭하여 필터</p>
    </div>
  );
}
