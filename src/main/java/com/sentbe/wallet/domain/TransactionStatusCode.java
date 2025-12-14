package com.sentbe.wallet.domain;

import lombok.Getter;

@Getter
public enum TransactionStatusCode {
    PROCESSING("00"),
    SUCCESS("10"),
    INSUFFICIENT_BALANCE("20"),
    WALLET_NOT_FOUND("21"),
    UNKNOWN_ERROR("99");
    
    private final String code;
    
    TransactionStatusCode(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    public boolean isFailed() {
        return this == INSUFFICIENT_BALANCE || this == WALLET_NOT_FOUND || 
               this == UNKNOWN_ERROR;
    }
    
    public boolean isProcessing() {
        return this == PROCESSING;
    }
}