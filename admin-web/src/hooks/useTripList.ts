import { useMemo } from "react";
import { useApiList } from "./useApiList";
import type { TripListItemResponse } from "../types/tripApi";

const DEFAULT_USER_ID = "1";

export function useTripList() {
  const { items, loading, error } = useApiList<TripListItemResponse>(
    `/api/trips?userId=${DEFAULT_USER_ID}`,
    "주행 목록",
  );

  // 최신 주행을 먼저 표시하기 위해 tripId 내림차순 정렬
  const trips = useMemo(() => [...items].sort((a, b) => b.tripId - a.tripId), [items]);

  return { trips, loading, error };
}
