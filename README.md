# 월렛 동시 출금 및 잔액 무결성 보장 시스템

## 프로젝트 개요
다수의 요청이 동일한 월렛에서 동시에 출금을 시도할 때 데이터 무결성을 완벽하게 보장하는 API 구현

## 기술 스택
- **Language**: Java 21
- **Framework**: Spring Boot 3+
- **Database**: MySQL
- **ORM**: Spring Data JPA

## 설계 결정

### 1. 동시성 제어 기법
**Pessimistic Lock (비관적 락) 선택**

**선택 이유**:
- `SELECT FOR UPDATE`를 통한 명시적 행 레벨 락
- 완벽한 데이터 무결성 보장
- 구현 단순성 및 예측 가능성

**구현 방식**:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM Wallet w WHERE w.walletId = :walletId")
Optional<Wallet> findByWalletIdWithLock(@Param("walletId") String walletId);
```

### 2. 멱등성 보장
**DB 제약으로 선점 방식**
- `wallet_id + transaction_id` 복합 유니크 인덱스
- PROCESSING 상태로 선점 INSERT 시도
- UNIQUE 충돌 시 기존 트랜잭션 반환
- 레이스 컨디션 완전 방지

### 3. 상태 머신 및 안전성
**트랜잭션 상태 관리**
- PROCESSING: 처리 중 (선점 성공)
- SUCCESS: 출금 성공
- FAILED: 잔액 부족 등 실패

**장점**:
- 완벽한 데이터 무결성
- 실패 요청도 기록 보존
- 장애 상황에서 유실 방지

**단점**:
- 처리량 제한 (직렬화로 인한 50-100 TPS)
- 락 대기로 인한 응답시간 증가

### 4. 우려사항 및 향후 대책

**현재 우려사항**:
- 단일 월렛에 대한 처리량 제한
- 락 대기 시간으로 인한 사용자 경험 저하

**향후 개선 방안**:
- Redis 분산 락을 통한 선행 필터링
- 읽기 전용 조회 API 분리
- 비동기 처리 도입

## 프로젝트 실행 방법

### 1. 프로젝트 빌드 및 실행
```bash
./gradlew bootRun
```

### 2. 데이터베이스 설정

**개발 환경 (Spring Boot Docker Compose Support)**:
```bash
# 애플리케이션 실행 시 MySQL 자동 시작
./gradlew bootRun
```

**수동 실행 (기존 방식)**:
```bash
# Docker Compose로 MySQL 실행
docker-compose up -d
./gradlew bootRun
```

**테스트 환경 (로컬 MySQL)**:
```bash
# MySQL 컨테이너가 실행 중인 상태에서 테스트 실행
./gradlew test
```

- 애플리케이션 시작 시 테이블 자동 생성
- 초기 월렛 데이터 자동 삽입

### 3. API 테스트
```bash
# 출금 요청
curl -X POST http://localhost:8080/api/wallets/wallet1/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 10000,
    "transactionId": "TXN_UUID_12345"
  }'
```

## 테스트 실행

### 동시성 테스트
```bash
./gradlew test --tests "*ConcurrencyTest"
```

**테스트 시나리오**:
- 100개 스레드가 동시에 10,000원씩 출금 요청
- 최종 잔액이 0원 미만이 되지 않는지 검증
- 총 출금 금액이 초기 잔액을 초과하지 않는지 검증

## API 명세

### 출금 API
- **Endpoint**: `POST /api/wallets/{walletId}/withdraw`
- **Request Body**:
```json
{
  "amount": 10000,
  "transactionId": "TXN_UUID_12345"
}
```
- **Response**: 
  - 성공: 200 OK
  - 잔액 부족: 400 Bad Request
  - 중복 요청: 409 Conflict

## 데이터베이스 스키마

### Wallet 테이블
```sql
CREATE TABLE wallet (
    wallet_id VARCHAR(50) PRIMARY KEY,
    balance DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### WalletTransaction 테이블
```sql
CREATE TABLE wallet_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    balance_after DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE(wallet_id, transaction_id)
);
```