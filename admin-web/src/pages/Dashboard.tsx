import { AlertTriangle, Zap, MapPin, Radio, ChevronRight } from "lucide-react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell,
} from "recharts";
import { useApp } from "../contexts/AppContext";
import { C, gradeColor } from "../constants/colors";
import { StatCard } from "../components/cards/StatCard";
import { RiskBadge, GradeBadge } from "../components/badges/RiskBadge";
import { Card, CardHead, chartTooltipStyle } from "../components/ui";
import { useStats, RECENT_TRIP_LIMIT } from "../hooks/useStats";

export function Dashboard() {
  const { t, onNav } = useApp();
  const s = useStats();
  const tt = chartTooltipStyle(t);

  const pie = [
    { name: "DANGER",  v: s.byLevel.DANGER,  c: C.danger },
    { name: "WARNING", v: s.byLevel.WARNING, c: C.warn   },
    { name: "SAFE",    v: s.byLevel.SAFE,    c: C.safe   },
  ];
  const hasEvents = s.sampleEventCount > 0;
  const empty = (msg: string) => (
    <p className="py-10 text-center text-xs" style={{ color: t.muted }}>{msg}</p>
  );

  return (
    <div className="space-y-5">
      <p role={s.error ? "alert" : "note"} className="text-xs rounded-xl px-3 py-2"
        style={{ background: t.bgSub, color: s.error ? C.danger : t.muted, border: `1px solid ${t.border}` }}>
        {s.error
          ? s.error
          : s.loading
            ? "실제 데이터를 불러오는 중입니다."
            : `서버 데이터 기준입니다. 추이·비율·최근 이벤트는 최근 주행 ${s.sampleTripCount}건(최대 ${RECENT_TRIP_LIMIT}건)의 센서 이벤트를 집계한 값입니다.`}
      </p>

      {/* KPI cards */}
      <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard title="전체 센서 이벤트" value={s.totalEvents.toLocaleString()} note="전체 주행 이벤트 합계" Icon={AlertTriangle} accent={C.danger} />
        <StatCard title="오늘 위험 이벤트" value={String(s.todayRiskEvents)} note="WARNING·DANGER, 최근 주행 기준" Icon={Zap} accent={C.warn} />
        <StatCard title="위험 구간" value={String(s.zoneCount)} note="등록된 위험 구간 수" Icon={MapPin} accent={C.blue} />
        <StatCard title="활성 디바이스" value={String(s.connectedDevices)} note={`전체 ${s.deviceCount}대 중 연결됨`} Icon={Radio} accent={C.mint} />
      </div>

      {/* Trend chart + Pie */}
      <div className="grid grid-cols-3 gap-4">
        <Card className="col-span-2">
          <CardHead
            title="위험 이벤트 추이"
            sub="최근 7일 WARNING / DANGER 발생 현황"
            right={
              <div className="flex items-center gap-3 text-[11px]" style={{ color: t.muted }}>
                <span className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-sm inline-block" style={{ background: C.warn }} /> WARNING
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-sm inline-block" style={{ background: C.danger }} /> DANGER
                </span>
              </div>
            }
          />
          <div className="p-5">
            {!hasEvents ? empty(s.loading ? "불러오는 중입니다." : "집계할 이벤트가 없습니다.") : (
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={s.trend} barCategoryGap="38%">
                  <CartesianGrid strokeDasharray="3 3" stroke={t.border} vertical={false} />
                  <XAxis dataKey="d" tick={{ fontSize: 11, fill: t.muted }} axisLine={false} tickLine={false} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: t.muted }} axisLine={false} tickLine={false} />
                  <Tooltip contentStyle={tt} />
                  <Bar dataKey="w"   name="WARNING" fill={C.warn}   radius={[5,5,0,0]} />
                  <Bar dataKey="dan" name="DANGER"  fill={C.danger} radius={[5,5,0,0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </Card>

        <Card>
          <CardHead title="위험 단계 비율" sub="최근 주행 이벤트 기준" />
          <div className="p-5">
            {!hasEvents ? empty(s.loading ? "불러오는 중입니다." : "집계할 이벤트가 없습니다.") : (
              <>
                <ResponsiveContainer width="100%" height={150}>
                  <PieChart>
                    <Pie data={pie} dataKey="v" cx="50%" cy="50%" innerRadius={44} outerRadius={68} paddingAngle={3}>
                      {pie.map((entry, i) => <Cell key={i} fill={entry.c} />)}
                    </Pie>
                    <Tooltip contentStyle={tt} />
                  </PieChart>
                </ResponsiveContainer>
                <div className="space-y-2.5 mt-3">
                  {pie.map(d => (
                    <div key={d.name} className="flex items-center justify-between text-xs">
                      <span className="flex items-center gap-2">
                        <span className="w-2.5 h-2.5 rounded-full" style={{ background: d.c }} />
                        <span className="font-medium" style={{ color: t.text }}>{d.name}</span>
                      </span>
                      <span style={{ color: t.muted }}>{d.v}건 · {Math.round((d.v / s.sampleEventCount) * 100)}%</span>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        </Card>
      </div>

      {/* Recent events + Zone top5 */}
      <div className="grid grid-cols-3 gap-4">
        <Card className="col-span-2">
          <CardHead
            title="최근 위험 이벤트"
            sub={`WARNING·DANGER ${s.recentRisk.length}건 표시`}
            right={
              <button onClick={() => onNav("events")}
                className="flex items-center gap-1 text-xs font-medium hover:opacity-70 transition-opacity"
                style={{ color: C.blue }}>
                전체 보기<ChevronRight size={13} />
              </button>
            }
          />
          {s.recentRisk.length === 0 ? empty(s.loading ? "불러오는 중입니다." : "최근 위험 이벤트가 없습니다.") : (
            <div className="overflow-x-auto">
              <table className="w-full text-xs">
                <thead>
                  <tr style={{ borderBottom: `1px solid ${t.border}`, background: t.bgSub }}>
                    {["이벤트 ID","발생시간","위험단계","센서","거리(mm)","디바이스"].map(h => (
                      <th key={h} className="px-5 py-3 text-left font-medium whitespace-nowrap" style={{ color: t.muted }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {s.recentRisk.map(e => (
                    <tr key={e.id}
                      onClick={() => onNav("event-detail", e.id, e.tripId)}
                      className="cursor-pointer transition-colors"
                      style={{ borderBottom: `1px solid ${t.border}`, background: e.rl === "DANGER" ? `${C.danger}07` : "transparent" }}
                      onMouseEnter={ev => (ev.currentTarget.style.background = e.rl === "DANGER" ? `${C.danger}12` : t.bgSub)}
                      onMouseLeave={ev => (ev.currentTarget.style.background = e.rl === "DANGER" ? `${C.danger}07` : "transparent")}>
                      <td className="px-5 py-3 font-mono font-semibold" style={{ color: C.blue }}>{e.id}</td>
                      <td className="px-5 py-3 whitespace-nowrap" style={{ color: t.muted }}>{e.at}</td>
                      <td className="px-5 py-3"><RiskBadge level={e.rl} /></td>
                      <td className="px-5 py-3 font-mono" style={{ color: t.textSub }}>{e.sensor}</td>
                      <td className="px-5 py-3 font-semibold" style={{ color: t.text }}>{e.mm ?? "—"}</td>
                      <td className="px-5 py-3 font-mono text-[11px]" style={{ color: t.muted }}>{e.dev}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        <Card>
          <CardHead
            title="위험 구간 TOP 5"
            right={
              <button onClick={() => onNav("zones")}
                className="flex items-center gap-1 text-xs font-medium hover:opacity-70 transition-opacity"
                style={{ color: C.blue }}>
                지도<ChevronRight size={13} />
              </button>
            }
          />
          <div className="p-5 space-y-4">
            {s.topZones.length === 0 && empty(s.loading ? "불러오는 중입니다." : "등록된 위험 구간이 없습니다.")}
            {s.topZones.map((z, i) => (
              <div key={z.id} className="flex items-center gap-3">
                <div className="w-6 h-6 rounded-full text-[11px] font-bold text-white flex items-center justify-center shrink-0"
                  style={{ background: i === 0 ? C.danger : i === 1 ? "#f06020" : i === 2 ? C.warn : t.muted }}>
                  {i + 1}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-[12px] font-semibold truncate" style={{ color: t.text }}>{z.id}</span>
                    <GradeBadge g={z.grade} />
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="flex-1 h-1.5 rounded-full overflow-hidden" style={{ background: t.border }}>
                      <div className="h-full rounded-full" style={{ width: `${Math.min(100, z.score)}%`, background: gradeColor(z.grade) }} />
                    </div>
                    <span className="text-[11px] font-bold shrink-0" style={{ color: t.text }}>{z.score}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
