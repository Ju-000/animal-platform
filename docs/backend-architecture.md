# 백엔드 구조 제안

## 추천 기술 방향

- Backend: Spring Boot
- Database: PostgreSQL 또는 MySQL
- Auth: Spring Security + JWT
- Batch: Spring Scheduler 또는 Spring Batch
- Payment: 국내 PG 또는 토스페이먼츠 연동 가능 구조

## 패키지 구조 예시

```text
com.example.animalplatform
├─ auth
├─ user
├─ shelter
├─ animal
├─ adoption
├─ donation
├─ notice
├─ statistics
├─ admin
├─ common
└─ external
   └─ publicapi
```

## 도메인별 책임

### auth

- 로그인
- 회원가입
- 토큰 발급

### user

- 회원 정보
- 마이페이지

### shelter

- 보호소 정보
- 보호소 담당자 권한

### animal

- 유기동물 조회
- 공공데이터 매핑
- 관심 등록

### adoption

- 입양 신청
- 상태 관리

### donation

- 후원 생성
- 결제 확인
- 사용 내역 관리

### statistics

- 집계 조회
- 대시보드 데이터 가공

### admin

- 관리자 전용 운영 기능

### external.publicapi

- `data.go.kr` API 클라이언트
- 응답 DTO
- 동기화 서비스

## 구현 우선순위

1. auth
2. animal / shelter 조회
3. favorites / adoptions
4. donations
5. admin / statistics
