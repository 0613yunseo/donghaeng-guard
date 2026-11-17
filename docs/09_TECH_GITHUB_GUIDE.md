# 기술스택 및 GitHub 작업 가이드

## 1. 프로젝트 저장소 구조

동행가드 프로젝트는 하나의 GitHub 저장소 안에서 앱, 관리자 웹, 백엔드, AI, 문서를 함께 관리하는 **monorepo 구조**로 운영한다.

```
donghaeng-guard/
├─ app/          # 사용자용 Android 앱
├─ admin-web/    # 관리자용 React 웹
├─ backend/      # Spring Boot 백엔드
├─ ai/           # FastAPI AI 분석 모듈
├─ docs/         # PRD, IA, API 명세, ERD 등 문서
├─ README.md
└─ .gitignore

```

각 담당자는 본인이 맡은 폴더를 중심으로 작업한다.


| 담당 영역    | 작업 폴더        |
| -------- | ------------ |
| 사용자 앱    | `app/`       |
| 관리자 웹    | `admin-web/` |
| 백엔드      | `backend/`   |
| AI 서버    | `ai/`        |
| 기획/설계 문서 | `docs/`      |


---

## 2. 브랜치 운영 방식

본 프로젝트는 `main`, `dev`, `feature` 브랜치를 구분하여 사용한다.


| 브랜치         | 용도                |
| ----------- | ----------------- |
| `main`      | 최종 안정 버전, 제출용 브랜치 |
| `dev`       | 개발 통합 브랜치         |
| `feature/*` | 각자 기능 개발 브랜치      |


### 기본 원칙

```
main에 직접 push 금지
dev에서 feature 브랜치를 생성
feature 브랜치에서 작업
작업 완료 후 dev로 Pull Request 생성
dev에서 테스트 후 안정화되면 main으로 병합

```

작업 흐름은 다음과 같다.

```
dev
└─ feature/작업명
   └─ 작업 완료 후 PR
      └─ dev 병합
         └─ 최종 안정화 후 main 병합

```

---

## 3. 작업 시작 전 필수 명령어

작업을 시작하기 전에는 항상 `dev` 브랜치를 최신 상태로 맞춘다.

```bash
git checkout dev
git pull origin dev

```

그다음 본인 작업 브랜치를 생성한다.

```bash
git checkout -b feature/작업명

```

예시:

```bash
git checkout -b feature/backend-login
git checkout -b feature/app-sensor
git checkout -b feature/admin-web-dashboard
git checkout -b feature/ai-risk-analysis

```

---

## 4. 브랜치 이름 규칙

브랜치 이름은 아래 규칙을 따른다.

```
feature/파트-작업내용

```

예시:

```
feature/app-sensor
feature/app-trip-record
feature/admin-web-dashboard
feature/admin-web-risk-events
feature/backend-risk-event-api
feature/backend-device-api
feature/ai-risk-analysis
feature/docs-api-update

```

개인 이름을 붙여야 할 경우 다음처럼 작성할 수 있다.

```
feature/admin-web-layout-yunseo
feature/backend-api-hyewon
feature/ai-model-juhyun

```

---

## 5. 작업 후 커밋 및 푸시

작업한 파일을 확인한다.

```bash
git status

```

변경사항을 추가한다.

```bash
git add .

```

커밋한다.

```bash
git commit -m "커밋 메시지"

```

원격 브랜치에 푸시한다.

```bash
git push -u origin feature/브랜치명

```

이미 한 번 푸시한 브랜치라면 다음부터는 아래 명령어만 사용해도 된다.

```bash
git push

```

---

## 6. 커밋 메시지 규칙

커밋 메시지는 아래 형식을 권장한다.

```
타입: 작업 내용

```


| 타입         | 의미         | 예시                                     |
| ---------- | ---------- | -------------------------------------- |
| `feat`     | 새로운 기능 추가  | `feat: add risk event table`           |
| `fix`      | 오류 수정      | `fix: update import path`              |
| `docs`     | 문서 수정      | `docs: update api spec`                |
| `style`    | UI, 스타일 수정 | `style: update dashboard color`        |
| `refactor` | 코드 구조 개선   | `refactor: split dashboard components` |
| `chore`    | 설정, 초기 세팅  | `chore: initialize admin web project`  |


예시:

```bash
git commit -m "feat: add admin dashboard layout"
git commit -m "fix: resolve package import error"
git commit -m "docs: update ERD document"
git commit -m "chore: initialize backend project"

```

---

## 7. Pull Request 규칙

작업이 끝나면 GitHub에서 Pull Request를 생성한다.

PR 방향은 반드시 다음과 같이 설정한다.

```
base: dev
compare: feature/작업브랜치

```

### 올바른 PR 예시

```
feature/admin-web-dashboard → dev
feature/backend-risk-api → dev
feature/app-sensor → dev
feature/ai-risk-analysis → dev

```

### 잘못된 PR 예시

```
feature/작업브랜치 → main
dev → main
main → dev

```

`main`은 최종 제출용 브랜치이므로 일반 개발 작업을 바로 병합하지 않는다.

---

## 8. PR 작성 양식

Pull Request 작성 시 아래 양식을 사용한다.

```markdown
## 작업 내용
- 작업한 내용을 bullet 형식으로 작성

## 확인 내용
- 실행 확인 여부
- 테스트 확인 여부
- 화면 확인 여부

## 참고 사항
- 팀원이 확인해야 할 내용
- 아직 미완성인 부분
- 추후 작업 예정 내용

```

예시:

