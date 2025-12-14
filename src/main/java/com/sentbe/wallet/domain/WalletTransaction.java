package com.sentbe.wallet.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transaction", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "transaction_id"}))
public class WalletTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "wallet_id", nullable = false, length = 50)
    private String walletId;
    
    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.SUCCESS;
    
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    protected WalletTransaction() {}
    
    public WalletTransaction(String walletId, String transactionId, BigDecimal amount, BigDecimal balanceAfter) {
        this.walletId = walletId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.status = TransactionStatus.SUCCESS;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String errorCode) {
        this.status = TransactionStatus.FAILED;
        this.errorCode = errorCode;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters
    public Long getId() { return id; }
    public String getWalletId() { return walletId; }
    public String getTransactionId() { return transactionId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public TransactionStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    public enum TransactionStatus {
        SUCCESS, FAILED
    }
}