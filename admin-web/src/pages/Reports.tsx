import { toast } from "sonner";
import { Download } from "lucide-react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  Legend, ResponsiveContainer, PieChart, Pie, Cell,
} from "recharts";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead, PrimaryBtn, chartTooltipStyle } from "../components/ui";
import { GradeBadge } from "../components/badges/RiskBadge";
import { weeklyData, regionData } from "../mocks/adminMockData";
import type { RiskGrade } from "../types/admin";

export function Reports() {
  const { t } = useApp();
  const pie = [
    { name: "DANGER",  v: 89,  c: C.danger },
    { name: "WARNING", v: 234, c: C.warn   },
    { name: "SAFE",    v: 925, c: C.safe   },
  ];
  const tt = chartTooltipStyle(t);

  return (
    <div className="space-y-5">
      {/* Header row */}
      <div className="flex items-center justify-between">
        <div>
          <p className="font-bold text-[15px]" style={{ color: t.text }}>2024년 6월 종합 리포트</p>
          <p className="text-xs mt-0.5" style={{ color: t.muted }}>공공기관 제출용 위험 이벤트 통계 보고서</p>
        </div>
        <div className="flex items-center gap-2">
          <select className="h-8 px-2.5 rounded-xl text-xs outline-none"
            style={{ background: t.input, border: `1px solid ${t.border}`, color: t.text }}>
            <option>2024년 6월</option>
            <option>2024년 5월</option>
          </select>
          <PrimaryBtn icon={Download} onClick={() => toast.success("리포트 PDF가 다운로드되었습니다.")}>
            리포트 다운로드
          </PrimaryBtn>
        </div>
      </div>

      {/* KPI summary */}
      <div className="grid grid-cols-4 gap-4">
        {[
          { l: "총 이벤트",        v: "1,248", s: "전월 +12.4%"   },
          { l: "DANGER 이벤트",    v: "89",    s: "전체의 7.1%"   },
          { l: "분석된 위험 구간", v: "82개",  s: "신규 5개 포함" },
          { l: "조치 완료 구간",   v: "34개",  s: "전체 41.5%"    },
        ].map(item => (
          <Card key={item.l} className="p-5">
            <p className="text-xs font-medium mb-2" style={{ color: t.muted }}>{item.l}</p>
            <p className="text-2xl font-bold" style={{ color: t.text }}>{item.v}</p>
            <p className="text-xs mt-1" style={{ color: t.muted }}>{item.s}</p>
          </Card>
        ))}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-3 gap-4">
        <Card className="col-span-2">
          <CardHead title="주간별 위험 이벤트 누적 현황" sub="최근 5주" />
          <div className="p-5">
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={weeklyData}>
                <CartesianGrid strokeDasharray="3 3" stroke={t.border} vertical={false} />
                <XAxis dataKey="w" tick={{ fontSize: 11, fill: t.muted }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: t.muted }} axisLine={false} tickLine={false} />
                <Tooltip contentStyle={tt} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Bar dataKey="safe" name="SAFE"    fill={C.safe}   radius={[4,4,0,0]} stackId="a" />
                <Bar dataKey="warn" name="WARNING" fill={C.warn}   radius={[0,0,0,0]} stackId="a" />
                <Bar dataKey="dan"  name="DANGER"  fill={C.danger} radius={[4,4,0,0]} stackId="a" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card>
          <CardHead title="위험 단계별 비율" sub="6월 전체 기준" />
          <div className="p-5">
            <ResponsiveContainer width="100%" height={148}>
              <PieChart>
                <Pie data={pie} dataKey="v" cx="50%" cy="50%" innerRadius={42} outerRadius={64} paddingAngle={3}>
                  {pie.map((entry, i) => <Cell key={i} fill={entry.c} />)}
                </Pie>
                <Tooltip contentStyle={tt} />
              </PieChart>
            </ResponsiveContainer>
            <div className="space-y-2.5 mt-3">
              {pie.map(d => (
                <div key={d.name} className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: d.c }} />
                  <span className="flex-1 text-[12px] font-medium" style={{ color: t.text }}>{d.name}</span>
                  <span className="text-[11px]" style={{ color: t.muted }}>{d.v}건</span>
                  <span className="text-[11px] font-bold" style={{ color: t.text }}>{Math.round(d.v / 12.48)}%</span>
                </div>
              ))}
            </div>
          </div>
        </Card>
      </div>

      {/* Regional distribution */}
      <Card>
        <CardHead title="지역별 위험도 현황" sub="서울시 구별 위험 점수 분포" />
        <div className="p-5 space-y-4">
          {regionData.map(r => {
            const c = r.s >= 70 ? C.danger : r.s >= 50 ? C.warn : C.blue;
            const g: RiskGrade = r.s >= 70 ? "HIGH" : r.s >= 50 ? "MEDIUM" : "LOW";
            return (
              <div key={r.r} className="flex items-center gap-4">
                <span className="text-[13px] font-semibold w-20 shrink-0" style={{ color: t.text }}>{r.r}</span>
                <div className="flex-1 h-2 rounded-full overflow-hidden" style={{ background: t.border }}>
                  <div className="h-full rounded-full transition-all duration-500" style={{ width: `${r.s}%`, background: c }} />
                </div>
                <span className="text-[13px] font-bold w-8 text-right shrink-0" style={{ color: t.text }}>{r.s}</span>
                <GradeBadge g={g} />
              </div>
            );
          })}
        </div>
      </Card>
    </div>
  );
}
