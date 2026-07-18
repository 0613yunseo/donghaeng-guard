# 기술스택 및 GitHub 작업 가이드

## 0. 문서 정보


| 항목    | 내용                                             |
| ----- | ---------------------------------------------- |
| 문서명   | 동행가드 기술스택 및 GitHub 작업 가이드                      |
| 프로젝트명 | 동행가드 DongHaeng Guard                           |
| 작성 목적 | 앱·관리자 웹·백엔드·AI 파트의 기술스택과 GitHub 협업 방식을 정의      |
| 기준 문서 | PRD, IA, 화면별 기능 정의서, 화면별 데이터 정의서, API 명세서, ERD |
| 대상    | 앱 개발, 관리자 웹 개발, 백엔드 개발, AI 분석 파트               |
| 저장 위치 | `/docs/TECH_GITHUB_GUIDE.md`                   |


---

# 1. 프로젝트 개발 방향

동행가드는 전동 휠체어 이용자의 이동 중 위험 요소를 센서로 감지하고, 사용자 앱과 관리자 웹을 통해 위험 상태와 위험 구간 데이터를 확인하는 서비스이다.

서비스는 다음 4개 파트로 나누어 개발한다.


| 파트    | 역할                                        |
| ----- | ----------------------------------------- |
| 사용자 앱 | 센서 연결, 실시간 위험 상태 표시, 위험 알림, 주행 기록 확인      |
| 관리자 웹 | 위험 이벤트 조회, 위험 구간 지도, 디바이스 상태, AI 분석 결과 관리 |
| 백엔드   | 사용자, 디바이스, 주행, 위험 이벤트, 위험 구간 데이터 관리       |
| AI    | 누적 위험 이벤트를 기반으로 위험 구간 점수화 및 등급 산출         |


---

# 2. 최종 기술스택

## 2.1 사용자 앱


| 항목     | 기술                                 |
| ------ | ---------------------------------- |
| 개발 도구  | Android Studio                     |
| 언어     | Kotlin                             |
| UI     | XML Layout 또는 Jetpack Compose      |
| 주요 기능  | BLE 연결, GPS 위치 수집, 진동/알림, 위험 상태 표시 |
| API 통신 | Retrofit                           |
| 로컬 저장  | SharedPreferences 또는 Room          |
| 권한 처리  | Android Permission API             |


### 선택 이유

동행가드 사용자 앱은 단순 화면 앱이 아니라 BLE 센서 연결, GPS 위치 수집, 위험 알림, 진동/소리 등 네이티브 기능이 중요하다. 따라서 React Native보다 Android Studio 기반 Kotlin 개발이 안정적이다.

---

## 2.2 관리자 웹


| 항목     | 기술                             |
| ------ | ------------------------------ |
| 개발 도구  | VSCode                         |
| 프레임워크  | React                          |
| 언어     | TypeScript                     |
| 빌드 도구  | Vite                           |
| 스타일    | Tailwind CSS 또는 CSS Modules    |
| 차트     | Recharts                       |
| 지도     | Kakao Map API 또는 Naver Map API |
| API 통신 | Axios                          |


### 선택 이유

관리자 웹은 대시보드, 테이블, 필터, 지도, 차트 중심의 화면이므로 React 기반 웹 개발이 적합하다. Figma Make에서 생성한 관리자 웹 코드는 `admin-web` 폴더에 반영한다.

---

## 2.3 백엔드


| 항목      | 기술                          |
| ------- | --------------------------- |
| 개발 도구   | IntelliJ IDEA               |
| 프레임워크   | Spring Boot                 |
| 언어      | Java 17                     |
| DB 연동   | Spring Data JPA             |
| DB      | MySQL                       |
| API 문서화 | Swagger / Springdoc OpenAPI |
| 인증      | MVP: 간편 userId / 확장: JWT    |
| 빌드 도구   | Gradle                      |


### 선택 이유

