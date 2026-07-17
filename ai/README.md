# ai — FastAPI AI 서버

## 이 폴더의 역할

경로 분석, 위험 예측 등 AI/ML 처리를 수행하고 결과를 **Spring Boot 백엔드**로 반환합니다.
클라이언트(앱·관리자 웹)는 이 서버에 직접 접근하지 않습니다.

## 사용하는 개발 도구

- FastAPI
- Python
- Pandas, scikit-learn
- RandomForest 등 ML 모델
- A* 경로 알고리즘
- Groq API (선택, LLM 연동 시)

## 초기 작업 예정 내용

- FastAPI 앱 구조 및 헬스체크
- 학습·추론 파이프라인 프로토타입
- 백엔드와의 요청/응답 계약 정의

## 관련 문서

- 루트 `README.md`
- `docs/04_API_SPEC.md` (내부 AI 연동 포함), `docs/06_TECHNICAL_GUIDE.md`
