import type { RiskLevel, TripStatus } from "./admin";

export interface SensorEventResponse {
  eventId: number;
  sensorType: string;
  riskLevel: RiskLevel;
  latitude: number | null;
  longitude: number | null;
  detectedAt: string;

  // 디바이스 정보
  deviceId?: string | null;
  deviceName?: string | null;

  // 초음파용
  sensorId?: number | null;
  distanceMm?: number | null;

  // 기울기용
  pitch?: number | null;
  slopeStatus?: string | null;

  // 카메라용
  detectedObject?: string | null;
  confidence?: number | null;
  stdDev?: number | null;
  imageUrl?: string | null;
}

export interface TripDetailResponse {
  tripId: number;
  startedAt: string;
  endedAt: string | null;
  status: TripStatus;
  eventCount: number;
  highestRiskLevel: RiskLevel | null;
  events: SensorEventResponse[];
}

export interface TripApiResponse {
  success: boolean;
  data: TripDetailResponse | null;
  message: string;
}