package com.sentbe.wallet.service;

import com.sentbe.wallet.domain.Wallet;
import com.sentbe.wallet.domain.WalletTransaction;
import com.sentbe.wallet.exception.InsufficientBalanceException;
import com.sentbe.wallet.exception.WalletNotFoundException;
import com.sentbe.wallet.repository.WalletRepository;
import com.sentbe.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

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
        // 1. 멱등성 체크
        Optional<WalletTransaction> existingTransaction = 
            transactionRepository.findByWalletIdAndTransactionId(walletId, transactionId);
        
        if (existingTransaction.isPresent()) {
            return existingTransaction.get();
        }
        
        // 2. Pessimistic Lock으로 월렛 조회
        Wallet wallet = walletRepository.findByWalletIdWithLock(walletId)
            .orElseThrow(() -> new WalletNotFoundException(walletId));
        
        // 3. 잔액 검증 및 출금 처리
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(wallet.getBalance(), amount);
        }
        
        wallet.withdraw(amount);
        walletRepository.save(wallet);
        
        // 4. 트랜잭션 기록 저장
        WalletTransaction transaction = new WalletTransaction(
            walletId, transactionId, amount, wallet.getBalance()
        );
        
        return transactionRepository.save(transaction);
    }
    

}