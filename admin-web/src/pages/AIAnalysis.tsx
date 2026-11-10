import { useApp } from "../contexts/AppContext";
import { BrainCircuit } from "lucide-react";
import { C } from "../constants/colors";
import { Card, CardHead } from "../components/ui";
import { RiskBadge } from "../components/badges/RiskBadge";
import { MOCK_AI_PREDICTION } from "../mocks/aiPredictionMock";

export function AIAnalysis() {
  const { t } = useApp();
  const result = MOCK_AI_PREDICTION;

  const riskLevel =
    result.riskGrade === "SAFE" ||
      result.riskGrade === "WARNING" ||
      result.riskGrade === "DANGER"
      ? result.riskGrade
      : null;

  const riskColor =
    riskLevel === "DANGER"
      ? C.danger
      : riskLevel === "WARNING"
        ? C.warn
        : C.blue;

  return (
    <div className="space-y-4">
      {/* Header */}
      <Card className="p-5">
        <div className="flex items-center gap-4">
          <div
            className="w-12 h-12 rounded-2xl flex items-center justify-center shrink-0"
            style={{ background: `${C.blue}18` }}
          >
            <BrainCircuit size={24} style={{ color: C.blue }} />
          </div>

          <div>
            <p
              className="font-bold text-sm"
              style={{ color: t.text }}
            >
              AI 위험 예측 분석
            </p>

            <p
              className="text-xs mt-1"
              style={{ color: t.muted }}
            >
              센서 측정 데이터를 기반으로 위험 등급과
              권장 조치를 제공하는 기능입니다.
            </p>

            <p
              className="text-xs mt-2 font-semibold"
              style={{ color: C.warn }}
            >
              현재 AI 연동 테스트용 예시 데이터입니다.
            </p>
          </div>
        </div>
      </Card>

      {/* Summary */}
      <div className="grid grid-cols-3 gap-4">
        <Card className="p-5">
          <p
            className="text-xs mb-3"
            style={{ color: t.muted }}
          >
            예시 분석 결과
          </p>

          <p
            className="text-3xl font-bold"
            style={{ color: t.text }}
          >
            1건
          </p>
        </Card>

        <Card className="p-5">
          <p
            className="text-xs mb-3"
            style={{ color: t.muted }}
          >
            AI 위험 등급
          </p>

          {riskLevel ? (
            <RiskBadge level={riskLevel} />
          ) : (
            <p
              className="text-lg font-bold"
              style={{ color: t.text }}
            >
              {result.riskGrade}
            </p>
          )}
        </Card>

        <Card className="p-5">
          <p
            className="text-xs mb-3"
            style={{ color: t.muted }}
          >
            AI 위험 점수
          </p>

          <p
            className="text-3xl font-bold"
            style={{ color: riskColor }}
          >
            {result.riskScore}
            <span
              className="text-sm ml-1"
              style={{ color: t.muted }}
            >
              점
            </span>
          </p>

          <div
            className="w-full h-2 rounded-full mt-3 overflow-hidden"
            style={{ background: t.border }}
          >
            <div
              className="h-full rounded-full"
              style={{
                width: `${Math.min(100, Math.max(0, result.riskScore))}%`,
                background: riskColor,
              }}
            />
          </div>
        </Card>
      </div>

      {/* AI prediction details */}
      <Card>
        <CardHead
          title="AI 분석 결과 상세"
          sub="AI 예측 등급, 위험 점수, 판정 사유 및 권장 조치"
        />

        <div className="p-5 space-y-5">
          <div className="grid grid-cols-2 gap-5">
            <div>
              <p
                className="text-xs mb-2"
                style={{ color: t.muted }}
              >
                위험 등급
              </p>

              {riskLevel ? (
                <RiskBadge level={riskLevel} />
              ) : (
                <p style={{ color: t.text }}>
                  {result.riskGrade}
                </p>
              )}
            </div>

            <div>
              <p
                className="text-xs mb-2"
                style={{ color: t.muted }}
              >
                위험 점수
              </p>

              <p
                className="text-lg font-bold"
                style={{ color: t.text }}
              >
                {result.riskScore}점
              </p>
            </div>
          </div>

          <div
            className="border-t pt-4"
            style={{ borderColor: t.border }}
          >
            <p
              className="text-xs font-semibold mb-2"
              style={{ color: t.muted }}
            >
              AI 판정 사유
            </p>

            <p
              className="text-sm leading-6"
              style={{ color: t.text }}
            >
              {result.reason}
            </p>
          </div>

          <div
            className="border-t pt-4"
            style={{ borderColor: t.border }}
          >
            <p
              className="text-xs font-semibold mb-2"
              style={{ color: t.muted }}
            >
              권장 조치
            </p>

            <p
              className="text-sm leading-6"
              style={{ color: t.text }}
            >
              {result.recommendedAction}
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}