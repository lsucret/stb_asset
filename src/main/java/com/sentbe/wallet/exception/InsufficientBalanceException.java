package com.sentbe.wallet.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(BigDecimal balance, BigDecimal amount) {
        super(String.format("Insufficient balance. Current: %s, Requested: %s", balance, amount));
    }
}