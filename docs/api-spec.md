# 백엔드 API 초안

## 1. 인증 / 사용자

### POST `/api/auth/signup`

- 회원가입

### POST `/api/auth/login`

- 로그인

### GET `/api/users/me`

- 내 정보 조회

### PATCH `/api/users/me`

- 내 정보 수정

## 2. 유기동물

### GET `/api/animals`

- 유기동물 목록 조회
- 필터
  - `region`
  - `shelterId`
  - `species`
  - `breed`
  - `sex`
  - `neuterStatus`
  - `serviceStatus`
  - `page`
  - `size`

### GET `/api/animals/{animalId}`

- 유기동물 상세 조회

### POST `/api/animals/{animalId}/favorites`

- 관심 등록
- 권한: 회원

### DELETE `/api/animals/{animalId}/favorites`

- 관심 해제
- 권한: 회원

## 3. 보호소

### GET `/api/shelters`

- 보호소 목록 조회

### GET `/api/shelters/{shelterId}`

- 보호소 상세 조회

### PATCH `/api/shelters/{shelterId}`

- 보호소 정보 수정
- 권한: 보호소 담당자, 관리자

## 4. 입양 신청

### POST `/api/adoptions`

- 입양 신청 생성
- 권한: 회원

### GET `/api/adoptions/me`

- 내 입양 신청 목록
- 권한: 회원

### GET `/api/shelter-admin/adoptions`

- 보호소 입양 신청 목록
- 권한: 보호소 담당자

### PATCH `/api/shelter-admin/adoptions/{applicationId}/status`

- 입양 신청 상태 변경
- 권한: 보호소 담당자

## 5. 후원

### POST `/api/donations`

- 후원 생성
- 권한: 회원

### POST `/api/donations/{donationId}/confirm`

- 결제 완료 반영

### GET `/api/donations/me`

- 내 후원 내역 조회
- 권한: 회원

### GET `/api/shelter-admin/donations`

- 보호소 수령 후원 목록
- 권한: 보호소 담당자

### POST `/api/shelter-admin/donation-usages`

- 후원금 사용 내역 등록
- 권한: 보호소 담당자

## 6. 공지

### GET `/api/notices`

- 공지 목록

### GET `/api/notices/{noticeId}`

- 공지 상세

### POST `/api/admin/notices`

- 공지 등록
- 권한: 관리자

## 7. 통계

### GET `/api/stats/summary`

- 전체 요약 통계

### GET `/api/stats/regions`

- 지역별 통계

### GET `/api/stats/monthly`

- 월별 통계

### GET `/api/admin/stats/dashboard`

- 관리자 상세 통계
- 권한: 관리자

## 8. 관리자 운영

### GET `/api/admin/users`

- 회원 목록 조회

### GET `/api/admin/shelters`

- 보호소 목록/승인 대기 조회

### PATCH `/api/admin/shelters/{shelterId}/approve`

- 보호소 승인 처리

### GET `/api/admin/animals/sync-logs`

- 공공데이터 수집 로그 조회

## 9. 응답 공통 규칙

- 목록 API는 `content`, `page`, `size`, `totalElements`, `totalPages` 구조 권장
- 상태 변경 API는 변경 후 최신 리소스 또는 간단한 성공 응답 반환
- 관리자/보호소 권한 API는 JWT 또는 세션 기반 인증 필요
