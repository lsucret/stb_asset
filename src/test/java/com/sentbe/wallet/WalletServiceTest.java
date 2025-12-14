package com.sentbe.wallet;

import com.sentbe.wallet.domain.TransactionStatusCode;
import com.sentbe.wallet.domain.Wallet;
import com.sentbe.wallet.domain.WalletTransaction;
import com.sentbe.wallet.exception.WalletNotFoundException;
import com.sentbe.wallet.repository.WalletRepository;
import com.sentbe.wallet.repository.WalletTransactionRepository;
import com.sentbe.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class WalletServiceTest {
    
    @Autowired
    private WalletService walletService;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private WalletTransactionRepository transactionRepository;
    
    private static final String TEST_WALLET_ID = "test-wallet";
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100000.00");
    
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        
        // 테스트용 월렛 생성
        Wallet testWallet = new Wallet(TEST_WALLET_ID, INITIAL_BALANCE);
        walletRepository.save(testWallet);
    }
    
    @Test
    @DisplayName("정상 출금 - 충분한 잔액이 있을 때 출금 성공")
    void successfulWithdraw() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("30000.00");
        String transactionId = "TXN_SUCCESS_001";
        
        // When
        WalletTransaction result = walletService.withdraw(TEST_WALLET_ID, transactionId, withdrawAmount);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(TransactionStatusCode.SUCCESS);
        assertThat(result.getAmount()).isEqualTo(withdrawAmount);
        assertThat(result.getBalanceAfter()).isEqualTo(new BigDecimal("70000.00"));
        assertThat(result.getWalletId()).isEqualTo(TEST_WALLET_ID);
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        
        // 월렛 잔액 확인
        Wallet wallet = walletRepository.findById(TEST_WALLET_ID).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(new BigDecimal("70000.00"));
    }
    
    @Test
    @DisplayName("잔액 부족 - 출금 금액이 잔액보다 클 때 실패")
    void insufficientBalance() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("150000.00"); // 잔액보다 큰 금액
        String transactionId = "TXN_INSUFFICIENT_001";
        
        // When
        WalletTransaction result = walletService.withdraw(TEST_WALLET_ID, transactionId, withdrawAmount);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(TransactionStatusCode.INSUFFICIENT_BALANCE);
        assertThat(result.getAmount()).isEqualTo(withdrawAmount);
        assertThat(result.getBalanceAfter()).isNull(); // 실패 시 balanceAfter는 null
        
        // 월렛 잔액은 변경되지 않아야 함
        Wallet wallet = walletRepository.findById(TEST_WALLET_ID).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(INITIAL_BALANCE);
    }
    
    @Test
    @DisplayName("존재하지 않는 월렛 - WalletNotFoundException 발생")
    void walletNotFound() {
        // Given
        String nonExistentWalletId = "non-existent-wallet";
        BigDecimal withdrawAmount = new BigDecimal("10000.00");
        String transactionId = "TXN_NOT_FOUND_001";
        
        // When & Then
        assertThatThrownBy(() -> 
            walletService.withdraw(nonExistentWalletId, transactionId, withdrawAmount)
        ).isInstanceOf(WalletNotFoundException.class);
    }
    
    @Test
    @DisplayName("멱등성 - 동일한 transactionId로 두 번 요청시 같은 결과 반환")
    void idempotency() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("20000.00");
        String transactionId = "TXN_IDEMPOTENT_001";
        
        // When - 첫 번째 요청
        WalletTransaction firstResult = walletService.withdraw(TEST_WALLET_ID, transactionId, withdrawAmount);
        
        // When - 두 번째 요청 (동일한 transactionId)
        WalletTransaction secondResult = walletService.withdraw(TEST_WALLET_ID, transactionId, withdrawAmount);
        
        // Then
        assertThat(firstResult.getId()).isEqualTo(secondResult.getId());
        assertThat(firstResult.getStatus()).isEqualTo(secondResult.getStatus());
        assertThat(firstResult.getAmount()).isEqualTo(secondResult.getAmount());
        assertThat(firstResult.getBalanceAfter()).isEqualTo(secondResult.getBalanceAfter());
        
        // 월렛 잔액은 한 번만 차감되어야 함
        Wallet wallet = walletRepository.findById(TEST_WALLET_ID).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(new BigDecimal("80000.00"));
        
        // 트랜잭션 기록은 1개만 있어야 함
        long transactionCount = transactionRepository.findAll().stream()
            .filter(t -> transactionId.equals(t.getTransactionId()))
            .count();
        assertThat(transactionCount).isEqualTo(1);
    }
    
    @Test
    @DisplayName("전액 출금 - 잔액과 동일한 금액 출금시 잔액 0원")
    void withdrawFullBalance() {
        // Given
        String transactionId = "TXN_FULL_001";
        
        // When
        WalletTransaction result = walletService.withdraw(TEST_WALLET_ID, transactionId, INITIAL_BALANCE);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(TransactionStatusCode.SUCCESS);
        assertThat(result.getBalanceAfter().compareTo(BigDecimal.ZERO)).isEqualTo(0);
        
        // 월렛 잔액 확인
        Wallet wallet = walletRepository.findById(TEST_WALLET_ID).orElseThrow();
        assertThat(wallet.getBalance().compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }
    
    @Test
    @DisplayName("소액 출금 - 1원 출금 테스트")
    void withdrawSmallAmount() {
        // Given
        BigDecimal smallAmount = new BigDecimal("1.00");
        String transactionId = "TXN_SMALL_001";
        
        // When
        WalletTransaction result = walletService.withdraw(TEST_WALLET_ID, transactionId, smallAmount);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(TransactionStatusCode.SUCCESS);
        assertThat(result.getAmount()).isEqualTo(smallAmount);
        assertThat(result.getBalanceAfter()).isEqualTo(new BigDecimal("99999.00"));
    }
    
    @Test
    @DisplayName("연속 출금 - 여러 번 출금하여 잔액 변화 확인")
    void multipleWithdraws() {
        // Given
        BigDecimal firstAmount = new BigDecimal("30000.00");
        BigDecimal secondAmount = new BigDecimal("20000.00");
        
        // When
        WalletTransaction first = walletService.withdraw(TEST_WALLET_ID, "TXN_MULTI_001", firstAmount);
        WalletTransaction second = walletService.withdraw(TEST_WALLET_ID, "TXN_MULTI_002", secondAmount);
        
        // Then
        assertThat(first.getStatus()).isEqualTo(TransactionStatusCode.SUCCESS);
        assertThat(first.getBalanceAfter()).isEqualTo(new BigDecimal("70000.00"));
        
        assertThat(second.getStatus()).isEqualTo(TransactionStatusCode.SUCCESS);
        assertThat(second.getBalanceAfter()).isEqualTo(new BigDecimal("50000.00"));
        
        // 최종 월렛 잔액 확인
        Wallet wallet = walletRepository.findById(TEST_WALLET_ID).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(new BigDecimal("50000.00"));
    }
}