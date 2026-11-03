import { useEffect, useState } from "react";
import type { TripListItemResponse, TripListApiResponse } from "../types/tripApi";

const DEFAULT_USER_ID = "1";

export function useTripList() {
    const [trips, setTrips] = useState<TripListItemResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const controller = new AbortController();

        async function load() {
            try {
                const response = await fetch(`/api/trips?userId=${DEFAULT_USER_ID}`, {
                    signal: controller.signal,
                });

                if (!response.ok) {
                    throw new Error(`주행 목록 조회에 실패했습니다. (HTTP ${response.status})`);
                }

                const result: TripListApiResponse = await response.json();

                if (!result.success || !Array.isArray(result.data)) {
                    throw new Error(result.message || "주행 목록 응답을 확인할 수 없습니다.");
                }

                if (!controller.signal.aborted) {
                    // 최신 주행을 먼저 표시하기 위해 tripId 내림차순 정렬
                    const sorted = [...result.data].sort((a, b) => b.tripId - a.tripId);
                    setTrips(sorted);
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

        void load();

        return () => controller.abort();
    }, []);

    return { trips, loading, error };
}
