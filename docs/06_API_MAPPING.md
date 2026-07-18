# API Mapping

---

## 0. 문서 정보


| 항목    | 내용                                                              |
| ----- | --------------------------------------------------------------- |
| 작성자   | 최윤서                                                             |
| 문서명   | 동행가드 API Mapping 문서 v1                                          |
| 기준 문서 | PRD v3, 사용자용 App IA v3, 관리자용 Web IA v1, 화면별 기능 정의서, 화면별 데이터 정의서 |
| 작성 목적 | 사용자용 앱과 관리자용 웹에서 필요한 API 호출 지점을 화면 단위로 정리                       |
| 작성 주체 | 앱개발 / 프론트엔드 담당                                                  |
| 협의 대상 | 백엔드 담당                                                          |
| 후속 문서 | ERD 요구사항, API 명세서, 백엔드 구현 계획                                    |


---

# 1. 문서 작성 목적

이 문서는 **화면별로 어떤 API가 필요한지** 정리하기 위한 문서이다.

앱개발 담당은 화면 흐름과 화면별 데이터를 기준으로 API가 필요한 지점을 먼저 정리한다.

백엔드 담당은 이 문서를 기준으로 ERD, API URL, Request Body, Response Body, DB 저장 구조를 설계한다.

```
화면별 데이터 정의서
→ API Mapping
→ 백엔드 ERD
→ API 명세서
→ 프론트/백엔드 연동

```

---

# 2. API 우선순위 기준


| 우선순위 | 의미                    |
| ---- | --------------------- |
| P0   | MVP 구현에 반드시 필요한 API   |
| P1   | MVP 이후 1차 확장에 필요한 API |
| P2   | 후순위 또는 고도화 API        |


---

# 3. 사용자용 App API Mapping

## 3.1 간편 시작 / 로그인 화면


| 항목        | 내용                     |
| --------- | ---------------------- |
| 화면        | 간편 시작 / 로그인            |
| 사용자 행동    | 간편 시작 또는 로그인           |
| 목적        | userId 생성 또는 기존 사용자 식별 |
| 우선순위      | P1                     |
| API 필요 여부 | 필요 가능                  |



| 기능    | Method | API 후보             | 요청 데이터                    | 응답 데이터              |
| ----- | ------ | ------------------ | ------------------------- | ------------------- |
| 간편 시작 | POST   | `/api/users/guest` | deviceInfo                | userId, userStatus  |
| 로그인   | POST   | `/api/auth/login`  | email, password           | accessToken, userId |
| 회원가입  | POST   | `/api/auth/signup` | email, password, nickname | userId              |


### MVP 처리안

```
초기 MVP에서는 로그인 없이 임시 userId 또는 guest userId 방식으로 처리 가능하다.

```

---

## 3.2 센서 연결 화면


| 항목        | 내용                          |
| --------- | --------------------------- |
| 화면        | 센서 연결 화면                    |
| 사용자 행동    | ESP32 센서 검색 및 연결            |
| 목적        | deviceId 확인 또는 사용자와 디바이스 연결 |
| 우선순위      | P1                          |
| API 필요 여부 | 선택                          |



| 기능            | Method | API 후보                           | 요청 데이터                                    | 응답 데이터                 |
| ------------- | ------ | -------------------------------- | ----------------------------------------- | ---------------------- |
| 디바이스 등록       | POST   | `/api/devices`                   | userId, deviceId, deviceName, sensorTypes | deviceId, deviceStatus |
| 내 디바이스 조회     | GET    | `/api/devices?userId={userId}`   | userId                                    | devices[]              |
| 디바이스 연결 상태 갱신 | PATCH  | `/api/devices/{deviceId}/status` | deviceStatus, lastConnectedAt             | updated                |


### 비고

```
BLE 검색/연결 자체는 앱 내부 기능이다.
백엔드는 deviceId 등록, 사용자-디바이스 매핑, 마지막 연결 시간 저장이 필요할 때 사용한다.

```

---

## 3.3 메인 주행 화면


| 항목        | 내용                      |
| --------- | ----------------------- |
| 화면        | 메인 주행 화면                |
| 사용자 행동    | 주행 시작, 주행 종료, 위험 상태 확인  |
| 목적        | 주행 세션 생성 및 실시간 주행 상태 관리 |
| 우선순위      | P0                      |
| API 필요 여부 | 필요                      |



