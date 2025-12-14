package com.sentbe.wallet.dto;

import com.sentbe.wallet.domain.WalletTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WithdrawResponse(
    String walletId,
    String transactionId,
    BigDecimal amount,
    BigDecimal balanceAfter,
    String status,
    LocalDateTime timestamp
) {
    public WithdrawResponse(WalletTransaction transaction) {
        this(
            transaction.getWalletId(),
            transaction.getTransactionId(),
            transaction.getAmount(),
            transaction.getBalanceAfter(),
            transaction.getStatus().name(),
            transaction.getCreatedAt()
        );
    }
}