import type { ReactNode } from "react";

interface MapPlaceholderProps {
  label?: string;
  children?: ReactNode;
  style?: React.CSSProperties;
  className?: string;
}

export function MapPlaceholder({ label = "서울특별시 위험 구간 현황", children, style, className = "" }: MapPlaceholderProps) {
  return (
    <div
      className={`relative overflow-hidden rounded-xl ${className}`}
      style={{ background: "linear-gradient(160deg,#0d1e3a 0%,#06111e 100%)", minHeight: 300, ...style }}
    >
      <svg className="absolute inset-0 w-full h-full" style={{ opacity: 0.12 }} viewBox="0 0 600 420" preserveAspectRatio="none">
        {[42,84,126,168,210,252,294,336,378].map(y =>
          <line key={`h${y}`} x1="0" y1={y} x2="600" y2={y} stroke="#3A7BD5" strokeWidth="0.8" />
        )}
        {[60,120,180,240,300,360,420,480,540].map(x =>
          <line key={`v${x}`} x1={x} y1="0" x2={x} y2="420" stroke="#3A7BD5" strokeWidth="0.8" />
        )}
        <path d="M0 200 Q100 155 200 180 Q310 205 400 160 Q500 115 600 145" stroke="#3A7BD5" strokeWidth="3" fill="none" />
        <path d="M0 260 Q90 240 175 260 Q265 280 345 248 Q425 218 495 245 Q560 265 600 255" stroke="#4A8BFF" strokeWidth="1.8" fill="none" />
        <path d="M115 0 Q128 105 118 210 Q108 315 128 420" stroke="#3A7BD5" strokeWidth="2" fill="none" />
        <path d="M305 0 Q295 85 315 168 Q335 248 308 338 Q282 398 308 420" stroke="#4A8BFF" strokeWidth="1.5" fill="none" />
        {([
          [78,48,130,90],
          [252,125,108,72],
          [388,195,148,100],
          [58,280,135,78],
        ] as [number,number,number,number][]).map(([x,y,w,h], i) => (
          <rect key={i} x={x} y={y} width={w} height={h} rx="7" fill="#254678" opacity="0.45" />
        ))}
      </svg>

      <div className="absolute top-3 left-3 rounded-lg px-2.5 py-1.5 text-[11px]"
        style={{ background: "rgba(0,0,0,0.5)", color: "rgba(255,255,255,0.75)" }}>
        {label}
      </div>

      {children}
    </div>
  );
}
