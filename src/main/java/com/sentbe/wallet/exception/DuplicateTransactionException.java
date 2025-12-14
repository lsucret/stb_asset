package com.sentbe.wallet.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String transactionId) {
        super("Duplicate transaction ID: " + transactionId);
    }
}