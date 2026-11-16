import { useEffect, useMemo, useState } from "react";
import { useApiList } from "./useApiList";
import { useTripList } from "./useTripList";
import { errorText, withTimeout } from "../lib/fetchTimeout";
import type { SensorEventResponse, TripApiResponse } from "../types/tripApi";
import type { DeviceStatus, RiskGrade, RiskLevel } from "../types/admin";

/** 이벤트 집계에 사용할 최근 주행 수 (주행마다 상세 조회가 필요해 개수를 제한한다) */
export const RECENT_TRIP_LIMIT = 10;

interface ZoneApiItem {
  zoneId: number;
  riskScore: number | null;
  riskGrade: RiskGrade | null;
  zoneEventCount: number | null;
  reason: string | null;
}
interface DeviceApiItem { deviceId: string; deviceStatus: DeviceStatus }

type StatEvent = SensorEventResponse & { tripId: number };

const pad = (n: number) => String(n).padStart(2, "0");
const dateKey = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const emptyCounts = (): Record<RiskLevel, number> => ({ SAFE: 0, WARNING: 0, DANGER: 0 });

/** 대시보드·리포트가 공유하는 실데이터 집계. 백엔드에 집계 API가 없어 기존 API 응답을 클라이언트에서 계산한다. */
export function useStats() {
  const { trips, loading: tripsLoading, error: tripsError } = useTripList();
  const { items: zones, loading: zonesLoading, error: zonesError } =
    useApiList<ZoneApiItem>("/api/risk-zones", "위험 구간");
  const { items: devices, loading: devicesLoading, error: devicesError } =
    useApiList<DeviceApiItem>("/api/devices?userId=1", "디바이스 목록");

  const recentIds = useMemo(
    () => trips.slice(0, RECENT_TRIP_LIMIT).map(t => t.tripId),
    [trips],
  );
  const recentKey = recentIds.join(",");

  const [events, setEvents] = useState<StatEvent[]>([]);
  const [eventsLoading, setEventsLoading] = useState(true);
  const [eventsError, setEventsError] = useState<string | null>(null);

  useEffect(() => {
    if (tripsLoading) return;
    const controller = new AbortController();

    async function load() {
      setEventsError(null);
      if (recentIds.length === 0) {
        setEvents([]);
        setEventsLoading(false);
        return;
      }
      setEventsLoading(true);
      try {
        const results = await Promise.allSettled(
          recentIds.map(async id => {
            const res = await fetch(`/api/trips/${id}`, { signal: withTimeout(controller.signal) });
            if (!res.ok) throw new Error(`주행 ${id} 조회 실패 (HTTP ${res.status})`);
            const body: TripApiResponse = await res.json();
            if (!body.success || !body.data || !Array.isArray(body.data.events)) {
              throw new Error(body.message || `주행 ${id} 응답을 확인할 수 없습니다.`);
            }
            return body.data.events.map(e => ({ ...e, tripId: id }));
          }),
        );
        if (controller.signal.aborted) return;

        const ok = results.flatMap(r => (r.status === "fulfilled" ? r.value : []));
        const failed = results.find(r => r.status === "rejected");
        setEvents(ok);
        if (failed && ok.length === 0) {
          setEventsError(errorText((failed as PromiseRejectedResult).reason, "이벤트를 불러오지 못했습니다."));
        }
      } finally {
        if (!controller.signal.aborted) setEventsLoading(false);
      }
    }

    void load();
    return () => controller.abort();
    // recentKey가 바뀔 때만 다시 조회한다
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tripsLoading, recentKey]);

  const stats = useMemo(() => {
    const today = dateKey(new Date());

    // 전체 센서 이벤트 수는 주행 목록의 eventCount 합계 (전체 주행 기준)
    const totalEvents = trips.reduce((sum, t) => sum + (t.eventCount ?? 0), 0);

    // 아래 값은 최근 주행(RECENT_TRIP_LIMIT개)의 이벤트 기준
    const byLevel = emptyCounts();
    const byDay = new Map<string, Record<RiskLevel, number>>();
    for (const e of events) {
      byLevel[e.riskLevel] += 1;
      const k = e.detectedAt.slice(0, 10);
      const c = byDay.get(k) ?? emptyCounts();
      c[e.riskLevel] += 1;
      byDay.set(k, c);
    }

    const todayRisk = byDay.get(today);
    const todayRiskEvents = todayRisk ? todayRisk.WARNING + todayRisk.DANGER : 0;

    // 최근 7일 추이
    const trend = Array.from({ length: 7 }, (_, i) => {
      const d = new Date();
      d.setDate(d.getDate() - (6 - i));
      const k = dateKey(d);
      const c = byDay.get(k) ?? emptyCounts();
      return { d: `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`, w: c.WARNING, dan: c.DANGER };
    });

    // 최근 5주 (월요일 시작) 누적
    const weekStart = (d: Date) => {
      const x = new Date(d);
      x.setDate(x.getDate() - ((x.getDay() + 6) % 7));
      return dateKey(x);
    };
    const weeks = new Map<string, Record<RiskLevel, number>>();
    for (const [k, c] of byDay) {
      const ws = weekStart(new Date(`${k}T00:00:00`));
      const w = weeks.get(ws) ?? emptyCounts();
      w.SAFE += c.SAFE; w.WARNING += c.WARNING; w.DANGER += c.DANGER;
      weeks.set(ws, w);
    }
    const weekly = Array.from({ length: 5 }, (_, i) => {
      const d = new Date();
      d.setDate(d.getDate() - 7 * (4 - i));
      const ws = weekStart(d);
      const c = weeks.get(ws) ?? emptyCounts();
      return { w: `${ws.slice(5)}~`, safe: c.SAFE, warn: c.WARNING, dan: c.DANGER };
    });

    const recentRisk = events
      .filter(e => e.riskLevel !== "SAFE")
      .sort((a, b) => b.detectedAt.localeCompare(a.detectedAt) || b.eventId - a.eventId)
      .slice(0, 6)
      .map(e => ({
        id: String(e.eventId),
        tripId: String(e.tripId),
        at: e.detectedAt.replace("T", " "),
        rl: e.riskLevel,
        sensor: e.sensorType,
        mm: e.distanceMm ?? null,
        dev: e.deviceName ?? e.deviceId ?? "—",
      }));

    const topZones = [...zones]
      .sort((a, b) => (b.riskScore ?? -1) - (a.riskScore ?? -1))
      .slice(0, 5)
      .map(z => ({ id: `ZONE-${z.zoneId}`, grade: z.riskGrade ?? ("LOW" as RiskGrade), score: z.riskScore ?? 0 }));

    const zoneGrades: Record<RiskGrade, number> = { HIGH: 0, MEDIUM: 0, LOW: 0 };
    for (const z of zones) if (z.riskGrade) zoneGrades[z.riskGrade] += 1;

    return {
      totalEvents,
      todayRiskEvents,
      zoneCount: zones.length,
      connectedDevices: devices.filter(d => d.deviceStatus === "CONNECTED").length,
      deviceCount: devices.length,
      byLevel,
      sampleEventCount: events.length,
      sampleTripCount: recentIds.length,
      trend,
      weekly,
      recentRisk,
      topZones,
      zoneGrades,
    };
  }, [trips, events, zones, devices, recentIds]);

  return {
    ...stats,
    loading: tripsLoading || zonesLoading || devicesLoading || eventsLoading,
    error: tripsError ?? zonesError ?? devicesError ?? eventsError,
  };
}
