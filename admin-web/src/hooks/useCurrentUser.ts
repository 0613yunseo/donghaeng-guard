const CURRENT_USER_ID = "1";

/**
 * 현재 사용자. 지금은 로그인이 없어서 앱과 웹이 모두 사용자 1번의 데이터를 조회한다.
 * shortcut: 이름·이메일·권한을 내려주는 계정 API가 없어 ID만 알고 있다, 로그인 API가 생기면 이 훅만 교체한다.
 */
export function useCurrentUser() {
  return {
    userId: CURRENT_USER_ID,
    name: `사용자 #${CURRENT_USER_ID}`,
    email: null as string | null,
    role: null as string | null,
  };
}
