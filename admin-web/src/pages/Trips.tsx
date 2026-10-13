import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Download } from "lucide-react";
import { useApp } from "../contexts/AppContext";
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
          <TripTable trips={filtered} />
        )}
      </Card>
    </div>
  );
}