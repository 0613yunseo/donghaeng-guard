# ERD

## 1. 전체 테이블 구성


| 테이블명        | 설명                | 관련 API                                               |
| ----------- | ----------------- | ---------------------------------------------------- |
| users       | 사용자 정보 관리         | POST /api/users/guest, POST /api/auth/login          |
| devices     | ESP32 센서 모듈 정보 관리 | POST /api/devices, GET /api/devices                  |
| trips       | 주행 세션 관리          | POST /api/trips/start, PATCH /api/trips/{tripId}/end |
| risk_events | 위험 이벤트 저장         | POST /api/risk-events                                |
| risk_zones  | AI 분석 결과 저장       | GET /api/risk-zones                                  |


---

## 2. 테이블 정의

### 2.1 users

사용자 정보를 저장하는 테이블. MVP에서는 간편 시작(guest userId) 방식으로 처리 가능.


| 컬럼명         | 타입           | 제약                 | 설명                |
| ----------- | ------------ | ------------------ | ----------------- |
| user_id     | BIGINT       | PK, AUTO_INCREMENT | 사용자 식별값           |
| nickname    | VARCHAR(50)  | NULL 가능            | 사용자 표시명           |
| email       | VARCHAR(100) | NOT NULL           | 이메일 (로그인 시 사용)    |
| password    | VARCHAR(20)  | NOT NULL           | 비밀번호 (로그인 시 사용)   |
| user_status | ENUM         | NOT NULL           | ACTIVE / INACTIVE |
| created_at  | DATETIME     | NOT NULL           | 생성 시간             |
| updated_at  | DATETIME     | NOT NULL           | 수정 시간             |


---

### 2.2 devices

ESP32 센서 모듈 정보를 저장하는 테이블. BLE 연결/해제 상태 및 마지막 연결 시간 관리.


| 컬럼명               | 타입           | 제약                 | 설명                               |
| ----------------- | ------------ | ------------------ | -------------------------------- |
| device_id         | VARCHAR(50)  | PK                 | 센서 모듈 식별값 (예: DG-ESP32-001)      |
| user_id           | BIGINT       | FK → users.user_id | 연결된 사용자                          |
| device_name       | VARCHAR(50)  | NULL 가능            | 디바이스 표시명                         |
| sensor_types      | VARCHAR(100) | NULL 가능            | TOF, ULTRASONIC (쉼표 구분)          |
| device_status     | ENUM         | NOT NULL           | CONNECTED / DISCONNECTED / ERROR |
| last_connected_at | DATETIME     | NULL 가능            | 마지막 연결 시간                        |
| created_at        | DATETIME     | NOT NULL           | 등록 시간                            |


---

### 2.3 trips

주행 세션을 저장하는 테이블. 주행 시작 시 생성되며 종료 시 ended_at 업데이트.


| 컬럼명                | 타입          | 제약                     | 설명                                      |
| ------------------ | ----------- | ---------------------- | --------------------------------------- |
| trip_id            | BIGINT      | PK, AUTO_INCREMENT     | 주행 세션 식별값                               |
| user_id            | BIGINT      | FK → users.user_id     | 주행한 사용자                                 |
| device_id          | VARCHAR(50) | FK → devices.device_id | 사용된 센서 모듈                               |
| trip_status        | ENUM        | NOT NULL               | STARTED / ENDED                         |
| started_at         | DATETIME    | NOT NULL               | 주행 시작 시간                                |
| ended_at           | DATETIME    | NULL 가능                | 주행 종료 시간                                |
| event_count        | INT         | DEFAULT 0              | 위험 이벤트 발생 횟수                            |
| highest_risk_level | ENUM        | NULL 가능                | 주행 중 최고 위험 단계 (SAFE / WARNING / DANGER) |


---

### 2.4 risk_events

위험 이벤트를 저장하는 테이블. 앱에서 GPS 위치와 함께 전송하며 거리 단위는 mm로 통일.


| 컬럼명         | 타입          | 제약                     | 설명                      |
| ----------- | ----------- | ---------------------- | ----------------------- |
| event_id    | BIGINT      | PK, AUTO_INCREMENT     | 위험 이벤트 식별값              |
| trip_id     | BIGINT      | FK → trips.trip_id     | 해당 주행 세션                |
| device_id   | VARCHAR(50) | FK → devices.device_id | 감지한 센서 모듈               |
| sensor_type | ENUM        | NOT NULL               | TOF / ULTRASONIC        |
| risk_type   | VARCHAR(30) | NULL 가능                | OBSTACLE / STEP 등       |
| risk_level  | ENUM        | NOT NULL               | SAFE / WARNING / DANGER |
| distance_mm | INT         | NOT NULL               | 감지 거리 (단위: mm)          |
| latitude    | DOUBLE      | NOT NULL               | 위도                      |
| longitude   | DOUBLE      | NOT NULL               | 경도                      |
| detected_at | DATETIME    | NOT NULL               | 위험 감지 시간                |


거리 기준: distance_mm > 400 → SAFE / 미정 < distance_mm ≤ 400 → WARNING / distance_mm ≤ 미정 → DANGER

---

### 2.5 risk_zones

AI 분석 모듈이 생성한 위험 구간 결과를 저장하는 테이블. 백엔드는 AI로부터 결과를 수신하여 저장하고 앱에 제공.


| 컬럼명              | 타입       | 제약                 | 설명                  |
| ---------------- | -------- | ------------------ | ------------------- |
| zone_id          | BIGINT   | PK, AUTO_INCREMENT | 위험 구간 식별값           |
| latitude         | DOUBLE   | NOT NULL           | 위험 구간 중심 위도         |
| longitude        | DOUBLE   | NOT NULL           | 위험 구간 중심 경도         |
| risk_score       | INT      | NOT NULL           | 위험도 점수 (0~100)      |
| risk_grade       | ENUM     | NOT NULL           | LOW / MEDIUM / HIGH |
| zone_event_count | INT      | DEFAULT 0          | 해당 구간 이벤트 누적 수      |
| reason           | TEXT     | NULL 가능            | 위험 구간 분석 이유         |
| updated_at       | DATETIME | NOT NULL           | 마지막 갱신 시간           |


---

## 3. 테이블 관계 (ERD 관계선)


| 관계                    | 설명                            |
| --------------------- | ----------------------------- |
| users → devices       | 1:N (한 사용자가 여러 디바이스 등록 가능)    |
| users → trips         | 1:N (한 사용자가 여러 주행 세션 가질 수 있음) |
| devices → trips       | 1:N (한 디바이스로 여러 주행 가능)        |
| trips → risk_events   | 1:N (한 주행에서 여러 위험 이벤트 발생 가능)  |
| devices → risk_events | 1:N (한 디바이스가 여러 위험 이벤트 감지 가능) |


