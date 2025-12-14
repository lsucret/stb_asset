package com.sentbe.wallet.repository;

import com.sentbe.wallet.domain.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    
    Optional<WalletTransaction> findByWalletIdAndTransactionId(String walletId, String transactionId);
}