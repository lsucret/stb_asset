package com.sentbe.wallet.domain;

import jakarta.persistence.*;
import jakarta.persistence.Convert;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transaction", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "transaction_id"}),
       indexes = {
           @Index(name = "idx_wallet_id", columnList = "wallet_id"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
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
    
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;
    
    @Convert(converter = TransactionStatusCodeConverter.class)
    @Column(name = "status", nullable = false, length = 2)
    private TransactionStatusCode status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    protected WalletTransaction() {}
    
    public WalletTransaction(String walletId, String transactionId, BigDecimal amount) {
        this.walletId = walletId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = TransactionStatusCode.PROCESSING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(TransactionStatusCode statusCode) {
        this.status = statusCode;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsSuccess(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
        this.status = TransactionStatusCode.SUCCESS;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters
    public Long getId() { return id; }
    public String getWalletId() { return walletId; }
    public String getTransactionId() { return transactionId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public TransactionStatusCode getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    

}