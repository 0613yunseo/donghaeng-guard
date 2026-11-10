export type Page =
  | "dashboard"
  | "events"
  | "event-detail"
  | "zones"
  | "trips"
  | "devices"
  | "ai"
  | "reports"
  | "settings"
  | "mypage";

export type RiskLevel = "SAFE" | "WARNING" | "DANGER";
export type RiskGrade = "LOW" | "MEDIUM" | "HIGH";
export type TripStatus = "STARTED" | "ENDED";
export type DeviceStatus = "CONNECTED" | "DISCONNECTED" | "ERROR";
export type ZoneStatus = "확인 전" | "검토 중" | "조치 완료";

export interface RiskEvent {
  id: string;
  at: string;
  rl: RiskLevel;
  sensor: string;
  rt: string;
  mm: number | null;
  lat: number | null;
  lng: number | null;
  dev: string;
  trip: string;
  user: string;

  // 센서별 상세 정보
  sensorId?: number | null;
  pitch?: number | null;
  slopeStatus?: string | null;

  // 카메라 감지 결과
  detectedObject?: string | null;
  confidence?: number | null;
  stdDev?: number | null;
  imageUrl?: string | null;
}

export interface RiskZone {
  id: string;
  score: number;
  grade: RiskGrade;
  cnt: number;
  last: string;
  st: ZoneStatus;
  loc: string;
}

export interface Trip {
  id: string;
  user: string;
  dev: string;
  st: TripStatus;
  start: string;
  end: string;
  cnt: number;
  rl: RiskLevel | null;
}

export interface Device {
  id: string;
  user: string;
  name: string;
  sensors: string[];
  st: DeviceStatus;
  last: string;
}

export interface AIAnalysis {
  id: string;
  score: number;
  grade: RiskGrade;
  cnt: number;
  reason: string;
  updated: string;
}

export interface Notification {
  id: number;
  title: string;
  body: string;
  color: string;
  read: boolean;
}

export interface ThemeTokens {
  bg: string;
  bgSub: string;
  card: string;
  cardHover: string;
  border: string;
  text: string;
  textSub: string;
  muted: string;
  sidebar: string;
  sidebarDeep: string;
  sidebarText: string;
  sidebarMuted: string;
  sidebarBorder: string;
  sidebarActive: string;
  header: string;
  input: string;
  shadow: string;
  shadowLg: string;
}
// AI 예측 결과 - 실제 API 연동 전 임시 규격
export interface AIPredictionResult {
  riskGrade: string;
  riskScore: number;
  reason: string;
  recommendedAction: string;
}