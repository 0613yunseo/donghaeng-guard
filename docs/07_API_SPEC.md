# API 명세서

### 1.1 응답 형식

모든 API는 아래 형식으로 응답한다.

```json
{
  "success": true,
  "data": { },
  "message": "ok"
}

```

실패 시:

```json
{
  "success": false,
  "data": null,
  "message": "에러 메시지"
}

```

### 1.2 우선순위 기준


| 우선순위 | 의미                    |
| ---- | --------------------- |
| P0   | MVP 구현에 반드시 필요한 API   |
| P1   | MVP 이후 1차 확장에 필요한 API |
| P2   | 후순위 또는 고도화 API        |


### 1.3 거리 기준


| 거리값                    | 위험 단계   |
| ---------------------- | ------- |
| distance_mm > 400      | SAFE    |
| 미정 < distance_mm ≤ 400 | WARNING |
| distance_mm ≤ 미정       | DANGER  |


---

## 2. 사용자 API

### 2.1 간편 시작 (Guest 사용자 생성)


| 항목     | 내용                                 |
| ------ | ---------------------------------- |
| Method | POST                               |
| URL    | /api/users/guest                   |
| 우선순위   | P1                                 |
| 설명     | 로그인 없이 임시 userId 생성. MVP 초기 처리 방식. |


**Request Body**

```json
{
  "deviceInfo": "Android 14 / Samsung Galaxy S23"
}

```

**Response**

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "userStatus": "ACTIVE"
  },
  "message": "ok"
}

```

---

### 2.2 회원가입


| 항목     | 내용               |
| ------ | ---------------- |
| Method | POST             |
| URL    | /api/auth/signup |
| 우선순위   | P2               |
| 설명     | 이메일 기반 회원가입      |


**Request Body**

```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "김민준"
}

```

**Response**

```json
{
  "success": true,
  "data": {
    "userId": 1
  },
  "message": "ok"
}

```

---

### 2.3 로그인


| 항목     | 내용              |
| ------ | --------------- |
| Method | POST            |
| URL    | /api/auth/login |
| 우선순위   | P2              |
| 설명     | 이메일 기반 로그인      |


**Request Body**

```json
{
  "email": "user@example.com",
  "password": "password123"
}

```

**Response**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "userId": 1
  },
  "message": "ok"
}

```

---

## 3. 디바이스 API

### 3.1 디바이스 등록


| 항목     | 내용                        |
| ------ | ------------------------- |
| Method | POST                      |
| URL    | /api/devices              |
| 우선순위   | P1                        |
| 설명     | ESP32 센서 모듈을 사용자와 연결하여 등록 |


**Request Body**

```json
{
  "userId": 1,
  "deviceId": "DG-ESP32-001",
  "deviceName": "동행가드 센서",
  "sensorTypes": ["TOF", "ULTRASONIC"]
}

```

**Response**

```json
{
  "success": true,
  "data": {
    "deviceId": "DG-ESP32-001",
    "deviceStatus": "CONNECTED"
  },
  "message": "ok"
}

```

---

### 3.2 내 디바이스 목록 조회


| 항목     | 내용                           |
| ------ | ---------------------------- |
| Method | GET                          |
| URL    | /api/devices?userId={userId} |
| 우선순위   | P1                           |
| 설명     | 사용자에 등록된 디바이스 목록 조회          |


**Response**

```json
{
  "success": true,
  "data": {
    "devices": [
      {
        "deviceId": "DG-ESP32-001",
        "deviceName": "동행가드 센서",
        "deviceStatus": "CONNECTED",
        "lastConnectedAt": "2026-05-22T14:00:00"
      }
    ]
  },
  "message": "ok"
}

```

---

### 3.3 디바이스 연결 상태 갱신


| 항목     | 내용                             |
| ------ | ------------------------------ |
| Method | PATCH                          |
| URL    | /api/devices/{deviceId}/status |
| 우선순위   | P1                             |
| 설명     | BLE 연결/해제 시 디바이스 상태 업데이트       |


**Request Body**

```json
{
  "deviceStatus": "DISCONNECTED",
  "lastConnectedAt": "2026-05-22T15:00:00"
}

```

**Response**

```json
{
  "success": true,
  "data": {
    "updated": true
  },
  "message": "ok"
}

```

---

## 4. 주행 API

### 4.1 주행 시작