동행가드 백엔드는 사용자, 디바이스, 주행 세션, 위험 이벤트, 위험 구간 데이터를 안정적으로 관리해야 한다. 관리자 웹과 앱이 함께 사용하는 API 서버이므로 Spring Boot 기반 구조가 적합하다.

---

## 2.4 AI 분석 모듈


| 항목      | 기술                        |
| ------- | ------------------------- |
| 개발 도구   | VSCode                    |
| 프레임워크   | FastAPI                   |
| 언어      | Python                    |
| 데이터 처리  | Pandas                    |
| 분석 방식   | MVP: 규칙 기반 분석 / 확장: ML 모델 |
| API 문서화 | FastAPI Swagger UI        |


### 선택 이유

AI 파트는 누적된 `risk_events` 데이터를 기반으로 위험 구간을 분석하고, `riskScore`, `riskGrade`, `reason`을 생성하는 역할이다. Python 기반 분석이 필요하므로 FastAPI를 별도 AI 모듈로 구성한다.

---

# 3. GitHub 저장소 구조

동행가드는 GitHub 저장소를 파트별로 여러 개 만들지 않고, 하나의 저장소 안에서 폴더를 나누는 방식으로 관리한다.

## 3.1 저장소명

```
donghaeng-guard

```

## 3.2 최상위 폴더 구조

```
donghaeng-guard/
├─ app/                  # 사용자용 Android 앱
├─ admin-web/             # 관리자용 React 웹
├─ backend/               # Spring Boot 백엔드
├─ ai/                    # FastAPI AI 분석 모듈
├─ docs/                  # 기획/설계 문서
├─ README.md
└─ .gitignore

```

---

# 4. 파트별 작업 도구


| 파트    | 작업 폴더        | 사용 도구                    |
| ----- | ------------ | ------------------------ |
| 사용자 앱 | `/app`       | Android Studio           |
| 관리자 웹 | `/admin-web` | VSCode                   |
| 백엔드   | `/backend`   | IntelliJ IDEA 또는 VSCode  |
| AI    | `/ai`        | VSCode                   |
| 문서    | `/docs`      | Notion, Markdown, GitHub |


각 팀원은 GitHub 저장소 전체를 clone하되, 실제 작업은 본인 파트 폴더만 열어서 진행한다.

---

# 5. docs 폴더 구성

```
docs/
├─ 01_PRD.md
├─ 02_IA_APP.md
├─ 03_IA_ADMIN_WEB.md
├─ 04_SCREEN_FUNCTION_DEFINITION.md
├─ 05_SCREEN_DATA_DEFINITION.md
├─ 06_API_MAPPING.md
├─ 07_API_SPEC.md
├─ 08_ERD.md
├─ 09_COLOR_SYSTEM.md
└─ 10_PROTOTYPE_LINKS.md

```

문서 흐름은 다음 순서로 관리한다.

```
PRD
→ IA
→ 화면별 기능 정의서
→ 화면별 데이터 정의서
→ API Mapping
→ API 명세서
→ ERD
→ 구현

```

---

# 6. 사용자 앱 폴더 구조

```
app/
├─ app/
│  └─ src/
│     └─ main/
│        ├─ java/com/donghaengguard/
│        │  ├─ ui/
│        │  │  ├─ onboarding/
│        │  │  ├─ sensor/
│        │  │  ├─ driving/
│        │  │  ├─ records/
│        │  │  ├─ riskzone/
│        │  │  └─ settings/
│        │  │
│        │  ├─ data/
│        │  │  ├─ api/
│        │  │  ├─ model/
│        │  │  ├─ repository/
│        │  │  └─ local/
│        │  │
│        │  ├─ ble/
│        │  ├─ location/
│        │  ├─ notification/
│        │  ├─ common/
│        │  └─ MainActivity.kt
│        │
│        ├─ res/
│        │  ├─ layout/
│        │  ├─ drawable/
│        │  ├─ values/
│        │  └─ mipmap/
│        │
│        └─ AndroidManifest.xml
│
├─ build.gradle
└─ README.md

```

