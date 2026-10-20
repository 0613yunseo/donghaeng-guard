import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Download } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";

import {
  Card,
  CardHead,
  FilterBar,
  SelectInput,
  PrimaryBtn,
  OutlineBtn,
} from "../components/ui";
import { TripTable } from "../components/tables/TripTable";
import type { RiskLevel, Trip, TripStatus } from "../types/admin";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import type { TripApiResponse, TripDetailResponse } from "../types/tripApi";

interface TripListItemResponse {
  tripId: number;
  startedAt: string;
  endedAt: string | null;
  status: TripStatus;
  eventCount: number;
  highestRiskLevel: RiskLevel | null;
  deviceId?: string | null;
  deviceName?: string | null;
}

interface TripListApiResponse {
  success: boolean;
  data: TripListItemResponse[] | null;
  message: string;
}

const DEFAULT_USER_ID = "1";

export function Trips() {
  const { t } = useApp();

  const [trips, setTrips] = useState<Trip[]>([]);
  const [selectedTripId, setSelectedTripId] = useState<string | null>(null);
  const [tripDetail, setTripDetail] = useState<TripDetailResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [st, setSt] = useState("");
  const [rl, setRl] = useState("");
  const [dev, setDev] = useState("");
  const [user, setUser] = useState("");

  const [applied, setApplied] = useState({
    st: "",
    rl: "",
    dev: "",
    user: "",
  });
  useEffect(() => {
    const controller = new AbortController();

    async function loadTrips() {
      try {
        const response = await fetch("http://localhost:8081/api/trips?userId=1", {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`주행 목록 조회에 실패했습니다. (HTTP ${response.status})`);
        }

        const result: TripListApiResponse = await response.json();

        if (!result.success || !Array.isArray(result.data)) {
          throw new Error(result.message || "주행 목록 응답을 확인할 수 없습니다.");
        }

        const mappedTrips: Trip[] = result.data.map((trip) => ({
          id: String(trip.tripId),
          user: DEFAULT_USER_ID,
          dev: trip.deviceName ?? trip.deviceId ?? "—",
          st: trip.status,
          start: trip.startedAt.replace("T", " "),
          end: trip.endedAt?.replace("T", " ") ?? "—",
          cnt: trip.eventCount,
          rl: trip.highestRiskLevel,
        }));

        if (!controller.signal.aborted) {
          setTrips(mappedTrips);
        }
      } catch (cause) {
        if (!controller.signal.aborted) {
          setError(
            cause instanceof Error
              ? cause.message
              : "주행 목록을 불러오지 못했습니다."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    void loadTrips();

    return () => controller.abort();
  }, []);
  useEffect(() => {
    if (!selectedTripId) {
      setTripDetail(null);
      setDetailError(null);
      setDetailLoading(false);
      return;
    }

    const controller = new AbortController();

    async function loadTripDetail() {
      setDetailLoading(true);
      setDetailError(null);
      setTripDetail(null);

      try {
        const response = await fetch(
          `http://localhost:8081/api/trips/${selectedTripId}`,
          { signal: controller.signal }
        );

        if (!response.ok) {
          throw new Error(
            `주행 상세 조회 실패 (HTTP ${response.status})`
          );
        }

        const result: TripApiResponse = await response.json();

        if (
          !result.success ||
          !result.data ||
          !Array.isArray(result.data.events)
        ) {
          throw new Error(
            result.message || "주행 상세 응답이 올바르지 않습니다."
          );
        }

        if (!controller.signal.aborted) {
          setTripDetail(result.data);
        }
      } catch (cause) {
        if (!controller.signal.aborted) {
          setDetailError(
            cause instanceof Error
              ? cause.message
              : "주행 데이터를 불러오지 못했습니다."
          );
        }
      } finally {
        if (!controller.signal.aborted) {
          setDetailLoading(false);
        }
      }
    }

    void loadTripDetail();

    return () => controller.abort();
  }, [selectedTripId]);
  const distanceData = (tripDetail?.events ?? [])
    .filter(
      (event) =>
        event.sensorType === "ULTRASONIC" &&
        event.distanceMm != null &&
        Number.isFinite(event.distanceMm)
    )
    .sort((a, b) => {
      const timeA = new Date(a.detectedAt).getTime();
      const timeB = new Date(b.detectedAt).getTime();

      return timeA - timeB || a.eventId - b.eventId;
    })
    .map((event) => ({
      eventId: event.eventId,
      time: event.detectedAt.replace("T", " ").slice(11, 19),
      distance: event.distanceMm!,
      riskLevel: event.riskLevel,
    }));
  const tiltData = (tripDetail?.events ?? [])
    .filter(
      (event) =>
        event.sensorType === "TILT" &&
        event.pitch != null &&
        Number.isFinite(event.pitch)
    )
    .sort((a, b) => {
      const timeA = new Date(a.detectedAt).getTime();
      const timeB = new Date(b.detectedAt).getTime();

      return timeA - timeB || a.eventId - b.eventId;
    })
    .map((event) => ({
      eventId: event.eventId,
      time: event.detectedAt.replace("T", " ").slice(11, 19),
      pitch: event.pitch!,
      slopeStatus: event.slopeStatus ?? "—",
      riskLevel: event.riskLevel,
    }));

  const filtered = trips.filter(
    (trip) =>
      (!applied.st || trip.st === applied.st) &&
      (!applied.rl || trip.rl === applied.rl) &&
      (!applied.dev || trip.dev === applied.dev) &&
      (!applied.user || trip.user === applied.user)
  );
  const deviceOptions = Array.from(
    new Set(trips.map((trip) => trip.dev).filter((device) => device && device !== "—"))
  );
  const userOptions = Array.from(
    new Set(trips.map((trip) => trip.user).filter(Boolean))
  );

  const apply = () => {
    setApplied({ st, rl, dev, user });
    toast.success("필터가 적용되었습니다.");
  };

  const reset = () => {
    setSt("");
    setRl("");
    setDev("");
    setUser("");
    setApplied({ st: "", rl: "", dev: "", user: "" });
    toast.info("필터가 초기화되었습니다.");
  };

  return (
    <div className="space-y-4">
      <FilterBar>
        <SelectInput value={st} onChange={setSt}>
          <option value="">주행 상태 전체</option>
          {(["STARTED", "ENDED"] as TripStatus[]).map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </SelectInput>

        <SelectInput value={rl} onChange={setRl}>
          <option value="">최고 위험단계 전체</option>
          {(["DANGER", "WARNING", "SAFE"] as RiskLevel[]).map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </SelectInput>

        <SelectInput value={dev} onChange={setDev}>
          <option value="">디바이스 전체</option>
          {deviceOptions.length === 0 ? (
            <option value="" disabled>
              디바이스 정보 미제공
            </option>
          ) : (
            deviceOptions.map((device) => (
              <option key={device} value={device}>
                {device}
              </option>
            ))
          )}
        </SelectInput>
        <SelectInput value={user} onChange={setUser}>
          <option value="">사용자 ID 전체</option>
          {userOptions.map((userId) => (
            <option key={userId} value={userId}>
              {userId}
            </option>
          ))}
        </SelectInput>

        <PrimaryBtn onClick={apply}>적용</PrimaryBtn>
        <OutlineBtn onClick={reset}>초기화</OutlineBtn>

        <span className="text-xs ml-auto" style={{ color: t.muted }}>
          {filtered.length}건 표시 중
        </span>
      </FilterBar>

      {selectedTripId && (
        <Card>
          <CardHead
            title={`주행 상세 분석 (주행 ID: ${selectedTripId})`}
            sub="선택한 주행의 센서 측정 데이터를 조회한 결과입니다."
            right={
              <OutlineBtn onClick={() => setSelectedTripId(null)}>
                닫기
              </OutlineBtn>
            }
          />

          <div className="p-5 space-y-4">
            {detailLoading ? (
              <p className="text-sm" style={{ color: t.muted }}>
                센서 측정 데이터를 불러오는 중입니다.
              </p>
            ) : detailError ? (
              <p role="alert" className="text-sm" style={{ color: C.danger }}>
                {detailError}
              </p>
            ) : tripDetail ? (
              <>
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <p className="text-xs" style={{ color: t.muted }}>
                      전체 센서 이벤트
                    </p>
                    <p className="text-xl font-bold" style={{ color: t.text }}>
                      {tripDetail.events.length}건
                    </p>
                  </div>

                  <div>
                    <p className="text-xs" style={{ color: t.muted }}>
                      초음파 거리 측정
                    </p>
                    <p className="text-xl font-bold" style={{ color: t.text }}>
                      {distanceData.length}건
                    </p>
                  </div>

                  <div>
                    <p className="text-xs" style={{ color: t.muted }}>
                      최고 위험 단계
                    </p>
                    <p className="text-xl font-bold" style={{ color: t.text }}>
                      {tripDetail.highestRiskLevel ?? "—"}
                    </p>
                  </div>
                </div>

                <div
                  className="border-t pt-4"
                  style={{ borderColor: t.border }}
                >
                  <p className="text-sm font-semibold mb-1" style={{ color: t.text }}>
                    초음파 센서 감지 거리 변화
                  </p>

                  <p className="text-xs mb-4" style={{ color: t.muted }}>
                    선택한 주행의 실제 센서 측정값 · 단위: mm
                  </p>

                  {distanceData.length === 0 ? (
                    <p className="py-12 text-center text-sm" style={{ color: t.muted }}>
                      표시할 초음파 거리 측정 데이터가 없습니다.
                    </p>
                  ) : (
                    <div className="w-full h-80">
                      <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={distanceData}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis
                            dataKey="time"
                            tick={{ fontSize: 11 }}
                            minTickGap={25}
                          />
                          <YAxis
                            tick={{ fontSize: 11 }}
                            label={{
                              value: "mm",
                              angle: -90,
                              position: "insideLeft",
                            }}
                          />
                          <Tooltip />
                          <Line
                            type="linear"
                            dataKey="distance"
                            name="감지 거리"
                            stroke={C.blue}
                            strokeWidth={2}
                            dot={false}
                            activeDot={{ r: 4 }}
                            isAnimationActive={false}
                          />
                        </LineChart>
                      </ResponsiveContainer>
                    </div>
                  )}
                </div>
                {/* Tilt sensor graph */}
                <div
                  className="border-t pt-4"
                  style={{ borderColor: t.border }}
                >
                  <p className="text-sm font-semibold mb-1" style={{ color: t.text }}>
                    기울기 센서 변화
                  </p>

                  <p className="text-xs mb-4" style={{ color: t.muted }}>
                    선택한 주행의 실제 기울기 측정값 · Pitch
                    {" · "}
                    {tiltData.length}건
                  </p>

                  {tiltData.length === 0 ? (
                    <p className="py-12 text-center text-sm" style={{ color: t.muted }}>
                      표시할 기울기 센서 측정 데이터가 없습니다.
                    </p>
                  ) : (
                    <div className="w-full h-80">
                      <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={tiltData}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis
                            dataKey="time"
                            tick={{ fontSize: 11 }}
                            minTickGap={25}
                          />
                          <YAxis
                            tick={{ fontSize: 11 }}
                          />
                          <Tooltip />
                          <Line
                            type="linear"
                            dataKey="pitch"
                            name="Pitch"
                            stroke={C.warn}
                            strokeWidth={2}
                            dot={false}
                            activeDot={{ r: 4 }}
                            isAnimationActive={false}
                          />
                        </LineChart>
                      </ResponsiveContainer>
                    </div>
                  )}
                </div>
              </>
            ) : null}
          </div>
        </Card>
      )}

      <Card>
        <CardHead
          title="주행 기록 목록"
          sub={`${filtered.length}건 / 전체 ${trips.length}건`}
          right={
            <OutlineBtn
              icon={Download}
              onClick={() => toast.success("CSV 파일이 다운로드되었습니다.")}
            >
              내보내기
            </OutlineBtn>
          }
        />

        {loading ? (
          <p role="status" className="p-5 text-sm" style={{ color: t.muted }}>
            주행 목록을 불러오는 중입니다.
          </p>
        ) : error ? (
          <p role="alert" className="p-5 text-sm">
            {error}
          </p>
        ) : (
          <TripTable
            trips={filtered}
            onRowClick={(tripId) => setSelectedTripId(tripId)}
          />
        )}
      </Card>


    </div>
  );
}