| 항목     | 내용                        |
| ------ | ------------------------- |
| Method | POST                      |
| URL    | /api/trips/start          |
| 우선순위   | P0                        |
| 설명     | 주행 세션을 생성하고 tripId를 발급한다. |


**Request Body**

```json
{
  "userId": 1,
  "deviceId": "DG-ESP32-001",
  "startedAt": "2026-05-22T14:00:00"
}

```

**Response**

```json
{
  "success": true,
  "data": {
    "tripId": 1,
    "status": "STARTED",
    "startedAt": "2026-05-22T14:00:00"
  },
  "message": "ok"
}

```

---

### 4.2 주행 종료


| 항목     | 내용                       |
| ------ | ------------------------ |
| Method | PATCH                    |
| URL    | /api/trips/{tripId}/end  |
| 우선순위   | P0                       |
| 설명     | 주행 세션을 종료하고 요약 정보를 저장한다. |


**Request Body**

```json
{
  "endedAt": "2026-05-22T15:00:00",
  "pendingEventCount": 0
}

```

**Response**

```json
{
  "success": true,
  "data": {
    "tripId": 1,
    "status": "ENDED",
    "eventCount": 5,
    "endedAt": "2026-05-22T15:00:00"
  },
  "message": "ok"
}

```

---

### 4.3 현재 주행 상태 조회


| 항목     | 내용                         |
| ------ | -------------------------- |
| Method | GET                        |
| URL    | /api/trips/{tripId}/status |
| 우선순위   | P0                         |
| 설명     | 현재 주행 세션의 상태와 이벤트 수를 조회한다. |


**Response**

```json
{
  "success": true,
  "data": {
    "tripStatus": "STARTED",
    "eventCount": 3
  },
  "message": "ok"
}

```

---

### 4.4 주행 기록 목록 조회


| 항목     | 내용                         |
| ------ | -------------------------- |
| Method | GET                        |
| URL    | /api/trips?userId={userId} |
| 우선순위   | P1                         |
| 설명     | 사용자의 전체 주행 기록 목록을 조회한다.    |


**Response**

```json
{
  "success": true,
  "data": {
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
  },
  "message": "ok"
}

```

---

### 4.5 주행 상세 조회


| 항목     | 내용                             |
| ------ | ------------------------------ |
| Method | GET                            |
| URL    | /api/trips/{tripId}            |
| 우선순위   | P1                             |
| 설명     | 특정 주행의 상세 정보와 위험 이벤트 목록을 조회한다. |


**Response**

```json
{
  "success": true,
  "data": {
    "tripId": 1,
    "startedAt": "2026-05-22T14:00:00",
    "endedAt": "2026-05-22T15:00:00",
    "status": "ENDED",
    "eventCount": 5,
    "highestRiskLevel": "DANGER",
    "events": [
      {
        "eventId": 101,
        "riskLevel": "DANGER",
        "distanceMm": 180,
        "latitude": 35.1796,
        "longitude": 129.0756,
        "detectedAt": "2026-05-22T14:30:00"
      }
    ]
  },
  "message": "ok"
}

```

---

### 4.6 주행 요약 조회


| 항목     | 내용                          |
| ------ | --------------------------- |
| Method | GET                         |
| URL    | /api/trips/{tripId}/summary |
| 우선순위   | P0                          |
| 설명     | 주행 종료 화면에 표시할 요약 정보를 반환한다.  |


**Response**

```json
{
  "success": true,
  "data": {
    "startedAt": "2026-05-22T14:00:00",
    "endedAt": "2026-05-22T15:00:00",
    "eventCount": 5,
    "highestRiskLevel": "DANGER"
  },
  "message": "ok"
}

```

---

## 5. 위험 이벤트 API

### 5.1 위험 이벤트 저장


| 항목     | 내용                                      |
| ------ | --------------------------------------- |
| Method | POST                                    |
| URL    | /api/risk-events                        |
| 우선순위   | P0                                      |
| 설명     | 위험 이벤트를 GPS 위치와 함께 저장한다. 거리 단위는 mm로 통일. |


**Request Body**

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

**Response**

```json
{
  "success": true,
  "data": {
    "eventId": 101,
    "saved": true
  },
  "message": "ok"
}

```

---

### 5.2 위험 이벤트 일괄 저장 (오프라인 재전송)


