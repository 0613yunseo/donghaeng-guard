import { C, gradeColor, riskBg, riskFg } from "../../constants/colors";
import type { RiskLevel, RiskGrade, DeviceStatus, TripStatus, ZoneStatus } from "../../types/admin";

export function RiskBadge({ level }: { level: RiskLevel }) {
  const dot = level === "DANGER" ? C.danger : level === "WARNING" ? C.warn : C.safe;
  return (
    <span style={{ background: riskBg(level), color: riskFg(level) }}
      className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold">
      <span className="w-1.5 h-1.5 rounded-full" style={{ background: dot }} />
      {level}
    </span>
  );
}

export function GradeBadge({ g }: { g: RiskGrade }) {
  void gradeColor;
  const bg = g === "HIGH" ? `${C.danger}15` : g === "MEDIUM" ? `${C.warn}13` : `${C.blue}14`;
  const fg = g === "HIGH" ? "#c0202d" : g === "MEDIUM" ? "#8a5d00" : "#1a5fa8";
  return (
    <span style={{ background: bg, color: fg }}
      className="inline-flex px-2.5 py-0.5 rounded-full text-[11px] font-semibold">
      {g}
    </span>
  );
}

const ZS_STYLE: Record<ZoneStatus, { bg: string; fg: string }> = {
  "확인 전":   { bg: "rgba(100,116,139,0.12)", fg: "#475569" },
  "검토 중":   { bg: `${C.warn}13`,             fg: "#8a5d00" },
  "조치 완료": { bg: `${C.mint}12`,             fg: "#067a57" },
};

export function ZsBadge({ s }: { s: ZoneStatus }) {
  const { bg, fg } = ZS_STYLE[s];
  return (
    <span style={{ background: bg, color: fg }}
      className="inline-flex px-2.5 py-0.5 rounded-full text-[11px] font-semibold">
      {s}
    </span>
  );
}

const DS_STYLE: Record<DeviceStatus, { bg: string; fg: string; dot: string }> = {
  CONNECTED:    { bg: `${C.mint}12`,            fg: "#067a57", dot: C.mint    },
  DISCONNECTED: { bg: "rgba(100,116,139,0.12)", fg: "#475569", dot: "#94a3b8" },
  ERROR:        { bg: `${C.danger}15`,          fg: "#c0202d", dot: C.danger  },
};

export function DevBadge({ s }: { s: DeviceStatus }) {
  const { bg, fg, dot } = DS_STYLE[s];
  return (
    <span style={{ background: bg, color: fg }}
      className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold">
      <span className="w-1.5 h-1.5 rounded-full" style={{ background: dot }} />{s}
    </span>
  );
}

export function TsBadge({ s }: { s: TripStatus }) {
  const on = s === "STARTED";
  return (
    <span style={{
      background: on ? `${C.mint}12` : "rgba(100,116,139,0.12)",
      color: on ? "#067a57" : "#475569",
    }} className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold">
      <span className="w-1.5 h-1.5 rounded-full" style={{ background: on ? C.mint : "#94a3b8" }} />{s}
    </span>
  );
}
