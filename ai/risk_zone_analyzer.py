"""DonghaengGuard risk-zone analysis (dry-run only)."""

import argparse
import json
import math
from collections import Counter
from datetime import datetime, timedelta, timezone
from pathlib import Path

import httpx

BACKEND_URL = 'http://16.179.249.107:8081'
CLUSTER_RADIUS_M = 25.0
ANALYSIS_DAYS = 7

RISK_WEIGHTS = {
    "WARNING": 50,
    "DANGER": 100,
}


def haversine_m(a, b):
    lat1, lon1 = a
    lat2, lon2 = b

    r = 6371000
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)

    h = (
        math.sin(dp / 2) ** 2
        + math.cos(p1) * math.cos(p2)
        * math.sin(dl / 2) ** 2
    )

    return 2 * r * math.asin(min(1, math.sqrt(h)))


def fetch_events():
    with httpx.Client(timeout=20.0) as client:
        response = client.get(f"{BACKEND_URL}/internal/ai/data")
        response.raise_for_status()
        payload = response.json()

    if payload.get("success") is not True:
        raise ValueError("Backend returned unsuccessful response")

    events = payload.get("data")

    if not isinstance(events, list):
        raise ValueError("Backend data must be a list")

    return events


def filter_events(events):
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    cutoff = now - timedelta(days=ANALYSIS_DAYS)
    filtered = []

    for event in events:
        if event.get("riskLevel") not in RISK_WEIGHTS:
            continue

        try:
            detected_at = datetime.fromisoformat(event["detectedAt"])
            lat = float(event["latitude"])
            lon = float(event["longitude"])
        except (KeyError, TypeError, ValueError):
            continue

        if not (-90 <= lat <= 90 and -180 <= lon <= 180):
            continue

        if detected_at < cutoff or detected_at > now:
            continue

        filtered.append({
            **event,
            "latitude": lat,
            "longitude": lon,
        })

    return filtered


def cluster_events(events):
    """Group events using connected components within 25m."""
    if not events:
        return []

    parent = list(range(len(events)))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(a, b):
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[rb] = ra

    # Compare unique coordinates rather than every repeated event.
    coords = {}
    for i, event in enumerate(events):
        key = (event["latitude"], event["longitude"])
        coords.setdefault(key, []).append(i)

    points = list(coords)

    for i, point_a in enumerate(points):
        for point_b in points[i + 1:]:
            if haversine_m(point_a, point_b) <= CLUSTER_RADIUS_M:
                union(coords[point_a][0], coords[point_b][0])

    for indices in coords.values():
        first = indices[0]
        for idx in indices[1:]:
            union(first, idx)

    groups = {}
    for i, event in enumerate(events):
        root = find(i)
        groups.setdefault(root, []).append(event)

    return list(groups.values())


def calculate_zone(cluster):
    counts = Counter(e["riskLevel"] for e in cluster)

    score = round(
        sum(RISK_WEIGHTS[e["riskLevel"]] for e in cluster)
        / len(cluster)
    )

    if score >= 70:
        grade = "HIGH"
    elif score >= 40:
        grade = "MEDIUM"
    else:
        grade = "LOW"

    sensor_counts = Counter(e.get("sensorType") for e in cluster)
    dominant_sensor = sensor_counts.most_common(1)[0][0]

    reason = (
        f"{dominant_sensor} 센서 중심 위험 이벤트 반복 감지 "
        f"(WARNING {counts['WARNING']}건, "
        f"DANGER {counts['DANGER']}건)"
    )

    return {
        "latitude": round(
            sum(e["latitude"] for e in cluster) / len(cluster), 7
        ),
        "longitude": round(
            sum(e["longitude"] for e in cluster) / len(cluster), 7
        ),
        "riskScore": score,
        "riskGrade": grade,
        "zoneEventCount": len(cluster),
        "reason": reason,
    }



