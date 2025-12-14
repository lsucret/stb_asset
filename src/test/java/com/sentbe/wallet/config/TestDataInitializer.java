package com.sentbe.wallet.config;

import com.sentbe.wallet.domain.Wallet;
import com.sentbe.wallet.repository.WalletRepository;

import java.math.BigDecimal;

public class TestDataInitializer {
    
    public static void initializeTestData(WalletRepository walletRepository) {
        if (walletRepository.count() == 0) {
            walletRepository.save(new Wallet("wallet1", new BigDecimal("1000000.00")));
            walletRepository.save(new Wallet("wallet2", new BigDecimal("500000.00")));
            walletRepository.save(new Wallet("wallet3", new BigDecimal("100000.00")));
        }
    }
}