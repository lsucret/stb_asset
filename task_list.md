# 월렛 동시 출금 시스템 구현 Task List

## 1. 프로젝트 기본 설정
- [x] build.gradle 의존성 설정 (Spring Boot 3, JPA, MySQL)
- [x] application.yml 설정 (MySQL 연결, JPA 설정)
- [x] docker-compose.yml 작성 (MySQL 컨테이너)

## 2. 도메인 모델 구현
- [x] Wallet 엔티티 작성
- [x] WalletTransaction 엔티티 작성
- [x] 엔티티 간 연관관계 설정

## 3. Repository 계층
- [x] WalletRepository 인터페이스 작성
- [x] Pessimistic Lock 쿼리 메서드 구현
- [x] WalletTransactionRepository 인터페이스 작성

## 4. Service 계층
- [x] WalletService 클래스 작성
- [x] 출금 비즈니스 로직 구현
- [x] 멱등성 체크 로직 구현
- [x] 트랜잭션 처리 (@Transactional)

## 5. Controller 계층
- [x] WalletController 클래스 작성
- [x] 출금 API 엔드포인트 구현
- [x] Request/Response DTO 작성
- [x] 예외 처리 및 HTTP 상태코드 설정

## 6. 예외 처리
- [x] 커스텀 예외 클래스 작성 (잔액부족, 중복요청 등)
- [x] GlobalExceptionHandler 작성
- [x] 적절한 HTTP 응답 코드 매핑

## 7. 데이터베이스 초기화
- [x] 초기 월렛 데이터 삽입 로직 작성
- [x] CommandLineRunner 또는 @PostConstruct 활용

## 8. 동시성 테스트
- [x] 통합 테스트 환경 설정
- [x] 멀티스레드 동시성 테스트 작성
- [x] 100개 스레드 동시 출금 시나리오 구현
- [x] 잔액 무결성 검증 로직 작성

## 9. 단위 테스트
- [ ] Service 계층 단위 테스트
- [ ] Repository 계층 테스트
- [ ] Controller 계층 테스트

## 10. 문서화 및 검증
- [ ] API 테스트 (Postman/curl)
- [ ] 동시성 테스트 결과 정리
- [ ] README.md 최종 업데이트
- [ ] 코드 리뷰 및 리팩토링

## 우선순위
1. **1-3번**: 기본 인프라 설정
2. **4-5번**: 핵심 비즈니스 로직
3. **8번**: 동시성 테스트 (핵심 평가 요소)
4. **나머지**: 완성도 향상