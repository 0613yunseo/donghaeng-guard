/** 백엔드가 응답하지 않을 때 무한 대기하지 않도록 취소 신호에 제한 시간을 더한다. */
export function withTimeout(signal: AbortSignal, ms = 10000): AbortSignal {
  return AbortSignal.any([signal, AbortSignal.timeout(ms)]);
}

/** 제한 시간 초과는 별도 문구로, 그 외에는 원본 메시지를 보여준다. */
export function errorText(cause: unknown, fallback: string): string {
  if (cause instanceof DOMException && cause.name === "TimeoutError") {
    return "백엔드 응답이 없습니다. 서버 실행 여부와 주소를 확인해 주세요.";
  }
  return cause instanceof Error ? cause.message : fallback;
}