| 기능          | Method | API 후보                       | 요청 데이터                      | 응답 데이터                              |
| ----------- | ------ | ---------------------------- | --------------------------- | ----------------------------------- |
| 주행 시작       | POST   | `/api/trips/start`           | userId, deviceId, startedAt | tripId, status, startedAt           |
| 주행 종료       | PATCH  | `/api/trips/{tripId}/end`    | endedAt                     | tripId, status, eventCount, endedAt |
| 현재 주행 상태 조회 | GET    | `/api/trips/{tripId}/status` | tripId                      | tripStatus, eventCount              |


### 주행 시작 요청 예시

```json
{
  "userId": 1,
  "deviceId": "DG-ESP32-001",
  "startedAt": "2026-05-22T14:00:00"
}

```

### 주행 시작 응답 예시

```json
{
  "tripId": 1,
  "status": "STARTED",
  "startedAt": "2026-05-22T14:00:00"
}

```

---

## 3.4 위험 알림 화면


| 항목        | 내용                        |
| --------- | ------------------------- |
| 화면        | 위험 알림 화면                  |
| 사용자 행동    | WARNING/DANGER 확인         |
| 목적        | 위험 이벤트를 GPS 위치와 함께 서버에 저장 |
| 우선순위      | P0                        |
| API 필요 여부 | 필요                        |



| 기능          | Method | API 후보                  | 요청 데이터                                                                               | 응답 데이터                  |
| ----------- | ------ | ----------------------- | ------------------------------------------------------------------------------------ | ----------------------- |
| 위험 이벤트 저장   | POST   | `/api/risk-events`      | tripId, deviceId, sensorType, riskLevel, distanceMm, latitude, longitude, detectedAt | eventId, saved          |
| 미전송 이벤트 재전송 | POST   | `/api/risk-events/bulk` | events[]                                                                             | savedCount, failedCount |


### 위험 이벤트 저장 요청 예시

```json
{
  "tripId": 1,
  "deviceId": "DG-ESP32-001",
  "sensorType": "TOF",
  "riskType": "OBSTACLE",
  "riskLevel": "DANGER",
  "distanceMm": 180,
  "latitude": 35.1796,
  "longitude": 129.0756,
  "detectedAt": "2026-05-22T14:30:00"
}

```

### 위험 이벤트 저장 응답 예시

```json
{
  "eventId": 101,
  "saved": true
}

```

---

## 3.5 주행 종료 화면


| 항목        | 내용                  |
| --------- | ------------------- |
| 화면        | 주행 종료 화면            |
| 사용자 행동    | 주행 종료 확인            |
| 목적        | 주행 세션 종료 및 요약 정보 저장 |
| 우선순위      | P0                  |
| API 필요 여부 | 필요                  |



| 기능       | Method | API 후보                        | 요청 데이터                     | 응답 데이터                         |
| -------- | ------ | ----------------------------- | -------------------------- | ------------------------------ |
| 주행 종료    | PATCH  | `/api/trips/{tripId}/end`     | endedAt, pendingEventCount | tripId, status, eventCount     |
| 주행 요약 조회 | GET    | `/api/trips/{tripId}/summary` | tripId                     | startedAt, endedAt, eventCount |


### 비고

```
주행 종료 API는 메인 주행 화면에서도 호출될 수 있다.
화면 기준으로는 주행 종료 화면에서 결과를 보여준다.

```

---

## 3.6 주행 기록 화면


| 항목        | 내용            |
| --------- | ------------- |
| 화면        | 주행 기록 화면      |
| 사용자 행동    | 과거 주행 목록 확인   |
| 목적        | 사용자별 주행 기록 조회 |
| 우선순위      | P1            |
| API 필요 여부 | 필요            |



| 기능          | Method | API 후보                       | 요청 데이터 | 응답 데이터  |
| ----------- | ------ | ---------------------------- | ------ | ------- |
| 주행 기록 목록 조회 | GET    | `/api/trips?userId={userId}` | userId | trips[] |
| 주행 기록 새로고침  | GET    | `/api/trips?userId={userId}` | userId | trips[] |


### 응답 예시

```json
{
  "trips": [
    {
      "tripId": 1,
      "startedAt": "2026-05-22T14:00:00",
      "endedAt": "2026-05-22T15:00:00",
      "status": "ENDED",
      "eventCount": 5,
      "highestRiskLevel": "DANGER"
    }
  ]
}

```

---

## 3.7 주행 상세 화면


| 항목        | 내용                      |
| --------- | ----------------------- |
| 화면        | 주행 상세 화면                |
| 사용자 행동    | 특정 주행 기록 선택             |
| 목적        | 해당 주행의 위험 이벤트 목록과 위치 확인 |
| 우선순위      | P1                      |
| API 필요 여부 | 필요                      |



