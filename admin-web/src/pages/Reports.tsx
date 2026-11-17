import { toast } from "sonner";
import { Download } from "lucide-react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  Legend, ResponsiveContainer, PieChart, Pie, Cell,
} from "recharts";
import { useApp } from "../contexts/AppContext";
import { C, gradeColor } from "../constants/colors";
import { Card, CardHead, PrimaryBtn, chartTooltipStyle } from "../components/ui";
import { GradeBadge } from "../components/badges/RiskBadge";
import { useStats, RECENT_TRIP_LIMIT } from "../hooks/useStats";
import type { RiskGrade } from "../types/admin";

export function Reports() {
  const { t } = useApp();
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
  const pct = (v: number) => (s.sampleEventCount ? `${Math.round((v / s.sampleEventCount) * 100)}%` : "—");

  // 화면에 표시된 집계값을 CSV로 내려받는다
  const downloadCsv = () => {
    const rows: (string | number)[][] = [
      ["항목", "값"],
      ["전체 센서 이벤트(전체 주행)", s.totalEvents],
      [`집계 대상 주행 수(최근 ${RECENT_TRIP_LIMIT}건 이내)`, s.sampleTripCount],
      ["집계 대상 이벤트 수", s.sampleEventCount],
      ["DANGER", s.byLevel.DANGER],
      ["WARNING", s.byLevel.WARNING],
      ["SAFE", s.byLevel.SAFE],
      ["등록된 위험 구간", s.zoneCount],
      [],
      ["주차 시작일", "SAFE", "WARNING", "DANGER"],
      ...s.weekly.map(w => [w.w.replace("~", ""), w.safe, w.warn, w.dan]),
    ];
    const csv = "﻿" + rows.map(r => r.join(",")).join("\r\n");
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    const a = document.createElement("a");
    a.href = url;
    a.download = "donghaeng-guard-report.csv";
    a.click();
    URL.revokeObjectURL(url);
    toast.success("리포트 CSV를 내려받았습니다.");
  };

  const zoneTotal = s.zoneCount;

  return (
    <div className="space-y-5">
      <p role={s.error ? "alert" : "note"} className="text-xs rounded-xl px-3 py-2"
        style={{ background: t.bgSub, color: s.error ? C.danger : t.muted, border: `1px solid ${t.border}` }}>
        {s.error
          ? s.error
          : s.loading
            ? "실제 데이터를 불러오는 중입니다."
            : `서버 데이터 기준입니다. 주간·비율은 최근 주행 ${s.sampleTripCount}건(최대 ${RECENT_TRIP_LIMIT}건)의 센서 이벤트를 집계한 값입니다.`}
      </p>

      {/* Header row */}
      <div className="flex items-center justify-between">
        <div>
          <p className="font-bold text-[15px]" style={{ color: t.text }}>종합 리포트</p>
          <p className="text-xs mt-0.5" style={{ color: t.muted }}>누적 주행 데이터 기반 위험 이벤트 통계</p>
        </div>
        <PrimaryBtn icon={Download} onClick={downloadCsv}>
          리포트 다운로드 (CSV)
        </PrimaryBtn>
      </div>

      {/* KPI summary */}
      <div className="grid grid-cols-4 gap-4">
        {[
          { l: "전체 센서 이벤트",   v: s.totalEvents.toLocaleString(), s: "전체 주행 합계" },
          { l: "DANGER 이벤트",      v: String(s.byLevel.DANGER),       s: hasEvents ? `집계 대상의 ${pct(s.byLevel.DANGER)}` : "집계 대상 없음" },
          { l: "등록된 위험 구간",   v: `${s.zoneCount}개`,             s: "서버에 저장된 구간" },
          { l: "HIGH 등급 구간",     v: `${s.zoneGrades.HIGH}개`,       s: zoneTotal ? `전체의 ${Math.round((s.zoneGrades.HIGH / zoneTotal) * 100)}%` : "등록된 구간 없음" },
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
          <CardHead title="주간별 위험 이벤트 누적 현황" sub="최근 5주 (월요일 시작)" />
          <div className="p-5">
            {!hasEvents ? empty(s.loading ? "불러오는 중입니다." : "집계할 이벤트가 없습니다.") : (
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={s.weekly}>
                  <CartesianGrid strokeDasharray="3 3" stroke={t.border} vertical={false} />
                  <XAxis dataKey="w" tick={{ fontSize: 11, fill: t.muted }} axisLine={false} tickLine={false} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: t.muted }} axisLine={false} tickLine={false} />
                  <Tooltip contentStyle={tt} />
                  <Legend wrapperStyle={{ fontSize: 11 }} />
                  <Bar dataKey="safe" name="SAFE"    fill={C.safe}   radius={[4,4,0,0]} stackId="a" />
                  <Bar dataKey="warn" name="WARNING" fill={C.warn}   radius={[0,0,0,0]} stackId="a" />
                  <Bar dataKey="dan"  name="DANGER"  fill={C.danger} radius={[4,4,0,0]} stackId="a" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </Card>

        <Card>
          <CardHead title="위험 단계별 비율" sub="집계 대상 이벤트 기준" />
          <div className="p-5">
            {!hasEvents ? empty(s.loading ? "불러오는 중입니다." : "집계할 이벤트가 없습니다.") : (
              <>
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
                      <span className="text-[11px] font-bold" style={{ color: t.text }}>{pct(d.v)}</span>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        </Card>
      </div>

      {/* 위험 구간 등급 분포 (지역 정보는 서버에 없어 등급 기준으로 표시) */}
      <Card>
        <CardHead title="위험 구간 등급 분포" sub="서버에 저장된 위험 구간 기준" />
        <div className="p-5 space-y-4">
          {zoneTotal === 0 && empty(s.loading ? "불러오는 중입니다." : "등록된 위험 구간이 없습니다.")}
          {zoneTotal > 0 && (["HIGH", "MEDIUM", "LOW"] as RiskGrade[]).map(g => {
            const n = s.zoneGrades[g];
            return (
              <div key={g} className="flex items-center gap-4">
                <span className="w-20 shrink-0"><GradeBadge g={g} /></span>
                <div className="flex-1 h-2 rounded-full overflow-hidden" style={{ background: t.border }}>
                  <div className="h-full rounded-full transition-all duration-500"
                    style={{ width: `${(n / zoneTotal) * 100}%`, background: gradeColor(g) }} />
                </div>
                <span className="text-[13px] font-bold w-12 text-right shrink-0" style={{ color: t.text }}>{n}개</span>
              </div>
            );
          })}
        </div>
      </Card>
    </div>
  );
}
