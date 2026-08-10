import type { ThemeTokens, RiskLevel, RiskGrade } from "../types/admin";

export const LIGHT: ThemeTokens = {
  bg:            "#EAF2FA",
  bgSub:         "#F1F5F9",
  card:          "#FFFFFF",
  cardHover:     "#F8FAFD",
  border:        "#D8E2EC",
  text:          "#10213F",
  textSub:       "#2E4A6B",
  muted:         "#64748B",
  sidebar:       "#1B3A6B",
  sidebarDeep:   "#142d54",
  sidebarText:   "rgba(255,255,255,0.9)",
  sidebarMuted:  "rgba(255,255,255,0.4)",
  sidebarBorder: "rgba(255,255,255,0.08)",
  sidebarActive: "rgba(58,123,213,0.28)",
  header:        "#FFFFFF",
  input:         "#F1F5F9",
  shadow:        "0 1px 3px rgba(16,33,63,0.08), 0 0 0 1px rgba(16,33,63,0.05)",
  shadowLg:      "0 8px 24px rgba(16,33,63,0.10)",
};

export const DARK: ThemeTokens = {
  bg:            "#0B1626",
  bgSub:         "#101e33",
  card:          "#132240",
  cardHover:     "#162848",
  border:        "rgba(255,255,255,0.08)",
  text:          "#E0EAF8",
  textSub:       "#A0B8D0",
  muted:         "#5A7A9A",
  sidebar:       "#0D1A2C",
  sidebarDeep:   "#09141f",
  sidebarText:   "rgba(255,255,255,0.88)",
  sidebarMuted:  "rgba(255,255,255,0.35)",
  sidebarBorder: "rgba(255,255,255,0.06)",
  sidebarActive: "rgba(58,123,213,0.32)",
  header:        "#132240",
  input:         "#0D1A2C",
  shadow:        "0 1px 4px rgba(0,0,0,0.35), 0 0 0 1px rgba(255,255,255,0.04)",
  shadowLg:      "0 8px 32px rgba(0,0,0,0.4)",
};

export const C = {
  navy:   "#1B3A6B",
  blue:   "#3A7BD5",
  mint:   "#00C896",
  safe:   "#2ECC71",
  warn:   "#FFB703",
  danger: "#E63946",
} as const;

export const gradeColor = (g: RiskGrade): string =>
  g === "HIGH" ? C.danger : g === "MEDIUM" ? C.warn : C.blue;

export const riskBg = (rl: RiskLevel): string =>
  rl === "DANGER" ? `${C.danger}15` : rl === "WARNING" ? `${C.warn}13` : `${C.safe}12`;

export const riskFg = (rl: RiskLevel): string =>
  rl === "DANGER" ? "#c0202d" : rl === "WARNING" ? "#8a5d00" : "#1a7a40";