| 기능           | Method | API 후보                            | 요청 데이터 | 응답 데이터                |
| ------------ | ------ | --------------------------------- | ------ | --------------------- |
| 주행 상세 조회     | GET    | `/api/trips/{tripId}`             | tripId | trip detail, events[] |
| 위험 이벤트 목록 조회 | GET    | `/api/trips/{tripId}/risk-events` | tripId | events[]              |


### 응답 예시

```json
{
  "tripId": 1,
  "startedAt": "2026-05-22T14:00:00",
  "endedAt": "2026-05-22T15:00:00",
  "status": "ENDED",
  "events": [
    {
      "eventId": 101,
      "sensorType": "TOF",
      "riskType": "OBSTACLE",
      "riskLevel": "DANGER",
      "distanceMm": 180,
      "latitude": 35.1796,
      "longitude": 129.0756,
      "detectedAt": "2026-05-22T14:30:00"
    }
  ]
}

```

---

## 3.8 위험구간 화면


| 항목        | 내용                             |
| --------- | ------------------------------ |
| 화면        | 위험구간 화면                        |
| 사용자 행동    | 위험 구간 지도/리스트 확인                |
| 목적        | 누적 위험 이벤트 또는 AI 분석 기반 위험 구간 확인 |
| 우선순위      | P1                             |
| API 필요 여부 | 필요                             |



| 기능          | Method | API 후보                     | 요청 데이터                             | 응답 데이터           |
| ----------- | ------ | -------------------------- | ---------------------------------- | ---------------- |
| 위험 구간 조회    | GET    | `/api/risk-zones`          | currentLocation, radius, riskGrade | riskZones[]      |
| 위험 구간 상세 조회 | GET    | `/api/risk-zones/{zoneId}` | zoneId                             | risk zone detail |
| 안전 경로 요청    | POST   | `/api/routes/recommend`    | start, destination                 | routeResult      |


### 위험 구간 응답 예시

```json
{
  "riskZones": [
    {
      "zoneId": 1,
      "latitude": 35.1796,
      "longitude": 129.0756,
      "riskScore": 85,
      "riskGrade": "HIGH",
      "eventCount": 7,
      "reason": "동일 구간에서 DANGER 이벤트가 반복 감지되었습니다."
    }
  ]
}

```

---

## 3.9 설정 화면


| 항목        | 내용               |
| --------- | ---------------- |
| 화면        | 설정 화면            |
| 사용자 행동    | 알림, 접근성, 디바이스 관리 |
| 목적        | 사용자 설정 관리        |
| 우선순위      | P2               |
| API 필요 여부 | 일부 선택            |



| 기능         | Method | API 후보                             | 요청 데이터           | 응답 데이터   |
| ---------- | ------ | ---------------------------------- | ---------------- | -------- |
| 사용자 설정 조회  | GET    | `/api/users/{userId}/settings`     | userId           | settings |
| 사용자 설정 수정  | PATCH  | `/api/users/{userId}/settings`     | settings         | updated  |
| 디바이스 연결 해제 | PATCH  | `/api/devices/{deviceId}/unassign` | userId, deviceId | updated  |


### MVP 처리안

```
알림, 글씨 크기, 고대비 설정은 MVP에서는 로컬 저장으로 처리 가능하다.

```

---

# 4. 관리자용 Web API Mapping

## 4.1 관리자 로그인 화면


| 항목   | 내용        |
| ---- | --------- |
| 화면   | 관리자 로그인   |
| 목적   | 관리자 권한 인증 |
| 우선순위 | P0        |



| 기능        | Method | API 후보                  | 요청 데이터          | 응답 데이터                     |
| --------- | ------ | ----------------------- | --------------- | -------------------------- |
| 관리자 로그인   | POST   | `/api/admin/auth/login` | email, password | adminId, role, accessToken |
| 관리자 정보 조회 | GET    | `/api/admin/me`         | accessToken     | adminInfo                  |


---

## 4.2 관리자 대시보드 화면


| 항목   | 내용                  |
| ---- | ------------------- |
| 화면   | 관리자 대시보드            |
| 목적   | 전체 위험 데이터와 운영 상태 요약 |
| 우선순위 | P0                  |



| 기능           | Method | API 후보                         | 요청 데이터    | 응답 데이터                                                            |
| ------------ | ------ | ------------------------------ | --------- | ----------------------------------------------------------------- |
| 대시보드 요약 조회   | GET    | `/api/admin/dashboard/summary` | dateRange | totalEventCount, todayEventCount, topRiskZones, activeDeviceCount |
| 최근 주행 조회     | GET    | `/api/admin/trips/recent`      | limit     | recentTrips[]                                                     |
| 위험 구간 TOP 조회 | GET    | `/api/admin/risk-zones/top`    | limit     | topRiskZones[]                                                    |


