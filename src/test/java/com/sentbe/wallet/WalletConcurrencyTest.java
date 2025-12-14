package com.sentbe.wallet;

import com.sentbe.wallet.domain.Wallet;
import com.sentbe.wallet.domain.WalletTransaction;
import com.sentbe.wallet.repository.WalletRepository;
import com.sentbe.wallet.repository.WalletTransactionRepository;
import com.sentbe.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class WalletConcurrencyTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("wallet_test_db")
            .withUsername("test_user")
            .withPassword("test_pass");
    
    @org.springframework.test.context.DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Autowired
    private WalletService walletService;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private WalletTransactionRepository transactionRepository;
    
    private static final String TEST_WALLET_ID = "test-wallet";
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000000.00");
    private static final BigDecimal WITHDRAW_AMOUNT = new BigDecimal("10000.00");
    private static final int THREAD_COUNT = 100;
    
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        
        // 테스트용 월렛 생성
        Wallet testWallet = new Wallet(TEST_WALLET_ID, INITIAL_BALANCE);
        walletRepository.save(testWallet);
        
        // 추가 테스트 데이터 초기화
        com.sentbe.wallet.config.TestDataInitializer.initializeTestData(walletRepository);
    }
    
    @Test
    void 동시에_100개_스레드가_출금_요청시_잔액_무결성_보장() throws InterruptedException {
        // Given
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    String transactionId = "TXN_" + threadIndex + "_" + System.currentTimeMillis();
                    walletService.withdraw(TEST_WALLET_ID, transactionId, WITHDRAW_AMOUNT);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executorService.shutdown();
        
        // Then
        Wallet finalWallet = walletRepository.findById(TEST_WALLET_ID).orElseThrow();
        List<WalletTransaction> transactions = transactionRepository.findAll();
        
        // 성공한 거래 수 검증
        long successfulTransactions = transactions.stream()
            .filter(t -> t.getStatus() == WalletTransaction.TransactionStatus.SUCCESS)
            .count();
        
        // 총 출금액 계산
        BigDecimal totalWithdrawn = transactions.stream()
            .filter(t -> t.getStatus() == WalletTransaction.TransactionStatus.SUCCESS)
            .map(WalletTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 예상 최종 잔액
        BigDecimal expectedBalance = INITIAL_BALANCE.subtract(totalWithdrawn);
        
        System.out.println("=== 동시성 테스트 결과 ===");
        System.out.println("초기 잔액: " + INITIAL_BALANCE);
        System.out.println("성공한 거래 수: " + successfulTransactions);
        System.out.println("실패한 거래 수: " + failureCount.get());
        System.out.println("총 출금액: " + totalWithdrawn);
        System.out.println("최종 잔액: " + finalWallet.getBalance());
        System.out.println("예상 잔액: " + expectedBalance);
        
        // 검증
        assertThat(finalWallet.getBalance()).isEqualTo(expectedBalance);
        assertThat(finalWallet.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(successfulTransactions).isEqualTo(successCount.get());
        assertThat(totalWithdrawn).isEqualTo(WITHDRAW_AMOUNT.multiply(BigDecimal.valueOf(successfulTransactions)));
    }
    
    @Test
    void 멱등성_테스트_동일한_transactionId로_중복_요청시_한번만_처리() throws InterruptedException {
        // Given
        String duplicateTransactionId = "DUPLICATE_TXN_123";
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // When - 동일한 transactionId로 10번 동시 요청
        for (int i = 0; i < 10; i++) {
            executorService.submit(() -> {
                try {
                    walletService.withdraw(TEST_WALLET_ID, duplicateTransactionId, WITHDRAW_AMOUNT);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 예외 무시 (중복 요청으로 인한 예외 발생 가능)
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executorService.shutdown();
        
        // Then
        List<WalletTransaction> transactions = transactionRepository.findAll();
        long duplicateTransactionCount = transactions.stream()
            .filter(t -> duplicateTransactionId.equals(t.getTransactionId()))
            .count();
        
        Wallet finalWallet = walletRepository.findById(TEST_WALLET_ID).orElseThrow();
        BigDecimal expectedBalance = INITIAL_BALANCE.subtract(WITHDRAW_AMOUNT);
        
        System.out.println("=== 멱등성 테스트 결과 ===");
        System.out.println("중복 거래 ID로 생성된 거래 수: " + duplicateTransactionCount);
        System.out.println("최종 잔액: " + finalWallet.getBalance());
        System.out.println("예상 잔액: " + expectedBalance);
        
        // 검증 - 중복 transactionId로는 단 1개의 거래만 생성되어야 함
        assertThat(duplicateTransactionCount).isEqualTo(1);
        assertThat(finalWallet.getBalance()).isEqualTo(expectedBalance);
    }
}