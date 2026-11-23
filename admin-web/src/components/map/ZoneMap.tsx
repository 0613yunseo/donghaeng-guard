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
  selectedId?: string | null;          // 선택된 구간: 크게 강조(깜빡임)하고 그 위치로 날아가 상세 말풍선을 연다
  hoverId?: string | null;             // 목록에서 마우스를 올린 구간: 지도에서 살짝 키워 보여 준다
  onSelect?: (id: string) => void;     // 마커를 눌렀을 때
  onHover?: (id: string | null) => void; // 마커에 마우스를 올리거나 뗐을 때
}

const SEOUL: L.LatLngExpression = [37.5665, 126.978];

/** 말풍선 내용. 서버 문자열이 들어가므로 innerHTML 대신 텍스트 노드로 만든다. */
function popupContent(z: ZoneMapItem): HTMLElement {
  const box = document.createElement("div");
  const title = document.createElement("strong");
  title.textContent = `${z.id} · ${z.grade}`;
  const line = document.createElement("div");
  line.textContent = z.label ?? `점수 ${z.score}점 · ${z.cnt}건`;
  box.append(title, line);
  if (!z.label && z.loc) {
    const loc = document.createElement("div");
    loc.style.maxWidth = "220px";
    loc.textContent = z.loc;
    box.append(loc);
  }
  return box;
}

/** OpenStreetMap 타일(API 키 불필요) 위에 위험 구간을 등급 색 원으로 표시한다. */
export function ZoneMap({ zones, selectedId = null, hoverId = null, onSelect, onHover }: ZoneMapProps) {
  const boxRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const layerRef = useRef<L.LayerGroup | null>(null);
  const markersRef = useRef<Map<string, L.CircleMarker>>(new Map());
  const onSelectRef = useRef(onSelect);
  const onHoverRef = useRef(onHover);
  const wasSelectedRef = useRef(false);

  useEffect(() => { onSelectRef.current = onSelect; onHoverRef.current = onHover; });

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
        .bindPopup(() => popupContent(z), { closeButton: false, offset: [0, -4] })
        .on("click", () => onSelectRef.current?.(z.id))
        .on("mouseover", () => onHoverRef.current?.(z.id))
        .on("mouseout", () => onHoverRef.current?.(null))
        .addTo(layer);
      marker.getElement()?.setAttribute("style", "cursor:pointer");
      markersRef.current.set(z.id, marker);
      points.push([z.lat, z.lng]);
    });
    if (points.length === 1) map.setView(points[0], 15);
    else if (points.length > 1) map.fitBounds(points, { padding: [40, 40], maxZoom: 16 });
  }, [zones]);

  // 선택(크게·진한 테두리·깜빡임)과 호버(살짝 크게)를 마커에 반영한다
  useEffect(() => {
    markersRef.current.forEach((marker, id) => {
      const selected = id === selectedId;
      const hovered = id === hoverId && !selected;
      marker.setStyle({
        radius: selected ? 17 : hovered ? 14 : 11,
        color: selected ? "#111827" : hovered ? "#1f2937" : "#fff",
        weight: selected ? 4 : hovered ? 3 : 2,
      });
      marker.getElement()?.classList.toggle("zone-pulse", selected);
      if (selected || hovered) marker.bringToFront();
    });
  }, [selectedId, hoverId, zones]);

  // 선택하면 그 위치로 날아가 확대하고 상세 말풍선을 연다. 선택이 풀리면 전체가 보이게 되돌린다.
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    const selected = selectedId ? markersRef.current.get(selectedId) : undefined;
    if (selected) {
      wasSelectedRef.current = true;
      map.flyTo(selected.getLatLng(), Math.max(map.getZoom(), 16), { duration: 0.8 });
      selected.openPopup();
    } else if (wasSelectedRef.current) {
      wasSelectedRef.current = false;
      map.closePopup();
      const points = [...markersRef.current.values()].map(m => m.getLatLng());
      if (points.length === 1) map.flyTo(points[0], 15, { duration: 0.8 });
      else if (points.length > 1) map.flyToBounds(L.latLngBounds(points), { padding: [40, 40], maxZoom: 16, duration: 0.8 });
    }
  }, [selectedId, zones]);

  return <div ref={boxRef} style={{ height: "100%", width: "100%", borderRadius: 12, zIndex: 0 }} />;
}