---

## 4.3 위험 이벤트 목록 화면


| 항목   | 내용                 |
| ---- | ------------------ |
| 화면   | 위험 이벤트 목록          |
| 목적   | 전체 위험 이벤트 조회 및 필터링 |
| 우선순위 | P0                 |



| 기능           | Method | API 후보                          | 요청 데이터                                         | 응답 데이터               |
| ------------ | ------ | ------------------------------- | ---------------------------------------------- | -------------------- |
| 위험 이벤트 목록 조회 | GET    | `/api/admin/risk-events`        | dateRange, riskLevel, sensorType, region, page | events[], pagination |
| 위험 이벤트 검색    | GET    | `/api/admin/risk-events/search` | keyword                                        | events[]             |


---

## 4.4 위험 이벤트 상세 화면


| 항목   | 내용              |
| ---- | --------------- |
| 화면   | 위험 이벤트 상세       |
| 목적   | 개별 위험 이벤트 상세 확인 |
| 우선순위 | P0              |



| 기능           | Method | API 후보                             | 요청 데이터   | 응답 데이터        |
| ------------ | ------ | ---------------------------------- | -------- | ------------- |
| 위험 이벤트 상세 조회 | GET    | `/api/admin/risk-events/{eventId}` | eventId  | event detail  |
| 연결된 주행 조회    | GET    | `/api/admin/trips/{tripId}`        | tripId   | trip detail   |
| 연결된 디바이스 조회  | GET    | `/api/admin/devices/{deviceId}`    | deviceId | device detail |


---

## 4.5 위험 구간 지도 화면


| 항목   | 내용                |
| ---- | ----------------- |
| 화면   | 위험 구간 지도          |
| 목적   | 위험 구간을 지도 기반으로 확인 |
| 우선순위 | P0                |



| 기능          | Method | API 후보                                  | 요청 데이터                       | 응답 데이터          |
| ----------- | ------ | --------------------------------------- | ---------------------------- | --------------- |
| 위험 구간 지도 조회 | GET    | `/api/admin/risk-zones/map`             | dateRange, region, riskGrade | riskZones[]     |
| 위험 구간 상세 조회 | GET    | `/api/admin/risk-zones/{zoneId}`        | zoneId                       | riskZone detail |
| 위험 구간 상태 변경 | PATCH  | `/api/admin/risk-zones/{zoneId}/status` | zoneStatus                   | updated         |


---

## 4.6 주행 기록 관리 화면


| 항목   | 내용              |
| ---- | --------------- |
| 화면   | 주행 기록 관리        |
| 목적   | 전체 사용자 주행 세션 관리 |
| 우선순위 | P1              |



| 기능          | Method | API 후보                                  | 요청 데이터                            | 응답 데이터      |
| ----------- | ------ | --------------------------------------- | --------------------------------- | ----------- |
| 전체 주행 목록 조회 | GET    | `/api/admin/trips`                      | dateRange, userId, deviceId, page | trips[]     |
| 주행 상세 조회    | GET    | `/api/admin/trips/{tripId}`             | tripId                            | trip detail |
| 주행별 이벤트 조회  | GET    | `/api/admin/trips/{tripId}/risk-events` | tripId                            | events[]    |


---

## 4.7 사용자 관리 화면


| 항목   | 내용                |
| ---- | ----------------- |
| 화면   | 사용자 관리            |
| 목적   | 사용자 정보 및 이용 이력 관리 |
| 우선순위 | P2                |



| 기능        | Method | API 후보                             | 요청 데이터                | 응답 데이터      |
| --------- | ------ | ---------------------------------- | --------------------- | ----------- |
| 사용자 목록 조회 | GET    | `/api/admin/users`                 | keyword, status, page | users[]     |
| 사용자 상세 조회 | GET    | `/api/admin/users/{userId}`        | userId                | user detail |
| 사용자 상태 변경 | PATCH  | `/api/admin/users/{userId}/status` | userStatus            | updated     |


---

## 4.8 디바이스 관리 화면


| 항목   | 내용                   |
| ---- | -------------------- |
| 화면   | 디바이스 관리              |
| 목적   | 센서 디바이스 상태와 연결 이력 관리 |
| 우선순위 | P1                   |



| 기능          | Method | API 후보                          | 요청 데이터                | 응답 데이터        |
| ----------- | ------ | ------------------------------- | --------------------- | ------------- |
| 디바이스 목록 조회  | GET    | `/api/admin/devices`            | status, keyword, page | devices[]     |
| 디바이스 상세 조회  | GET    | `/api/admin/devices/{deviceId}` | deviceId              | device detail |
| 비정상 디바이스 조회 | GET    | `/api/admin/devices/errors`     | dateRange             | devices[]     |


