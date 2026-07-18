import type { RiskEvent, RiskZone, Trip, Device, AIAnalysis, Notification } from "../types/admin";
import { C } from "../constants/colors";

export const EVENTS: RiskEvent[] = [
  { id:"EVT-001", at:"2024-06-30 14:23:11", rl:"DANGER",  sensor:"TOF",        rt:"OBSTACLE", mm:120, lat:37.5563, lng:126.9723, dev:"DG-ESP32-001", trip:"TRIP-001", user:"USR-001" },
  { id:"EVT-002", at:"2024-06-30 13:55:04", rl:"WARNING", sensor:"ULTRASONIC", rt:"CURB",     mm:250, lat:37.5571, lng:126.9731, dev:"DG-ESP32-002", trip:"TRIP-002", user:"USR-002" },
  { id:"EVT-003", at:"2024-06-30 13:12:48", rl:"SAFE",    sensor:"TOF",        rt:"SLOPE",    mm:480, lat:37.5580, lng:126.9745, dev:"DG-ESP32-001", trip:"TRIP-001", user:"USR-001" },
  { id:"EVT-004", at:"2024-06-30 12:44:33", rl:"DANGER",  sensor:"ULTRASONIC", rt:"OBSTACLE", mm:85,  lat:37.5548, lng:126.9710, dev:"DG-ESP32-003", trip:"TRIP-003", user:"USR-003" },
  { id:"EVT-005", at:"2024-06-30 11:30:22", rl:"WARNING", sensor:"TOF",        rt:"UNKNOWN",  mm:310, lat:37.5595, lng:126.9752, dev:"DG-ESP32-002", trip:"TRIP-004", user:"USR-004" },
  { id:"EVT-006", at:"2024-06-30 10:15:09", rl:"SAFE",    sensor:"ULTRASONIC", rt:"SLOPE",    mm:620, lat:37.5608, lng:126.9768, dev:"DG-ESP32-004", trip:"TRIP-005", user:"USR-005" },
  { id:"EVT-007", at:"2024-06-30 09:58:45", rl:"DANGER",  sensor:"TOF",        rt:"CURB",     mm:95,  lat:37.5535, lng:126.9698, dev:"DG-ESP32-001", trip:"TRIP-006", user:"USR-001" },
  { id:"EVT-008", at:"2024-06-30 09:22:17", rl:"WARNING", sensor:"ULTRASONIC", rt:"OBSTACLE", mm:195, lat:37.5519, lng:126.9685, dev:"DG-ESP32-005", trip:"TRIP-007", user:"USR-006" },
];

export const ZONES: RiskZone[] = [
  { id:"ZONE-001", score:92, grade:"HIGH",   cnt:47, last:"2024-06-30 14:23", st:"검토 중",   loc:"마포구 홍익로 23" },
  { id:"ZONE-002", score:78, grade:"HIGH",   cnt:35, last:"2024-06-30 12:11", st:"확인 전",  loc:"마포구 양화로 15" },
  { id:"ZONE-003", score:61, grade:"MEDIUM", cnt:22, last:"2024-06-29 18:44", st:"조치 완료", loc:"은평구 응암로 8" },
  { id:"ZONE-004", score:55, grade:"MEDIUM", cnt:18, last:"2024-06-29 15:30", st:"검토 중",   loc:"서대문구 신촌로 31" },
  { id:"ZONE-005", score:34, grade:"LOW",    cnt:9,  last:"2024-06-28 11:20", st:"조치 완료", loc:"용산구 이태원로 45" },
];

export const TRIPS: Trip[] = [
  { id:"TRIP-001", user:"USR-001", dev:"DG-ESP32-001", st:"ENDED",   start:"2024-06-30 09:00", end:"2024-06-30 09:45", cnt:5, rl:"DANGER"  },
  { id:"TRIP-002", user:"USR-002", dev:"DG-ESP32-002", st:"STARTED", start:"2024-06-30 13:30", end:"—",                cnt:2, rl:"WARNING" },
  { id:"TRIP-003", user:"USR-003", dev:"DG-ESP32-003", st:"ENDED",   start:"2024-06-30 10:15", end:"2024-06-30 11:00", cnt:7, rl:"DANGER"  },
  { id:"TRIP-004", user:"USR-004", dev:"DG-ESP32-002", st:"ENDED",   start:"2024-06-30 08:00", end:"2024-06-30 08:50", cnt:3, rl:"WARNING" },
  { id:"TRIP-005", user:"USR-005", dev:"DG-ESP32-004", st:"STARTED", start:"2024-06-30 14:00", end:"—",                cnt:1, rl:"SAFE"    },
  { id:"TRIP-006", user:"USR-001", dev:"DG-ESP32-001", st:"ENDED",   start:"2024-06-29 16:00", end:"2024-06-29 17:10", cnt:4, rl:"DANGER"  },
];