| 항목     | 내용                                 |
| ------ | ---------------------------------- |
| Method | POST                               |
| URL    | /api/risk-events/bulk              |
| 우선순위   | P1                                 |
| 설명     | 네트워크 오프라인 중 쌓인 이벤트를 재연결 시 일괄 전송한다. |


**Request Body**

```json
{
  "events": [
    {
      "tripId": 1,
      "deviceId": "DG-ESP32-001",
      "sensorType": "TOF",
      "riskLevel": "DANGER",
      "distanceMm": 180,
      "latitude": 35.1796,
      "longitude": 129.0756,
      "detectedAt": "2026-05-22T14:30:00"
    }
  ]
}

```

**Response**

```json
{
  "success": true,
  "data": {
    "savedCount": 3,
    "failedCount": 0
  },
  "message": "ok"
}

```

---

### 5.3 주행별 위험 이벤트 목록 조회


| 항목     | 내용                              |
| ------ | ------------------------------- |
| Method | GET                             |
| URL    | /api/trips/{tripId}/risk-events |
| 우선순위   | P1                              |
| 설명     | 특정 주행의 위험 이벤트 목록을 조회한다.         |


**Response**

```json
{
  "success": true,
  "data": {
    "events": [
      {
        "eventId": 101,
        "sensorType": "TOF",
        "riskLevel": "DANGER",
        "distanceMm": 180,
        "latitude": 35.1796,
        "longitude": 129.0756,
        "detectedAt": "2026-05-22T14:30:00"
      }
    ]
  },
  "message": "ok"
}

```

---

## 6. 위험 구간 API (AI 연동)

### 6.1 위험 구간 목록 조회


| 항목     | 내용                               |
| ------ | -------------------------------- |
| Method | GET                              |
| URL    | /api/risk-zones                  |
| 우선순위   | P1                               |
| 설명     | AI 분석 결과로 생성된 위험 구간 목록을 앱에 제공한다. |


**Response**

```json
{
  "success": true,
  "data": {
    "zones": [
      {
        "zoneId": 1,
        "latitude": 35.1796,
        "longitude": 129.0756,
        "riskScore": 85,
        "riskGrade": "HIGH",
        "zoneEventCount": 12,
        "reason": "단차 반복 감지 구간",
        "updatedAt": "2026-05-22T00:00:00"
      }
    ]
  },
  "message": "ok"
}

```

---

### 6.2 AI 분석용 데이터 제공 (내부 API)


| 항목     | 내용                                              |
| ------ | ----------------------------------------------- |
| Method | GET                                             |
| URL    | /internal/ai/data                               |
| 우선순위   | P1                                              |
| 설명     | AI 파트가 분석에 필요한 누적 위험 이벤트 데이터를 조회한다. 앱 외부 노출 없음. |


**Response**

```json
{
  "success": true,
  "data": {
    "events": [
      {
        "eventId": 101,
        "sensorType": "TOF",
        "riskLevel": "DANGER",
        "distanceMm": 180,
        "latitude": 35.1796,
        "longitude": 129.0756,
        "detectedAt": "2026-05-22T14:30:00"
      }
    ]
  },
  "message": "ok"
}

```

---

### 6.3 AI 분석 결과 저장 (내부 API)


| 항목     | 내용                                  |
| ------ | ----------------------------------- |
| Method | POST                                |
| URL    | /internal/ai/result                 |
| 우선순위   | P1                                  |
| 설명     | AI 파트가 분석 완료 후 위험 구간 결과를 백엔드에 전달한다. |


**Request Body**

```json
{
  "zones": [
    {
      "latitude": 35.1796,
      "longitude": 129.0756,
      "riskScore": 85,
      "riskGrade": "HIGH",
      "zoneEventCount": 12,
      "reason": "단차 반복 감지 구간"
    }
  ]
}

```

**Response**

```json
{
  "success": true,
  "data": {
    "savedCount": 1
  },
  "message": "ok"
}

```

---

## 7. 에러 코드


| 코드  | 설명                                   |
| --- | ------------------------------------ |
| 400 | 잘못된 요청 (필수값 누락, 형식 오류)               |
| 401 | 인증 실패 (토큰 없음 또는 만료)                  |
| 404 | 리소스를 찾을 수 없음 (tripId, deviceId 등 없음) |
| 409 | 중복 요청 (이미 존재하는 리소스)                  |
| 500 | 서버 내부 오류                             |


