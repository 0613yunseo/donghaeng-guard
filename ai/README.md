# DonghaengGuard AI 위험구간 자동 갱신

## 1. 기능 개요

최근 7일간의 위험 이벤트를 분석하여 위험구간을 생성하고,
AWS EC2에서 매시간 자동으로 갱신한다.

- WARNING 및 DANGER 이벤트 분석
- 25m 이내 이벤트 연결 기반 공간 군집화
- WARNING 50점, DANGER 100점의 평균으로 위험 점수 산출
- 군집별 대표 좌표, 위험 등급, 이벤트 수 및 위험 사유 생성
- 기존 위험구간과 비교하여 변경된 경우에만 저장
- 분석 결과가 0건이면 기존 위험구간 유지
- 기존 위험구간 조회 실패 시 갱신 중단
- flock을 통한 중복 실행 방지

## 2. 실행 환경

- AWS EC2 Ubuntu
- Python 3
- httpx
- systemd service 및 timer
- Spring Boot 백엔드
- AI 서버 경로: /home/ubuntu/ai-server

## 3. 백엔드 API 연동

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | /internal/ai/data | 위험 이벤트 조회 |
| GET | /api/risk-zones | 기존 위험구간 조회 |
| POST | /internal/ai/result | 위험구간 갱신 |

기존 백엔드 API를 사용하므로 추가 백엔드 코드 수정은 필요하지 않다.
백엔드 주소 변경 시 risk_zone_analyzer.py의 BACKEND_URL을 수정한다.

## 4. 파일 구성

- risk_zone_analyzer.py: 위험구간 분석 및 자동 갱신 모듈
- deploy/systemd/donghaeguard-risk-zone.service: 실행 서비스
- deploy/systemd/donghaeguard-risk-zone.timer: 시간별 실행 설정

## 5. EC2 재배포 방법

아래 명령어는 저장소를 내려받은 EC2의 저장소 루트에서 실행한다.
기존 AI 서버 및 Python 가상환경이 준비되어 있어야 한다.

### 분석 모듈 설치

```bash
cp ai/risk_zone_analyzer.py /home/ubuntu/ai-server/
/home/ubuntu/ai-server/venv/bin/python -m pip install httpx
```

### Dry-run 검증

```bash
cd /home/ubuntu/ai-server
./venv/bin/python risk_zone_analyzer.py
```

기본 실행은 분석 결과만 출력하며 DB를 변경하지 않는다.

### systemd 설정 설치

저장소 루트로 돌아온 후 실행한다.

```bash
sudo cp ai/deploy/systemd/donghaeguard-risk-zone.service /etc/systemd/system/
sudo cp ai/deploy/systemd/donghaeguard-risk-zone.timer /etc/systemd/system/

sudo systemd-analyze verify \
  /etc/systemd/system/donghaeguard-risk-zone.service \
  /etc/systemd/system/donghaeguard-risk-zone.timer

sudo systemctl daemon-reload
```

### 자동 실행 활성화

분석 결과 및 백엔드 연결을 확인한 후 활성화한다.

```bash
sudo systemctl enable --now donghaeguard-risk-zone.timer
```

자동 실행은 매시간 정각을 기준으로 수행된다.
위험구간 변경이 감지되면 기존 위험구간을 교체하므로
운영 DB에 적용하기 전에 분석 결과를 확인해야 한다.

## 6. 운영 상태 확인

```bash
systemctl status donghaeguard-risk-zone.timer
systemctl list-timers donghaeguard-risk-zone.timer
sudo journalctl -u donghaeguard-risk-zone.service --no-pager -n 100
```

oneshot 서비스는 실행 완료 후 inactive 상태가 되는 것이 정상이다.

주요 로그:

- AUTO SUCCESS: 위험구간 저장 완료
- AUTO SKIP: 기존 위험구간과 동일 - POST 생략
- AUTO SKIP: 위험 구간 0개 - 기존 DB 유지

## 7. 자동 실행 중지

```bash
sudo systemctl disable --now donghaeguard-risk-zone.timer
```

이미 실행 중인 분석 작업은 별도로 상태를 확인한다.

## 8. 운영 검증 결과

검증일: 2026년 11월 24일 (KST)

| 항목 | 결과 |
| --- | --- |
| 전체 이벤트 | 3,132건 |
| 위험 이벤트 | 2,844건 |
| 생성된 위험구간 | 2개 |
| 위험 점수 | 69점, 65점 |
| 위험 등급 | MEDIUM, MEDIUM |
| 자동 실행 검증 기간 | 00:00~14:00 |
| 전체 자동 실행 | 15회 |
| 정상 종료 | 15회 |
| 최초 위험구간 저장 | 1회 |
| 동일 결과 갱신 생략 | 14회 |
| 실행 실패 | 0회 |
| 백엔드 위험구간 조회 | HTTP 200 |
| 기존 AI 서버 Health Check | HTTP 200 |

빈 배열 전송 방지 로직은 소스코드에서 확인하였다.
반복 실행 결과는 약 14시간의 관측 결과이며 장기 안정성이나
위험구간 예측 정확도를 의미하지 않는다.

## 9. 보안 및 주의사항

- 분석 결과가 0건일 경우 빈 배열을 백엔드에 전송하지 않는다.
- 기존 위험구간 조회 실패 시 POST 요청을 수행하지 않는다.
- API 키, SSH 개인키, 환경변수 파일 및 가상환경은 GitHub에 포함하지 않는다.
- 기존 AI 예측 및 설명 서비스와 별도로 실행된다.
- 서버 경로 또는 실행 계정이 변경되면 systemd 설정도 수정해야 한다.
