package com.sentbe.wallet.service;

import com.sentbe.wallet.domain.WalletTransaction;
import com.sentbe.wallet.domain.TransactionStatusCode;
import com.sentbe.wallet.repository.WalletTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
public class WalletTransactionService {
    
    private final WalletTransactionRepository transactionRepository;
    
    public WalletTransactionService(WalletTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    @Transactional(readOnly = true)
    public Optional<WalletTransaction> findByWalletIdAndTransactionId(String walletId, String transactionId) {
        return transactionRepository.findByWalletIdAndTransactionId(walletId, transactionId);
    }
    
    @Transactional
    public WalletTransaction createProcessingTransaction(String walletId, String transactionId, BigDecimal amount) {
        try {
            WalletTransaction transaction = new WalletTransaction(walletId, transactionId, amount);
            transaction = transactionRepository.saveAndFlush(transaction);
            log.debug("Transaction processing started: walletId={}, transactionId={}", walletId, transactionId);
            return transaction;
        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition detected, retrying: walletId={}, transactionId={}", walletId, transactionId);
            return findExistingTransaction(walletId, transactionId);
        }
    }
    
    @Transactional(readOnly = true)
    public WalletTransaction findExistingTransaction(String walletId, String transactionId) {
        return transactionRepository.findByWalletIdAndTransactionId(walletId, transactionId)
            .orElseThrow(() -> new RuntimeException("Expected existing transaction not found"));
    }
}