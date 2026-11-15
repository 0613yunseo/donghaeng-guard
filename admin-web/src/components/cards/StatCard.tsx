import type { ElementType } from "react";
import { TrendingUp, TrendingDown } from "lucide-react";
import { useApp } from "../../contexts/AppContext";
import { C } from "../../constants/colors";

interface StatCardProps {
  title: string;
  value: string;
  /** 전주 대비 같은 증감 문구. 실제 비교 데이터가 있을 때만 넘긴다. */
  delta?: string;
  up?: boolean;
  /** 증감 대신 보여줄 보조 설명 */
  note?: string;
  Icon: ElementType;
  accent: string;
}

export function StatCard({ title, value, delta, up = false, note, Icon, accent }: StatCardProps) {
  const { t } = useApp();
  return (
    <div className="rounded-2xl overflow-hidden p-5"
      style={{ background: t.card, border: `1px solid ${t.border}`, boxShadow: t.shadow }}>
      <div className="flex items-start justify-between mb-4">
        <p className="text-xs font-medium" style={{ color: t.muted }}>{title}</p>
        <div className="w-9 h-9 rounded-xl flex items-center justify-center" style={{ background: `${accent}1a` }}>
          <Icon size={18} style={{ color: accent }} />
        </div>
      </div>
      <p className="text-3xl font-bold mb-2" style={{ color: t.text }}>{value}</p>
      {delta !== undefined ? (
        <div className="flex items-center gap-1.5">
          {up
            ? <TrendingUp  size={12} style={{ color: C.danger }} />
            : <TrendingDown size={12} style={{ color: C.safe  }} />}
          <span className="text-[11px] font-semibold" style={{ color: up ? C.danger : C.safe }}>{delta}</span>
          <span className="text-[11px]" style={{ color: t.muted }}>전주 대비</span>
        </div>
      ) : note ? (
        <p className="text-[11px]" style={{ color: t.muted }}>{note}</p>
      ) : null}
    </div>
  );
}
