# Animal Platform Backend

Spring Boot 기반의 유기동물 통합 관리형 플랫폼 백엔드입니다.

## 현재 포함된 내용

- 기본 Spring Boot 애플리케이션 실행 구조
- 도메인 패키지 골격
  - `admin`
  - `auth`
  - `animal`
  - `adoption`
  - `common`
  - `donation`
  - `external.publicapi`
  - `shelter`
  - `statistics`
  - `user`
- 샘플 API 엔드포인트
  - `GET /api/admin/dashboard`
  - `GET /api/health`
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
  - `GET /api/animals`
  - `GET /api/shelters`
  - `POST /api/adoptions`
  - `POST /api/donations`
  - `GET /api/stats/summary`
  - `GET /api/users/me`

## 소셜 로그인 환경 변수

- `FRONTEND_BASE_URL`
- `CORS_ALLOWED_ORIGINS`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `KAKAO_CLIENT_ID`
- `KAKAO_CLIENT_SECRET`
- `NAVER_CLIENT_ID`
- `NAVER_CLIENT_SECRET`
- `PORTONE_STORE_ID`
- `PORTONE_CHANNEL_KEY`
- `PORTONE_API_SECRET`
- `PAYMENT_PREPARE_TTL_MINUTES`
- `STORAGE_MAX_FILE_SIZE_BYTES`

## 실행 및 검증

- Windows: `.\mvnw.cmd spring-boot:run`
- macOS/Linux: `./mvnw spring-boot:run`
- 테스트: `.\mvnw.cmd test`
- 테스트는 `src/test/resources/application.properties`의 H2 기반 `test` profile로 실행되어 로컬 `.env`의 MySQL 설정에 의존하지 않습니다.
- 운영 프로필에서는 Swagger UI와 OpenAPI 문서가 비활성화됩니다.
