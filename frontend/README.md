# Animal Platform Frontend

React 기반 유기동물 입양/후원 플랫폼 프론트엔드입니다.

## 포함된 내용

- Vite + React 기본 구조
- `react-router-dom` 기반 라우팅
- 메인 / 유기동물 / 보호소 / 통계 / 회원 / 보호소관리 / 관리자 화면 초안
- 서비스 방향에 맞춘 카드형 레이아웃과 대시보드 스타일

## 주요 페이지

- `/`
- `/animals`
- `/shelters`
- `/stats`
- `/me`
- `/shelter-admin`
- `/admin`

## 실행 명령

- `npm.cmd ci`
- `npm.cmd run dev`
- `npm.cmd run build`

## 환경 변수

- `.env.example`를 참고해 `.env` 파일을 만들 수 있다.
- `VITE_API_BASE_URL` 기본값은 `http://localhost:8080`이다.
- 소셜 로그인 완료 후 프론트는 `/login/success`, 실패 시 `/login/failure`로 돌아온다.

## 포트원 결제 연결 메모

- 프론트 결제 진입은 `src/lib/portone.js`에 연결되어 있다.
- 결제 준비 요청은 로그인 사용자의 프로필 정보를 우선 사용한다.
- 실제 포트원 SDK 스크립트와 키는 환경 변수 및 백엔드 `/api/payments/portone/config` 응답을 기준으로 연결하면 된다.
- 백엔드는 `/api/payments/prepare`, `/api/payments/confirm`, `/api/payments/portone/config`를 제공한다.