export const DEVICES: Device[] = [
  { id:"DG-ESP32-001", user:"USR-001", name:"휠체어 감지기 A", sensors:["TOF","ULTRASONIC"], st:"CONNECTED",    last:"2024-06-30 14:20" },
  { id:"DG-ESP32-002", user:"USR-002", name:"휠체어 감지기 B", sensors:["ULTRASONIC"],       st:"CONNECTED",    last:"2024-06-30 13:55" },
  { id:"DG-ESP32-003", user:"USR-003", name:"휠체어 감지기 C", sensors:["TOF"],              st:"DISCONNECTED", last:"2024-06-29 18:00" },
  { id:"DG-ESP32-004", user:"USR-004", name:"휠체어 감지기 D", sensors:["TOF","ULTRASONIC"], st:"CONNECTED",    last:"2024-06-30 14:01" },
  { id:"DG-ESP32-005", user:"USR-005", name:"휠체어 감지기 E", sensors:["ULTRASONIC"],       st:"ERROR",        last:"2024-06-28 12:30" },
  { id:"DG-ESP32-006", user:"USR-006", name:"휠체어 감지기 F", sensors:["TOF"],              st:"DISCONNECTED", last:"2024-06-27 09:15" },
];

export const AI_DATA: AIAnalysis[] = [
  { id:"ZONE-001", score:92, grade:"HIGH",   cnt:47, reason:"단차·장애물 반복 감지. 보도블록 파손 구간으로 추정.",      updated:"2024-06-30 06:00" },
  { id:"ZONE-002", score:78, grade:"HIGH",   cnt:35, reason:"경사로 충돌 위험. 야간 조도 부족으로 감지 오류 가능성.",   updated:"2024-06-30 06:00" },
  { id:"ZONE-003", score:61, grade:"MEDIUM", cnt:22, reason:"횡단보도 앞 단차 감지. 점자블록 손상 이상 반응.",         updated:"2024-06-29 06:00" },
  { id:"ZONE-004", score:55, grade:"MEDIUM", cnt:18, reason:"버스정류장 불법주차로 인한 장애물 빈발.",               updated:"2024-06-29 06:00" },
  { id:"ZONE-005", score:34, grade:"LOW",    cnt:9,  reason:"공사 구간 임시 보호대 반응. 공사 종료 후 감소 예상.",     updated:"2024-06-28 06:00" },
];

export const NOTIFICATIONS: Notification[] = [
  { id:1, title:"DANGER 이벤트 발생", body:"EVT-007 · DG-ESP32-001 · 방금 전", color:C.danger, read:false },
  { id:2, title:"디바이스 오류 감지",  body:"DG-ESP32-005 ERROR 상태 · 2분 전",  color:C.warn,   read:false },
  { id:3, title:"AI 분석 완료",        body:"위험 구간 5개 업데이트 · 06:00",     color:C.blue,   read:true  },
];

export const trendData = [
  { d:"06/24", w:8,  dan:3 }, { d:"06/25", w:12, dan:5 }, { d:"06/26", w:6,  dan:2 },
  { d:"06/27", w:15, dan:7 }, { d:"06/28", w:10, dan:4 }, { d:"06/29", w:18, dan:9 },
  { d:"06/30", w:14, dan:6 },
];

export const weeklyData = [
  { w:"5/4주", safe:120, warn:45, dan:12 },
  { w:"6/1주", safe:98,  warn:52, dan:18 },
  { w:"6/2주", safe:135, warn:38, dan:9  },
  { w:"6/3주", safe:142, warn:61, dan:22 },
  { w:"6/4주", safe:110, warn:48, dan:15 },
];

export const regionData = [
  { r:"마포구",   s:78 },
  { r:"은평구",   s:62 },
  { r:"서대문구", s:55 },
  { r:"용산구",   s:41 },
  { r:"종로구",   s:38 },
];
