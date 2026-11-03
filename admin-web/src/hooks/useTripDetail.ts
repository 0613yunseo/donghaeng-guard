import { useEffect, useRef, useState } from "react";
import type { RiskEvent, Trip } from "../types/admin";
import type { TripApiResponse, TripDetailResponse } from "../types/tripApi";

const DEFAULT_USER_ID = "1";

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

/**
 * 지정한 tripId의 상세 센서 데이터를 불러오는 훅.
 * tripId가 null이면 요청하지 않고 초기 상태를 반환한다.
 *
 * stale data 방지: tripId가 변경되면 AbortController로 이전 요청을
 * 취소하고, 응답을 수신했을 때 ref로 보관한 요청 ID와 비교하여
 * 구버전 응답이 상태를 덮어쓰지 않도록 한다.
 */
export function useTripDetail(tripId: string | null) {
  const [data, setData] = useState<TripDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // tripId가 변경될 때마다 요청 시퀀스 번호를 증가시켜 stale 응답을 무시한다
  const requestSeqRef = useRef(0);

  useEffect(() => {
    if (!tripId) {
      setData(null);
      setError(null);
      setLoading(false);
      return;
    }

    const controller = new AbortController();
    const seq = ++requestSeqRef.current;

    // 새 tripId 요청 즉시 이전 데이터 초기화 및 로딩 상태 표시
    setData(null);
    setError(null);
    setLoading(true);

    async function load() {
      try {
        const response = await fetch(`/api/trips/${tripId}`, {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`주행 조회에 실패했습니다. (HTTP ${response.status})`);
        }

        const result: TripApiResponse = await response.json();

        if (!result.success || !result.data || !Array.isArray(result.data.events)) {
          throw new Error(result.message || "주행 응답을 확인할 수 없습니다.");
        }

        // 현재 요청이 최신인지 확인 (stale response 방지)
        if (!controller.signal.aborted && seq === requestSeqRef.current) {
          setData(result.data);
        }
      } catch (cause) {
        if (!controller.signal.aborted && seq === requestSeqRef.current) {
          setError(
            cause instanceof Error
              ? cause.message
              : "주행 정보를 불러오지 못했습니다."
          );
        }
      } finally {
        if (!controller.signal.aborted && seq === requestSeqRef.current) {
          setLoading(false);
        }
      }
    }

    void load();

    return () => controller.abort();
  }, [tripId]);

  const tripDevice =
    data?.events?.[0]?.deviceName ?? data?.events?.[0]?.deviceId ?? "—";

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

      // 백엔드 응답의 deviceName/deviceId 기준으로 표시
      dev: event.deviceName ?? event.deviceId ?? "—",

      trip: String(data.tripId),

      // 현재 trip detail 응답에는 userId가 없으므로 임시 표시
      user: DEFAULT_USER_ID,

      sensorId: event.sensorId ?? null,
      pitch: event.pitch ?? null,
      slopeStatus: event.slopeStatus ?? null,

      detectedObject: event.detectedObject ?? null,
      confidence: event.confidence ?? null,
      stdDev: event.stdDev ?? null,
      imageUrl: event.imageUrl ?? null,
    })) ?? [];

  const trips: Trip[] = data
    ? [
      {
        id: String(data.tripId),
        user: DEFAULT_USER_ID,
        dev: tripDevice,
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