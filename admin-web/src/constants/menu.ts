import {
  LayoutDashboard, AlertTriangle, MapPin, Navigation2,
  Cpu, BrainCircuit, FileBarChart2, Settings,
} from "lucide-react";
import type { Page } from "../types/admin";

export const NAV = [
  { key: "dashboard" as Page, label: "Dashboard",   Icon: LayoutDashboard },
  { key: "events"    as Page, label: "위험 이벤트",  Icon: AlertTriangle },
  { key: "zones"     as Page, label: "위험 구간",    Icon: MapPin },
  { key: "trips"     as Page, label: "주행 기록",    Icon: Navigation2 },
  { key: "devices"   as Page, label: "디바이스",     Icon: Cpu },
  { key: "ai"        as Page, label: "AI 분석",      Icon: BrainCircuit },
  { key: "reports"   as Page, label: "리포트",       Icon: FileBarChart2 },
  { key: "settings"  as Page, label: "설정",         Icon: Settings },
];

export const PAGE_TITLE: Record<Page, string> = {
  dashboard:       "Dashboard",
  events:          "위험 이벤트",
  "event-detail":  "이벤트 상세",
  zones:           "위험 구간 지도",
  trips:           "주행 기록",
  devices:         "디바이스 관리",
  ai:              "AI 분석",
  reports:         "리포트",
  settings:        "설정",
  mypage:          "마이페이지",
};
