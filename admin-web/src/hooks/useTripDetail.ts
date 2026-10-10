import { useEffect, useState } from "react";
import type { RiskEvent, Trip } from "../types/admin";
import type { TripApiResponse, TripDetailResponse } from "../types/tripApi";

const DEFAULT_USER_ID = "1";
const DEFAULT_DEVICE_ID = "DG-ESP32-001";

const getRiskTypeLabel = (sensorType?: string) => {
  switch (sensorType) {
    case "ULTRASONIC":
      return "근접/단차 감지";
    case "TILT":
      return "기울기 감지";
    case "CAMERA":
      return "객체 감지";
    default:
      return "—";
  }
};

export function useTripDetail() {
  const [data, setData] = useState<TripDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      try {
        const response = await fetch("http://localhost:8081/api/trips/1001", {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`주행 조회에 실패했습니다. (HTTP ${response.status})`);
        }

        const result: TripApiResponse = await response.json();

        if (!result.success || !result.data || !Array.isArray(result.data.events)) {
          throw new Error(result.message || "주행 응답을 확인할 수 없습니다.");
        }

        if (!controller.signal.aborted) {
          setData(result.data);
        }
      } catch (cause) {
        if (!controller.signal.aborted) {
          setError(
            cause instanceof Error
              ? cause.message
              : "주행 정보를 불러오지 못했습니다."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    void load();

    return () => controller.abort();
  }, []);

  const events: RiskEvent[] =
    data?.events.map((event) => ({
      id: String(event.eventId),
      at: event.detectedAt.replace("T", " "),
      rl: event.riskLevel,
      sensor: event.sensorType,

      // 현재 API에는 별도 위험유형 필드가 없으므로 sensorType 기준으로 프론트에서 파생 표시
      rt: getRiskTypeLabel(event.sensorType),

      // ULTRASONIC에만 distanceMm이 있고, TILT/CAMERA는 null이 정상
      mm: event.distanceMm ?? null,

      lat: event.latitude,
      lng: event.longitude,

      // 현재 GET /api/trips/{tripId} 응답에는 deviceId/userId가 없으므로 임시 fallback 표시
      // 추후 백엔드 응답에 deviceId, userId가 추가되면 해당 값으로 교체 예정
      dev: DEFAULT_DEVICE_ID,
      trip: String(data.tripId),
      user: DEFAULT_USER_ID,
    })) ?? [];

  const trips: Trip[] = data
    ? [
        {
          id: String(data.tripId),
          user: DEFAULT_USER_ID,
          dev: DEFAULT_DEVICE_ID,
          st: data.status,
          start: data.startedAt.replace("T", " "),
          end: data.endedAt?.replace("T", " ") ?? "—",
          cnt: data.eventCount,
          rl: data.highestRiskLevel,
        },
      ]
    : [];

  return { data, trips, events, loading, error };
}