## 사용자 앱 주요 화면


| 폴더         | 화면                           |
| ---------- | ---------------------------- |
| onboarding | 온보딩 / 권한 안내                  |
| sensor     | 센서 연결 화면                     |
| driving    | 메인 주행 화면, 위험 알림 화면, 주행 종료 화면 |
| records    | 주행 기록 목록, 주행 상세              |
| riskzone   | 위험 구간 지도, 위험 구간 상세           |
| settings   | 디바이스, 알림, 접근성 설정             |


---

# 7. 관리자 웹 폴더 구조

```
admin-web/
├─ src/
│  ├─ pages/
│  │  ├─ login/
│  │  ├─ dashboard/
│  │  ├─ risk-events/
│  │  ├─ risk-zones/
│  │  ├─ trips/
│  │  ├─ devices/
│  │  ├─ ai/
│  │  ├─ reports/
│  │  └─ settings/
│  │
│  ├─ components/
│  │  ├─ layout/
│  │  ├─ cards/
│  │  ├─ tables/
│  │  ├─ badges/
│  │  ├─ charts/
│  │  └─ map/
│  │
│  ├─ services/
│  │  └─ api/
│  │
│  ├─ types/
│  ├─ constants/
│  ├─ mocks/
│  ├─ App.tsx
│  └─ main.tsx
│
├─ package.json
└─ README.md

```

## 관리자 웹 주요 화면


| 폴더          | 화면            |
| ----------- | ------------- |
| dashboard   | 전체 위험 현황 대시보드 |
| risk-events | 위험 이벤트 목록/상세  |
| risk-zones  | 위험 구간 지도/상세   |
| trips       | 주행 기록 관리      |
| devices     | 디바이스 관리       |
| ai          | AI 분석 결과      |
| reports     | 리포트/통계        |
| settings    | 관리자 설정        |


---

# 8. 백엔드 폴더 구조

```
backend/
├─ src/
│  └─ main/
│     ├─ java/com/donghaengguard/
│     │  ├─ DonghaengGuardApplication.java
│     │  │
│     │  ├─ global/
│     │  │  ├─ config/
│     │  │  ├─ exception/
│     │  │  ├─ response/
│     │  │  └─ security/
│     │  │
│     │  ├─ common/
│     │  │  ├─ enums/
│     │  │  └─ constants/
│     │  │
│     │  └─ domain/
│     │     ├─ user/
│     │     ├─ admin/
│     │     ├─ device/
│     │     ├─ trip/
│     │     ├─ riskevent/
│     │     ├─ riskzone/
│     │     └─ ai/
│     │
│     └─ resources/
│        ├─ application.yml
│        ├─ application-local.yml
│        └─ db/
│           ├─ schema.sql
│           └─ data.sql
│
├─ build.gradle
└─ README.md

```

## 백엔드 도메인 구조

각 도메인은 아래 구조를 따른다.

```
controller/
service/
repository/
dto/
entity/

```

예시:

```
domain/riskevent/
├─ controller/
│  └─ RiskEventController.java
├─ service/
│  └─ RiskEventService.java
├─ repository/
│  └─ RiskEventRepository.java
├─ dto/
│  ├─ RiskEventCreateRequest.java
│  ├─ RiskEventResponse.java
│  └─ RiskEventListResponse.java
└─ entity/
   └─ RiskEvent.java

```

---

# 9. AI 폴더 구조

```
ai/
├─ app/
│  ├─ main.py
│  ├─ routers/
│  │  └─ risk_zone_router.py
│  ├─ schemas/
│  │  ├─ risk_event_schema.py
│  │  └─ risk_zone_schema.py
│  ├─ services/
│  │  └─ risk_zone_service.py
│  ├─ models/
│  └─ utils/
│
├─ requirements.txt
└─ README.md

```

