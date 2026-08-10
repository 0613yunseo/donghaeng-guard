import { useState } from "react";
import { toast } from "sonner";
import { BrainCircuit, RefreshCw } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { C, gradeColor } from "../constants/colors";
import { Card, CardHead, OutlineBtn } from "../components/ui";
import { GradeBadge } from "../components/badges/RiskBadge";
import { AI_DATA } from "../mocks/adminMockData";
import type { RiskGrade } from "../types/admin";

export function AIAnalysis() {
  const { t } = useApp();
  const [analyzing, setAnalyzing] = useState(false);
  const [gradeF,    setGradeF]    = useState("");

  const filteredAI = AI_DATA.filter(z => !gradeF || z.grade === gradeF);

  const runAnalysis = () => {
    setAnalyzing(true);
    toast.loading("AI 분석을 실행 중입니다…", { id: "ai" });
    setTimeout(() => {
      setAnalyzing(false);
      toast.success("AI 분석이 완료되었습니다.", { id: "ai" });
    }, 2000);
  };

  return (
    <div className="space-y-4">
      {/* Header card */}
      <div className="grid grid-cols-4 gap-4">
        <Card className="col-span-3 p-5">
          <div className="flex items-center gap-4 mb-5">
            <div className="w-12 h-12 rounded-2xl flex items-center justify-center shrink-0"
              style={{ background: `${C.blue}18` }}>
              <BrainCircuit size={24} style={{ color: C.blue }} />
            </div>
            <div className="flex-1">
              <p className="font-bold text-sm" style={{ color: t.text }}>AI 위험 구간 자동 분석</p>
              <p className="text-xs mt-0.5" style={{ color: t.muted }}>
                분석 주기: 매일 06:00 자동 실행 · 마지막 업데이트 2024-06-30 06:00
              </p>
            </div>
            <OutlineBtn icon={RefreshCw} onClick={runAnalysis}>
              {analyzing ? "분석 중…" : "재분석 실행"}
            </OutlineBtn>
          </div>
          <div className="grid grid-cols-3 gap-4">
            {([
              ["HIGH",   2, C.danger] as [RiskGrade, number, string],
              ["MEDIUM", 2, C.warn  ] as [RiskGrade, number, string],
              ["LOW",    1, C.blue  ] as [RiskGrade, number, string],
            ]).map(([g, cnt, c]) => (
              <div key={g} className="p-4 rounded-xl" style={{ background: `${c}0f`, border: `1px solid ${c}20` }}>
                <p className="text-2xl font-bold" style={{ color: c }}>{cnt}개</p>
                <p className="text-xs font-medium mt-1" style={{ color: t.muted }}>{g} 위험 구간</p>
              </div>
            ))}
          </div>
        </Card>

        {/* Average score */}
        <Card className="p-5 flex flex-col items-center justify-center text-center">
          <p className="text-xs font-medium mb-2" style={{ color: t.muted }}>평균 위험 점수</p>
          <p className="text-5xl font-bold" style={{ color: t.text }}>64</p>
          <p className="text-xs mt-1 mb-4" style={{ color: t.muted }}>/ 100점</p>
          <div className="w-full h-2 rounded-full overflow-hidden" style={{ background: t.border }}>
            <div className="h-full rounded-full"
              style={{ width: "64%", background: `linear-gradient(90deg,${C.blue},${C.danger})` }} />
          </div>
          <div className="flex justify-between w-full text-[10px] mt-1" style={{ color: t.muted }}>
            <span>0</span><span>100</span>
          </div>
        </Card>
      </div>

      {/* Analysis results table */}
      <Card>
        <CardHead
          title="AI 분석 결과 상세"
          sub="위험 구간별 분석 사유 및 위험 점수"
          right={
            <select
              value={gradeF}
              onChange={e => setGradeF(e.target.value)}
              className="h-7 px-2 rounded-lg text-xs outline-none"
              style={{ background: t.input, border: `1px solid ${t.border}`, color: t.text }}>
              <option value="">등급 전체</option>
              {(["HIGH","MEDIUM","LOW"] as RiskGrade[]).map(g => <option key={g} value={g}>{g}</option>)}
            </select>
          }
        />
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr style={{ borderBottom: `1px solid ${t.border}`, background: t.bgSub }}>
                {["구간 ID","위험 점수","위험 등급","이벤트 수","AI 분석 사유","업데이트"].map(h => (
                  <th key={h} className="px-4 py-3 text-left font-medium" style={{ color: t.muted }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filteredAI.map(z => (
                <tr key={z.id}
                  className="transition-colors"
                  style={{ borderBottom: `1px solid ${t.border}` }}
                  onMouseEnter={e => (e.currentTarget.style.background = t.bgSub)}
                  onMouseLeave={e => (e.currentTarget.style.background = "transparent")}>
                  <td className="px-4 py-3 font-mono font-semibold" style={{ color: C.blue }}>{z.id}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <div className="w-20 h-1.5 rounded-full overflow-hidden" style={{ background: t.border }}>
                        <div className="h-full rounded-full" style={{ width: `${z.score}%`, background: gradeColor(z.grade) }} />
                      </div>
                      <span className="font-bold" style={{ color: t.text }}>{z.score}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3"><GradeBadge g={z.grade} /></td>
                  <td className="px-4 py-3 text-center font-bold" style={{ color: t.text }}>{z.cnt}</td>
                  <td className="px-4 py-3 max-w-xs" style={{ color: t.muted }}>{z.reason}</td>
                  <td className="px-4 py-3 whitespace-nowrap" style={{ color: t.muted }}>{z.updated}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
