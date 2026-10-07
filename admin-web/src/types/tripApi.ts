import type { RiskLevel, TripStatus } from "./admin";

export interface SensorEventResponse {
  eventId: number;
  sensorType: string;
  riskLevel: RiskLevel;
  latitude: number | null;
  longitude: number | null;
  detectedAt: string;
  sensorId?: number | null;
  distanceMm?: number | null;
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
