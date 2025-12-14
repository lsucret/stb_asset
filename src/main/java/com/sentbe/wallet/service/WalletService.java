package com.sentbe.wallet.service;

import com.sentbe.wallet.domain.Wallet;
import com.sentbe.wallet.domain.WalletTransaction;
import com.sentbe.wallet.domain.TransactionStatusCode;
import com.sentbe.wallet.exception.WalletNotFoundException;
import com.sentbe.wallet.repository.WalletRepository;
import com.sentbe.wallet.repository.WalletTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
public class WalletService {
    
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    
    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }
    
    @Transactional
    public WalletTransaction withdraw(String walletId, String transactionId, BigDecimal amount) {
        // 1. DB 제약으로 멱등성 보장 - PROCESSING 상태로 선점 시도
        WalletTransaction transaction;
        try {
            transaction = new WalletTransaction(walletId, transactionId, amount);
            transaction = transactionRepository.saveAndFlush(transaction); // 즉시 flush로 UNIQUE 충돌 확정
            log.debug("Transaction processing started: walletId={}, transactionId={}", walletId, transactionId);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE 제약 위반 시만 처리
            Optional<WalletTransaction> existingTransaction = 
                transactionRepository.findByWalletIdAndTransactionId(walletId, transactionId);
            
            if (existingTransaction.isPresent()) {
                log.warn("Duplicate transaction detected: walletId={}, transactionId={}", walletId, transactionId);
                return existingTransaction.get();
            }
            throw e;
        }
        
        try {
            // 2. Pessimistic Lock으로 월렛 조회
            WalletTransaction finalTransaction = transaction;
            Wallet wallet = walletRepository.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> {
                    finalTransaction.markAsFailed(TransactionStatusCode.WALLET_NOT_FOUND);
                    return new WalletNotFoundException(walletId);
                });
            
            // 3. 잔액 검증
            if (wallet.getBalance().compareTo(amount) < 0) {
                transaction.markAsFailed(TransactionStatusCode.INSUFFICIENT_BALANCE);
                return transaction; // 예외 대신 실패 트랜잭션 반환
            }
            
            // 4. 출금 처리
            wallet.withdraw(amount);
            walletRepository.save(wallet);
            
            // 5. 성공 상태로 업데이트 (영속성 컨텍스트에서 자동 반영)
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