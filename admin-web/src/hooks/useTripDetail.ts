import { useEffect, useState } from "react";
import type { RiskEvent, Trip } from "../types/admin";
import type { TripApiResponse, TripDetailResponse } from "../types/tripApi";

export function useTripDetail() {
  const [data, setData] = useState<TripDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      try {
        const response = await fetch("http://localhost:8081/api/trips/1", {
          signal: controller.signal,
        });
        if (!response.ok) throw new Error(`주행 조회에 실패했습니다. (HTTP ${response.status})`);
        const result: TripApiResponse = await response.json();
        if (!result.success || !result.data || !Array.isArray(result.data.events)) {
          throw new Error(result.message || "주행 응답을 확인할 수 없습니다.");
        }
        if (!controller.signal.aborted) setData(result.data);
      } catch (cause) {
        if (!controller.signal.aborted) {
          setError(cause instanceof Error ? cause.message : "주행 정보를 불러오지 못했습니다.");
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    void load();
    return () => controller.abort();
  }, []);

  const events: RiskEvent[] = data?.events.map(event => ({
    id: String(event.eventId),
    at: event.detectedAt.replace("T", " "),
    rl: event.riskLevel,
    sensor: event.sensorType,
    rt: "—",
    mm: event.distanceMm ?? null,
    lat: event.latitude,
    lng: event.longitude,
    dev: "—",
    trip: String(data.tripId),
    user: "—",
  })) ?? [];

  const trips: Trip[] = data ? [{
    id: String(data.tripId), user: "—", dev: "—", st: data.status,
    start: data.startedAt.replace("T", " "),
    end: data.endedAt?.replace("T", " ") ?? "—",
    cnt: data.eventCount, rl: data.highestRiskLevel,
  }] : [];

  return { data, trips, events, loading, error };
}
