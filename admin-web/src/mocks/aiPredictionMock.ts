import type { AIPredictionResult } from "../types/admin";

export const MOCK_AI_PREDICTION: AIPredictionResult = {
    riskGrade: "WARNING",
    riskScore: 68,
    reason: "전방 장애물과의 거리가 가까워 충돌 위험이 있습니다.",
    recommendedAction: "속도를 줄이고 전방 장애물을 확인하세요.",
};