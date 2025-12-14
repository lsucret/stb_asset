package com.sentbe.wallet.service;

import com.sentbe.wallet.domain.Wallet;
import com.sentbe.wallet.domain.WalletTransaction;
import com.sentbe.wallet.domain.TransactionStatusCode;
import com.sentbe.wallet.exception.WalletNotFoundException;
import com.sentbe.wallet.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
public class WalletService {
    
    private final WalletRepository walletRepository;
    private final WalletTransactionService walletTransactionService;
    
    public WalletService(WalletRepository walletRepository, WalletTransactionService walletTransactionService) {
        this.walletRepository = walletRepository;
        this.walletTransactionService = walletTransactionService;
    }
    
    @Transactional
    public WalletTransaction withdraw(String walletId, String transactionId, BigDecimal amount) {
        // 1. 멱등성 체크 - 기존 트랜잭션 조회
        Optional<WalletTransaction> existingTransaction = 
            walletTransactionService.findByWalletIdAndTransactionId(walletId, transactionId);
        
        if (existingTransaction.isPresent()) {
            log.info("Duplicate transaction detected: walletId={}, transactionId={}", walletId, transactionId);
            return existingTransaction.get();
        }
        
        // 2. DB 제약으로 멱등성 보장 - PROCESSING 상태로 선점 시도
        WalletTransaction transaction = walletTransactionService.createProcessingTransaction(walletId, transactionId, amount);
        
        try {
            // 3. Pessimistic Lock으로 월렛 조회
            WalletTransaction finalTransaction = transaction;
            Wallet wallet = walletRepository.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> {
                    finalTransaction.markAsFailed(TransactionStatusCode.WALLET_NOT_FOUND);
                    return new WalletNotFoundException(walletId);
                });
            
            // 4. 잔액 검증
            if (wallet.getBalance().compareTo(amount) < 0) {
                transaction.markAsFailed(TransactionStatusCode.INSUFFICIENT_BALANCE);
                return transaction; // 예외 대신 실패 트랜잭션 반환
            }
            
            // 5. 출금 처리
            wallet.withdraw(amount);
            walletRepository.save(wallet);
            
            // 6. 성공 상태로 업데이트 (영속성 컨텍스트에서 자동 반영)
            transaction.markAsSuccess(wallet.getBalance());
            return transaction;
            
        } catch (Exception e) {
            // 예상치 못한 예외 발생 시
            log.error("unexpected Error ", e);
            transaction.markAsFailed(TransactionStatusCode.UNKNOWN_ERROR);
            throw e;
        }
    }

}