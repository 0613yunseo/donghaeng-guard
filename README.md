# 동행가드 (DongHaeng Guard)

> 전동 휠체어 이용자를 위한 **IoT·AI 기반 주행 위험 감지 서비스**
> 한양여자대학교 빅데이터과 캡스톤 디자인 · Team nulla

센서와 카메라로 주행 중 위험을 실시간으로 확인하고, 주행 기록과 위험구간을 앱과 관리자 웹에서 확인할 수 있는 통합 서비스입니다.

- **관리자 웹**: https://donghaeng-guard-admin.vercel.app/

## 왜 만들었나

전동 휠체어 주행 중에는 눈으로만 판단하기 어려운 위험이 있습니다.

- 앞의 장애물은 가까워진 뒤에야 알아보는 경우가 있습니다.
- 경사는 눈으로 가늠하기 어렵고, 기울기에 따라 주행 안정성이 달라집니다.
- 턱이나 균열 같은 노면은 주행 중 바로 파악하기 어렵습니다.

동행가드는 센서와 카메라로 이 위험을 확인하고, 기록으로 남겨 다음 이동에 활용합니다.

## 주요 기능

| 영역 | 기능 |
|------|------|
| 센서 · 카메라 | 초음파 센서 5채널, 기울기 센서(MPU6050), USB 카메라(YOLO 객체 탐지 · MiDaS 깊이 분석)로 주행 환경 수집 |
| 위험 판단 | 규칙 기반으로 위험 단계를 판단하고, AI가 보조 예측·설명 (AI 점수는 실제 사고 확률이 아님) |
| Android 앱 | 센서 선택, 실시간 위험 알림, 주행 기록, AI 분석 결과, **안전 지도**(위험구간 + Tmap 보행자 경로 위험도) |
| 관리자 웹 | Dashboard, 위험 이벤트·사진, 주행 상세 분석, 위험구간 지도, AI 분석, 리포트 |
| AI 서버 | 위험 예측(RandomForest), 위험 설명(LLM), 최근 7일 주행 데이터 기반 위험구간 자동 갱신 |

## 시스템 구성

```
[ESP32 센서 · USB 카메라] → [Python 수집/영상 분석]
                                   │
                                   v
 [Android 앱]  [관리자 웹]  →  [Spring Boot 백엔드] ⇄ [FastAPI AI 서버]
                                   │
                                   v
                              [MySQL]
```

- 앱과 관리자 웹은 **Spring Boot 백엔드 API만** 호출합니다. (AI 서버에 직접 접근하지 않음)
- 백엔드는 AI 예측·설명을 요청하고, AI 서버는 센서 데이터로 위험구간을 다시 계산해 백엔드에 저장합니다.
- 백엔드·AI 서버는 AWS, 관리자 웹은 Vercel에 배포합니다.

## 폴더 구조

| 경로 | 설명 |
|------|------|
| `app/` | Android (Kotlin, Jetpack Compose) 앱 |
| `admin-web/` | React + TypeScript + Vite 관리자 웹 |
| `src/` | Spring Boot 백엔드 (Java 17, JPA) |
| `ai/` | FastAPI AI 서버 (위험구간 분석) |
| `docs/` | PRD, IA, API 명세, ERD 등 문서 |
| `scripts/` | 배포 보조 스크립트 |

## 기술 스택

| 영역 | 기술 |
|------|------|
| Sensor | ESP32, 초음파 센서, MPU6050, USB 카메라, Python, YOLOv4-tiny, MiDaS |
| Mobile | Kotlin, Jetpack Compose, osmdroid(OpenStreetMap), Tmap 보행자 경로 |
| Admin Web | React, TypeScript, Vite, Leaflet |
| Backend | Spring Boot, Java 17, JPA, MySQL |
| AI | FastAPI, Python, scikit-learn(RandomForest), Groq LLM |

## 팀

| 이름 | 담당 |
|------|------|
| 권지연 | 센서 |
| 원주현 | AI |
| 이혜원 | 백엔드 |
| 최윤서 | 웹 · 앱 |

## 참고

- 안전 점수와 AI 예측 점수는 **실제 사고 발생 확률이 아닌 참고 정보**입니다.
- Tmap 보행자 경로는 참고용이며, **휠체어 통행 가능 여부와 실제 안전을 보장하지 않습니다.**
- API 키와 접속 정보 등 비밀값은 저장소에 커밋하지 않습니다.
- 자세한 설계와 API 명세는 [`docs/`](docs/)를 참고하세요.

## 협업 규칙

- `main`: 배포 가능한 안정 브랜치, `dev`: 통합 개발 브랜치, 기능은 `feature/*`에서 개발 후 `dev`로 PR
- PR 단위는 작게 유지하고, API 변경 시 `docs/` 명세를 함께 갱신합니다.
- 커밋 메시지는 변경 목적이 드러나게 작성합니다.
