import { useApp } from "../../contexts/AppContext";
import { C } from "../../constants/colors";
import { RiskBadge, TsBadge } from "../badges/RiskBadge";
import type { Trip } from "../../types/admin";

interface Props {
  trips: Trip[];
}

export function TripTable({ trips }: Props) {
  const { t } = useApp();

  if (trips.length === 0) {
    return (
      <div className="py-16 text-center text-sm" style={{ color: t.muted }}>
        조건에 맞는 주행 기록이 없습니다.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead>
          <tr style={{ borderBottom: `1px solid ${t.border}`, background: t.bgSub }}>
            {["주행 ID","사용자 ID","디바이스 ID","상태","시작 시각","종료 시각","이벤트 수","최고 위험단계"].map(h => (
              <th key={h} className="px-4 py-3 text-left font-medium whitespace-nowrap" style={{ color: t.muted }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {trips.map(trip => (
            <tr key={trip.id}
              className="cursor-pointer transition-colors"
              style={{ borderBottom: `1px solid ${t.border}` }}
              onMouseEnter={e => (e.currentTarget.style.background = t.bgSub)}
              onMouseLeave={e => (e.currentTarget.style.background = "transparent")}>
              <td className="px-4 py-3 font-mono font-semibold" style={{ color: C.blue }}>{trip.id}</td>
              <td className="px-4 py-3" style={{ color: t.muted }}>{trip.user}</td>
              <td className="px-4 py-3 font-mono text-[11px]" style={{ color: t.muted }}>{trip.dev}</td>
              <td className="px-4 py-3"><TsBadge s={trip.st} /></td>
              <td className="px-4 py-3 whitespace-nowrap" style={{ color: t.muted }}>{trip.start}</td>
              <td className="px-4 py-3 whitespace-nowrap" style={{ color: t.muted }}>{trip.end}</td>
              <td className="px-4 py-3 text-center">
                <span className="font-bold" style={{ color: trip.cnt >= 5 ? C.danger : trip.cnt >= 3 ? C.warn : t.text }}>
                  {trip.cnt}
                </span>
              </td>
              <td className="px-4 py-3"><RiskBadge level={trip.rl} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