---

## 4.9 AI 분석 관리 화면


| 항목   | 내용             |
| ---- | -------------- |
| 화면   | AI 분석 관리       |
| 목적   | 위험 구간 분석 결과 확인 |
| 우선순위 | P1             |



| 기능          | Method | API 후보                                | 요청 데이터            | 응답 데이터          |
| ----------- | ------ | ------------------------------------- | ----------------- | --------------- |
| AI 분석 결과 조회 | GET    | `/api/admin/ai/analyses`              | dateRange, page   | analyses[]      |
| 분석 상세 조회    | GET    | `/api/admin/ai/analyses/{analysisId}` | analysisId        | analysis detail |
| 위험 구간 분석 실행 | POST   | `/api/admin/ai/analyze-risk-zones`    | dateRange, region | analysisResult  |


---

## 4.10 리포트 / 통계 화면


| 항목   | 내용                     |
| ---- | ---------------------- |
| 화면   | 리포트 / 통계               |
| 목적   | 기간별, 지역별, 위험 단계별 통계 조회 |
| 우선순위 | P1/P2                  |



| 기능           | Method | API 후보                            | 요청 데이터            | 응답 데이터            |
| ------------ | ------ | --------------------------------- | ----------------- | ----------------- |
| 기간별 통계 조회    | GET    | `/api/admin/reports/events/trend` | dateRange         | eventTrend        |
| 지역별 통계 조회    | GET    | `/api/admin/reports/regions`      | dateRange, region | regionRiskSummary |
| 위험 단계별 통계 조회 | GET    | `/api/admin/reports/risk-levels`  | dateRange         | riskLevelRatio    |
| 리포트 다운로드     | GET    | `/api/admin/reports/download`     | dateRange, format | file              |


---

# 5. MVP 기준 우선 API

## 5.1 사용자용 App P0 API


| 우선순위 | API                              | 목적        |
| ---- | -------------------------------- | --------- |
| P0   | `POST /api/trips/start`          | 주행 시작     |
| P0   | `POST /api/risk-events`          | 위험 이벤트 저장 |
| P0   | `PATCH /api/trips/{tripId}/end`  | 주행 종료     |
| P1   | `GET /api/trips?userId={userId}` | 주행 기록 조회  |
| P1   | `GET /api/trips/{tripId}`        | 주행 상세 조회  |
| P1   | `GET /api/risk-zones`            | 위험 구간 조회  |


---

## 5.2 관리자용 Web P0 API


| 우선순위 | API                                    | 목적        |
| ---- | -------------------------------------- | --------- |
| P0   | `POST /api/admin/auth/login`           | 관리자 로그인   |
| P0   | `GET /api/admin/dashboard/summary`     | 대시보드 요약   |
| P0   | `GET /api/admin/risk-events`           | 위험 이벤트 목록 |
| P0   | `GET /api/admin/risk-events/{eventId}` | 위험 이벤트 상세 |
| P0   | `GET /api/admin/risk-zones/map`        | 위험 구간 지도  |


---

# 6. 백엔드와 협의가 필요한 항목

## 6.1 API URL 구조

현재 초안은 다음 구조를 기준으로 한다.

```
사용자용 App API: /api/...
관리자용 Web API: /api/admin/...

```

백엔드에서 도메인 구조에 따라 변경 가능하다.

---

## 6.2 인증 방식


| 구분    | MVP 처리안                | 확장 처리안              |
| ----- | ---------------------- | ------------------- |
| 사용자 앱 | guest userId 또는 간편 로그인 | JWT 로그인             |
| 관리자 웹 | 관리자 로그인 필수             | JWT + Role 기반 접근 제어 |


---

## 6.3 위험 이벤트 중복 저장 기준

위험 상황이 지속될 경우 이벤트가 너무 많이 저장될 수 있다.

협의 필요 항목:

```
- 몇 초 간격으로 저장할지
- 같은 위치/같은 riskLevel이면 중복으로 볼지
- 앱에서 제한할지, 백엔드에서 제한할지

```

---

## 6.4 GPS 누락 처리

협의 필요 항목:

```
- GPS가 없으면 위험 이벤트 저장을 막을지
- latitude/longitude null 허용할지
- locationAccuracy 필드를 저장할지

```

---

## 6.5 riskType 처리

MVP에서는 센서만으로 위험 유형 구분이 어렵다.

처리안:

```
초기값: OBSTACLE 또는 UNKNOWN
추후 AI/관리자 검토로 CURB, SLOPE 등 확장

```

---