def post_zones(zones):
    """Validate and replace backend risk zones explicitly."""
    if not zones:
        raise ValueError("위험 구간이 0개이므로 POST를 차단합니다.")

    valid_grades = {"LOW", "MEDIUM", "HIGH"}

    for zone in zones:
        if not (-90 <= zone["latitude"] <= 90):
            raise ValueError("잘못된 위도입니다.")

        if not (-180 <= zone["longitude"] <= 180):
            raise ValueError("잘못된 경도입니다.")

        if not (0 <= zone["riskScore"] <= 100):
            raise ValueError("잘못된 위험 점수입니다.")

        if zone["riskGrade"] not in valid_grades:
            raise ValueError("잘못된 위험 등급입니다.")

        if zone["zoneEventCount"] <= 0:
            raise ValueError("위험 이벤트 수가 0 이하입니다.")

    print("\n주의: 기존 위험 구간 전체가 교체됩니다.")
    confirmation = input("실제로 저장하려면 POST를 입력하세요: ")

    if confirmation != "POST":
        print("사용자가 저장을 취소했습니다.")
        return

    with httpx.Client(timeout=30.0) as client:
        response = client.post(
            f"{BACKEND_URL}/internal/ai/result",
            json={"zones": zones},
        )
        response.raise_for_status()
        result = response.json()

    if result.get("success") is not True:
        raise ValueError("백엔드 저장 실패")

    saved_count = result.get("data", {}).get("savedCount")

    if saved_count != len(zones):
        raise ValueError(
            f"저장 수 불일치: 요청 {len(zones)}개, 응답 {saved_count}개"
        )

    print(f"위험 구간 {saved_count}개 저장 완료")



ZONE_FIELDS = (
    "latitude",
    "longitude",
    "riskScore",
    "riskGrade",
    "zoneEventCount",
    "reason",
)


def normalize_zones(zones):
    """Compare only meaningful zone fields, ignoring DB metadata."""
    normalized = []

    for zone in zones:
        item = {field: zone[field] for field in ZONE_FIELDS}
        item["latitude"] = round(float(item["latitude"]), 7)
        item["longitude"] = round(float(item["longitude"]), 7)
        normalized.append(item)

    return sorted(
        normalized,
        key=lambda z: (
            z["latitude"],
            z["longitude"],
            z["riskScore"],
            z["riskGrade"],
            z["zoneEventCount"],
            z["reason"],
        ),
    )


def auto_update_zones(zones):
    """Update backend only when nonempty analysis results changed."""
    if not zones:
        print("AUTO SKIP: 위험 구간 0개 - 기존 DB 유지")
        return

    with httpx.Client(timeout=30.0) as client:
        response = client.get(f"{BACKEND_URL}/api/risk-zones")
        response.raise_for_status()
        payload = response.json()

    if payload.get("success") is not True:
        raise ValueError("기존 위험 구간 조회 실패")

    existing = payload.get("data")

    if not isinstance(existing, list):
        raise ValueError("기존 위험 구간 응답 형식 오류")

    if normalize_zones(existing) == normalize_zones(zones):
        print("AUTO SKIP: 기존 위험 구간과 동일 - POST 생략")
        return

    print("AUTO UPDATE: 위험 구간 변경 감지")

    # 자동 저장은 별도 함수에서 처리하여 수동 확인 절차와 분리
    with httpx.Client(timeout=30.0) as client:
        response = client.post(
            f"{BACKEND_URL}/internal/ai/result",
            json={"zones": zones},
        )
        response.raise_for_status()
        result = response.json()

    if result.get("success") is not True:
        raise ValueError("자동 갱신 저장 실패")

    saved_count = result.get("data", {}).get("savedCount")

    if saved_count != len(zones):
        raise ValueError(
            f"저장 수 불일치: 요청 {len(zones)}개, 응답 {saved_count}개"
        )

    print(f"AUTO SUCCESS: 위험 구간 {saved_count}개 저장 완료")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--post",
        action="store_true",
        help="분석 결과를 백엔드에 저장합니다.",
    )
    parser.add_argument(
        "--auto",
        action="store_true",
        help="변경된 위험 구간만 자동 저장합니다.",
    )
    args = parser.parse_args()

    if args.post and args.auto:
        parser.error("--post와 --auto는 동시에 사용할 수 없습니다.")

    events = fetch_events()
    filtered = filter_events(events)
    clusters = cluster_events(filtered)

    zones = [calculate_zone(c) for c in clusters]
    zones.sort(key=lambda z: (-z["riskScore"], -z["zoneEventCount"]))

    result = {"zones": zones}

    print("===== RISK ZONE DRY RUN =====")
    print("전체 이벤트:", len(events))
    print("분석 대상 이벤트:", len(filtered))
    print("생성된 위험 구간:", len(zones))
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if args.auto:
        auto_update_zones(zones)
    elif args.post:
        post_zones(zones)
    else:
        print("DRY RUN ONLY - 백엔드 POST 전송 없음")


if __name__ == "__main__":
    main()