## AI 처리 흐름

```
백엔드에서 risk_events 데이터 제공
→ AI 모듈이 위험 구간 분석
→ riskScore, riskGrade, reason 생성
→ 백엔드에 risk_zones 결과 전달
→ 앱/관리자 웹에서 위험 구간 조회

```

---

# 10. Git 브랜치 전략

## 10.1 기본 브랜치


| 브랜치       | 역할                 |
| --------- | ------------------ |
| main      | 최종 제출용 안정 버전       |
| dev       | 개발 통합 브랜치          |
| feature/* | 각 기능 또는 파트별 작업 브랜치 |


## 10.2 추천 브랜치 구조

```
main
└─ dev
   ├─ feature/docs-init
   ├─ feature/project-structure
   ├─ feature/app-init
   ├─ feature/admin-web-init
   ├─ feature/backend-init
   ├─ feature/ai-init
   ├─ feature/api-trip
   ├─ feature/api-risk-event
   ├─ feature/api-risk-zone
   └─ feature/integration

```

---

# 11. GitHub 작업 순서

## 11.1 최초 세팅

```bash
git clone 저장소주소
cd donghaeng-guard

git checkout -b dev
git push origin dev

git checkout -b feature/project-structure

```

폴더 생성:

```bash
mkdir app admin-web backend ai docs
touch README.md
touch .gitignore

```

커밋:

```bash
git add .
git commit -m "chore: initialize project structure"
git push origin feature/project-structure

```

GitHub에서 Pull Request 생성:

```
feature/project-structure → dev

```

---

# 12. 파트별 작업 방식

## 12.1 앱 개발자

```bash
git checkout dev
git pull origin dev
git checkout -b feature/app-init

```

작업 폴더:

```
/app

```

작업 내용:

```
- Android Studio 프로젝트 생성
- 권한 안내 화면
- 센서 연결 화면
- 메인 주행 화면
- 위험 알림 화면
- mock 데이터 기반 주행 기록 화면

```

---

## 12.2 관리자 웹 개발자

```bash
git checkout dev
git pull origin dev
git checkout -b feature/admin-web-init

```

작업 폴더:

```
/admin-web

```

작업 내용:

```
- React + TypeScript + Vite 프로젝트 생성
- Figma Make 코드 반영
- 대시보드 화면 구성
- 위험 이벤트 목록/상세 화면 구성
- 위험 구간 지도 화면 구성
- mock 데이터 연결

```

---

## 12.3 백엔드 개발자

```bash
git checkout dev
git pull origin dev
git checkout -b feature/backend-init

```

작업 폴더:

```
/backend

```

작업 내용:

```
- Spring Boot 프로젝트 생성
- Entity 생성
- Repository 생성
- 공통 응답 형식 생성
- P0 API 더미 응답 구현
- DB 연결
- 실제 저장 로직 구현

```

---

## 12.4 AI 개발자

```bash
git checkout dev
git pull origin dev
git checkout -b feature/ai-init

```

작업 폴더:

```
/ai

```

작업 내용:

```
- FastAPI 프로젝트 생성
- 위험 이벤트 입력 스키마 정의
- 위험 구간 분석 로직 작성
- riskScore, riskGrade 산출
- 분석 결과 응답 형식 정의

```

---

# 13. 합치는 순서

```
1. feature/docs-init → dev
2. feature/project-structure → dev
3. feature/app-init → dev
4. feature/admin-web-init → dev
5. feature/backend-init → dev
6. feature/ai-init → dev
7. feature/api-trip → dev
8. feature/api-risk-event → dev
9. feature/api-risk-zone → dev
10. feature/integration → dev
11. dev 테스트 완료 후 dev → main

```

`main`에는 직접 작업하지 않는다.

최종 발표나 제출 가능한 안정 버전만 `main`에 반영한다.

---

# 14. 프론트-백엔드 협업 기준

## 14.1 반드시 맞출 값


| 항목        | 기준                               |
| --------- | -------------------------------- |
| API 응답 형식 | `success`, `data`, `message`     |
| 위험 단계     | SAFE / WARNING / DANGER          |
| 위험 등급     | LOW / MEDIUM / HIGH              |
| 센서 종류     | TOF / ULTRASONIC                 |
| 디바이스 상태   | CONNECTED / DISCONNECTED / ERROR |
| 주행 상태     | STARTED / ENDED                  |
| 거리 단위     | mm                               |
| 날짜 형식     | ISO 형식                           |
| 좌표        | latitude / longitude             |


---

## 14.2 프론트 작업 방식

프론트는 백엔드 API가 완성되기 전까지 mock 데이터를 사용한다.

```
mocks
→ services
→ pages

```

화면 안에 더미 데이터를 직접 작성하지 않고, 반드시 `mocks` 또는 `services`에서 불러오는 구조로 만든다.

---

## 14.3 백엔드 작업 방식

백엔드는 처음부터 모든 DB 로직을 완성하지 않고, P0 API부터 더미 응답을 제공한다.

```
더미 응답
→ DB 연결
→ 실제 저장 로직
→ 예외 처리
→ Swagger 문서화

```

---

# 15. P0 우선 구현 API

## 사용자 앱 API

```
POST /api/trips/start
PATCH /api/trips/{tripId}/end
GET /api/trips/{tripId}/status
GET /api/trips/{tripId}/summary
POST /api/risk-events

```

## 관리자 웹 API

```
POST /api/admin/auth/login
GET /api/admin/dashboard/summary
GET /api/admin/risk-events
GET /api/admin/risk-events/{eventId}
GET /api/admin/risk-zones/map

```

## AI 연동 API

```
GET /internal/ai/data
POST /internal/ai/result

```

---

# 16. 커밋 메시지 규칙


| 타입       | 의미        | 예시                                   |
| -------- | --------- | ------------------------------------ |
| feat     | 기능 추가     | feat: add risk event list page       |
| fix      | 오류 수정     | fix: correct risk level badge color  |
| docs     | 문서 수정     | docs: update API specification       |
| chore    | 설정/구조 작업  | chore: initialize backend structure  |
| refactor | 코드 구조 개선  | refactor: separate api service logic |
| style    | UI/CSS 수정 | style: update dashboard card layout  |
| test     | 테스트 코드    | test: add risk event api test        |


---

# 17. Pull Request 규칙

PR 제목 예시:

```
[APP] 센서 연결 화면 초기 구현
[ADMIN] 위험 이벤트 목록 화면 구현
[BE] 주행 시작 API 구현
[AI] 위험 구간 분석 로직 초안
[DOCS] API 명세서 업데이트

```

PR에는 아래 내용을 작성한다.

```
## 작업 내용
-

## 확인한 내용
-

## 관련 문서
-

## 다음 작업
-

```

---

# 18. 최종 개발 흐름

```
기획 문서 정리
→ GitHub 저장소 생성
→ app / admin-web / backend / ai / docs 폴더 분리
→ 각 파트별 초기 프로젝트 생성
→ mock 데이터 기반 화면 구현
→ P0 API 더미 응답 연결
→ DB 저장 로직 구현
→ AI 분석 결과 연동
→ 앱/웹 통합 테스트
→ dev 안정화
→ main 최종 반영

```

---

# 19. 최종 요약

동행가드는 하나의 GitHub 저장소에서 관리하되, 파트별로 폴더를 분리한다.

```
app = Android Studio Kotlin 사용자 앱
admin-web = React TypeScript 관리자 웹
backend = Spring Boot API 서버
ai = FastAPI 위험 구간 분석 모듈
docs = PRD, IA, API 명세, ERD 문서

```

작업은 각자 feature 브랜치에서 진행하고, `dev`에서 통합 후 최종 안정 버전만 `main`에 반영한다.