```markdown
## 작업 내용
- 관리자 웹 대시보드 레이아웃 구현
- 위험 이벤트 테이블 UI 추가
- 동행가드 컬러 시스템 반영
- mock data 기반 화면 구성

## 확인 내용
- npm install 정상 실행
- npm run dev 정상 실행
- 브라우저에서 관리자 웹 화면 확인

## 참고 사항
- 현재 백엔드 API는 연결하지 않고 mock data 사용
- 추후 risk-events API 완성 후 연동 예정

```

---

## 9. 작업 시 주의사항

### 1. main 직접 작업 금지

`main` 브랜치에서는 직접 수정하지 않는다.

```bash
git checkout main

```

상태에서 작업하지 않도록 주의한다.

### 2. 작업 전 dev 최신화

항상 작업 시작 전에 아래 명령어를 실행한다.

```bash
git checkout dev
git pull origin dev

```

### 3. 각자 담당 폴더 중심으로 작업

충돌을 줄이기 위해 본인이 맡은 폴더 중심으로 수정한다.

```
앱 담당: app/
관리자 웹 담당: admin-web/
백엔드 담당: backend/
AI 담당: ai/
문서 담당: docs/

```

공통 파일을 수정해야 할 경우 팀원에게 먼저 공유한다.

공통 파일 예시:

```
README.md
.gitignore
docs/
package 관련 파일
환경설정 파일

```

### 4. node_modules 업로드 금지

`node_modules/`는 GitHub에 올리지 않는다.

필요한 패키지는 `package.json`과 `package-lock.json`으로 관리한다.

### 5. .env 업로드 금지

API Key, DB 비밀번호, 토큰 등이 들어간 `.env` 파일은 절대 GitHub에 올리지 않는다.

예시:

```
.env
.env.local
application-secret.yml

```

필요한 경우 `.env.example` 파일을 만들어 형식만 공유한다.

---

## 10. 충돌 발생 시 처리

Pull Request 또는 merge 중 충돌이 발생하면 혼자 무리해서 해결하지 않고 팀원에게 공유한다.

충돌 가능성이 높은 상황:

```
같은 파일을 여러 명이 수정한 경우
README.md를 동시에 수정한 경우
docs 문서를 동시에 수정한 경우
package.json을 동시에 수정한 경우

```

충돌이 발생하면 다음 정보를 공유한다.

```
1. 충돌 난 브랜치 이름
2. 충돌 난 파일 이름
3. 어떤 작업을 하던 중이었는지
4. 에러 메시지 또는 GitHub 화면 캡처

```

---

## 11. 관리자 웹 실행 방법

관리자 웹은 `admin-web/` 폴더에서 실행한다.

```bash
cd admin-web
npm install
npm run dev

```

브라우저에서 아래 주소로 접속한다.

```
<http://localhost:5173/>

```

필요한 패키지가 추가되었을 경우 다음 명령어로 설치한다.

```bash
npm install

```

특정 패키지를 추가해야 할 경우:

```bash
npm install 패키지명

```

예시:

```bash
npm install lucide-react recharts sonner

```

---

## 12. 현재 관리자 웹 작업 기준

관리자 웹은 현재 다음 기준으로 작업한다.

```
Framework: React
Language: TypeScript
Build Tool: Vite
Data: mock data 우선 사용
API: 백엔드 완성 후 연동
Design: Figma Make 기반
Color System: 동행가드 컬러 시스템 적용

```

관리자 웹 주요 화면:

```
Dashboard
위험 이벤트
위험 구간
주행 기록
디바이스
AI 분석
리포트
설정

```

---

## 13. 백엔드 / AI 연동 기준

앱과 관리자 웹은 FastAPI를 직접 호출하지 않는다.

기본 구조는 다음과 같다.

```
사용자 앱 / 관리자 웹
        ↓
Spring Boot 백엔드
        ↓
DB 저장 및 조회
        ↓
필요 시 FastAPI AI 서버 내부 호출

```

FastAPI AI 서버는 AI 분석 전용 모듈로 사용한다.

예시:

```
앱 → Spring Boot → DB
관리자 웹 → Spring Boot → DB
Spring Boot → FastAPI → AI 분석 결과 반환 → DB 저장

```

---

## 14. 개발 전 팀원 확인 사항

개발 전 아래 내용을 팀원 간에 확정한다.

```
1. 백엔드 DB 구조와 docs/08_ERD.md 일치 여부
2. API 명세서와 실제 백엔드 API 경로 일치 여부
3. 앱에서 전송할 센서 데이터 형식
4. 위험 단계 SAFE / WARNING / DANGER 기준
5. AI 서버 입력값과 반환값
6. 관리자 웹 mock data와 실제 API 응답 구조
7. 각자 담당 폴더 및 브랜치 이름

```

---

## 15. 작업 예시

### 백엔드 작업 예시

```bash
git checkout dev
git pull origin dev
git checkout -b feature/backend-risk-event-api

# backend 폴더에서 작업

git status
git add .
git commit -m "feat: add risk event api"
git push -u origin feature/backend-risk-event-api

```

PR 생성:

```
base: dev
compare: feature/backend-risk-event-api

```

### 관리자 웹 작업 예시

```bash
git checkout dev
git pull origin dev
git checkout -b feature/admin-web-risk-events

# admin-web 폴더에서 작업

cd admin-web
npm install
npm run dev

```

작업 완료 후:

```bash
cd ..
git status
git add .
git commit -m "feat: add risk event page"
git push -u origin feature/admin-web-risk-events

```

PR 생성:

```
base: dev
compare: feature/admin-web-risk-events

```

---

## 16. 핵심 요약

```
main은 최종 제출용
dev는 개발 통합용
각자 feature 브랜치에서 작업
PR은 dev로 올리기
main 직접 push 금지
작업 전 dev pull 필수
담당 폴더 중심으로 작업
충돌 발생 시 팀원에게 공유

```

