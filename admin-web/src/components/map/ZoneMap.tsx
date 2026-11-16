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
  lat: number | null;
  lng: number | null;
}

const SEOUL: L.LatLngExpression = [37.5665, 126.978];

/** OpenStreetMap 타일(API 키 불필요) 위에 위험 구간을 등급 색 원으로 표시한다. */
export function ZoneMap({ zones }: { zones: ZoneMapItem[] }) {
  const boxRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const layerRef = useRef<L.LayerGroup | null>(null);

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
    const points: L.LatLngTuple[] = [];
    zones.forEach(z => {
      if (z.lat == null || z.lng == null) return;
      const color = gradeColor(z.grade);
      L.circleMarker([z.lat, z.lng], { radius: 11, color: "#fff", weight: 2, fillColor: color, fillOpacity: 0.9 })
        .bindTooltip(`${z.id} · ${z.grade} · 점수 ${z.score} · ${z.cnt}건<br/>${z.loc}`)
        .addTo(layer);
      points.push([z.lat, z.lng]);
    });
    if (points.length === 1) map.setView(points[0], 15);
    else if (points.length > 1) map.fitBounds(points, { padding: [40, 40], maxZoom: 16 });
  }, [zones]);

  return <div ref={boxRef} style={{ height: "100%", width: "100%", borderRadius: 12, zIndex: 0 }} />;
}
