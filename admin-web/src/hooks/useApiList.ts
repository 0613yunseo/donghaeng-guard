import { useEffect, useState } from "react";
import { errorText, withTimeout } from "../lib/fetchTimeout";

interface ApiEnvelope<T> {
  success: boolean;
  message?: string;
  data?: T[];
}

/** { success, data: T[] } 형식의 목록 API를 조회한다. refresh()로 재조회 가능. */
export function useApiList<T>(url: string, errorLabel: string) {
  const [items, setItems] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const response = await fetch(url, { signal: withTimeout(controller.signal) });
        if (!response.ok) throw new Error(`${errorLabel} 조회 실패 (HTTP ${response.status})`);

        const result: ApiEnvelope<T> = await response.json();
        if (!result.success || !Array.isArray(result.data)) {
          throw new Error(result.message || `${errorLabel} 응답을 확인할 수 없습니다.`);
        }
        if (!controller.signal.aborted) setItems(result.data);
      } catch (cause) {
        if (!controller.signal.aborted) {
          setError(errorText(cause, `${errorLabel}을(를) 불러오지 못했습니다.`));
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void load();
    return () => controller.abort();
  }, [url, errorLabel, tick]);

  return { items, loading, error, refresh: () => setTick(n => n + 1) };
}
