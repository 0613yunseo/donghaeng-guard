import { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { gradeColor } from "../../constants/colors";
import type { RiskGrade } from "../../types/admin";

export interface ZoneMapItem {
  id: string;
  grade: RiskGrade;
  score: number;
  cnt: number;
  loc: string;
  label?: string;   // 있으면 툴팁에 점수·건수 대신 이 문구를 쓴다
  lat: number | null;
  lng: number | null;
}

interface ZoneMapProps {
  zones: ZoneMapItem[];
  selectedId?: string | null;          // 선택된 구간: 강조하고 그 위치로 확대한다
  onSelect?: (id: string) => void;     // 마커를 눌렀을 때
}

const SEOUL: L.LatLngExpression = [37.5665, 126.978];

/** OpenStreetMap 타일(API 키 불필요) 위에 위험 구간을 등급 색 원으로 표시한다. */
export function ZoneMap({ zones, selectedId = null, onSelect }: ZoneMapProps) {
  const boxRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const layerRef = useRef<L.LayerGroup | null>(null);
  const markersRef = useRef<Map<string, L.CircleMarker>>(new Map());
  const onSelectRef = useRef(onSelect);

  useEffect(() => { onSelectRef.current = onSelect; });

  // 지도 생성은 한 번만
  useEffect(() => {
    if (!boxRef.current) return;
    const map = L.map(boxRef.current).setView(SEOUL, 12);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "© OpenStreetMap",
    }).addTo(map);
    layerRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;
    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // 구간이 바뀌면 마커를 다시 그리고 범위를 맞춘다
  useEffect(() => {
    const map = mapRef.current;
    const layer = layerRef.current;
    if (!map || !layer) return;
    layer.clearLayers();
    markersRef.current.clear();
    const points: L.LatLngTuple[] = [];
    zones.forEach(z => {
      if (z.lat == null || z.lng == null) return;
      const color = gradeColor(z.grade);
      const marker = L.circleMarker([z.lat, z.lng], { radius: 11, color: "#fff", weight: 2, fillColor: color, fillOpacity: 0.9 })
        .bindTooltip(z.label ?? `${z.id} · ${z.grade} · 점수 ${z.score} · ${z.cnt}건<br/>${z.loc}`)
        .on("click", () => onSelectRef.current?.(z.id))
        .addTo(layer);
      markersRef.current.set(z.id, marker);
      points.push([z.lat, z.lng]);
    });
    if (points.length === 1) map.setView(points[0], 15);
    else if (points.length > 1) map.fitBounds(points, { padding: [40, 40], maxZoom: 16 });
  }, [zones]);

  // 선택된 구간은 크게·진한 테두리로 강조하고, 그 위치로 이동·확대한다
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    markersRef.current.forEach((marker, id) => {
      const on = id === selectedId;
      marker.setStyle({ radius: on ? 16 : 11, color: on ? "#111827" : "#fff", weight: on ? 4 : 2 });
      if (on) marker.bringToFront();
      else marker.closeTooltip();   // 이전에 선택해 열어 둔 말풍선은 닫는다
    });
    const selected = selectedId ? markersRef.current.get(selectedId) : undefined;
    if (selected) {
      map.setView(selected.getLatLng(), Math.max(map.getZoom(), 16), { animate: true });
      selected.openTooltip();
    }
  }, [selectedId, zones]);

  return <div ref={boxRef} style={{ height: "100%", width: "100%", borderRadius: 12, zIndex: 0 }} />;
}
