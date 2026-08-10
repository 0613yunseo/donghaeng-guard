# DongHaeng Guard (동행 가드)

노인·취약계층의 안전한 이동과 보호를 위한 통합 플랫폼 모노레포입니다.

## 1. 프로젝트 개요

DongHaeng Guard는 모바일 앱, 관리자 웹, 백엔드 API, AI 분석 서버로 구성된 서비스입니다.
사용자(보호자·피보호자)는 앱을 통해 위치·경로·알림을 확인하고, 운영자는 관리자 웹에서 모니터링 및 설정을 수행합니다.
**모든 클라이언트(앱·관리자 웹)는 Spring Boot 백엔드만 호출하며, FastAPI AI 서버에는 직접 접근하지 않습니다.**

## 2. 서비스 구조

```
[Mobile App]     [Admin Web]
       \           /
        \         /
         v       v
    [Spring Boot Backend]
              |
              v
         [Database]
              ^
              |
    [FastAPI AI Server]
              |
              +-- AI 결과 --> Backend --> DB 저장
              |
       App / Admin은 Backend API로만 조회
```

- **Mobile App / Admin Web**: REST API 호출 대상은 **Spring Boot Backend**만 사용합니다.
- **Spring Boot Backend**: 인증, 비즈니스 로직, DB 영속화, AI 서버 연동(서버 간 통신).
- **Database**: PostgreSQL 또는 MySQL (백엔드 JPA).
- **FastAPI AI Server**: 경로·위험 분석 등 AI 처리 후 결과를 백엔드로 전달합니다.

## 3. 폴더 구조

| 경로 | 설명 |
|------|------|
| `app/` | Android (Kotlin) 모바일 앱 |
| `admin-web/` | React + TypeScript + Vite 관리자 웹 |
| `backend/` | Spring Boot 백엔드 API |
| `ai/` | FastAPI AI 분석 서버 |
| `docs/` | PRD, IA, API 명세, ERD 등 문서 |

## 4. 기술스택

| 영역 | 기술 |
|------|------|
| Mobile | Android Studio, Kotlin |
| Admin Web | React, TypeScript, Vite |
| Backend | Spring Boot, Java 17, JPA, PostgreSQL/MySQL |
| AI | FastAPI, Python, Pandas, scikit-learn, RandomForest, A* 경로, Groq API(선택) |
| Docs | Markdown |

## 5. Git 브랜치 전략

- `main`: 배포 가능한 안정 브랜치
- `dev`: 통합 개발 브랜치
- `feature/project-structure`: 모노레포 구조·문서
- `feature/app`: Android 앱
- `feature/admin-web`: 관리자 웹
- `feature/backend`: Spring Boot 백엔드
- `feature/ai`: FastAPI AI 서버
- `feature/docs`: 설계·명세 문서

기능 개발은 `feature/*`에서 진행 후 `dev`로 PR, 검증 후 `main` 반영을 권장합니다.

## 6. 협업 규칙

- PR 단위는 작게 유지하고, 리뷰 후 머지합니다.
- API 변경 시 `docs/` API 명세를 함께 갱신합니다.
- **`.env` 및 비밀값은 저장소에 커밋하지 않습니다.**
- 클라이언트는 **백엔드 API만** 사용합니다(AI 서버 직접 호출 금지).
- 커밋 메시지는 변경 목적이 드러나게 작성합니다.

## 7. 초기 개발 순서

1. `docs/`: PRD·IA·API·ERD 초안 정리
2. `backend/`: 도메인·DB·기본 API 스켈레톤
3. `ai/`: AI 파이프라인 프로토타입 및 백엔드 연동 계약
4. `app/`, `admin-web/`: 백엔드 API 기준 UI·연동
5. 통합 테스트 및 `dev` → `main` 배포 준비

각 하위 폴더의 `README.md`에서 역할·도구·예정 작업을 확인하세요.
