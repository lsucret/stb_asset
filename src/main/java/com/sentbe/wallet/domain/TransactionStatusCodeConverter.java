package com.sentbe.wallet.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionStatusCodeConverter implements AttributeConverter<TransactionStatusCode, String> {
    
    @Override
    public String convertToDatabaseColumn(TransactionStatusCode attribute) {
        return attribute != null ? attribute.getCode() : null;
    }
    
    @Override
    public TransactionStatusCode convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        
        for (TransactionStatusCode status : TransactionStatusCode.values()) {
            if (status.getCode().equals(dbData)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Unknown status code: " + dbData);
    }
}