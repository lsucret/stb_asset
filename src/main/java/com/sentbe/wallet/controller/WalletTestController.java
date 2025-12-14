package com.sentbe.wallet.controller;

import com.sentbe.wallet.domain.Wallet;
import com.sentbe.wallet.repository.WalletRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/test/wallets")
public class WalletTestController {
    
    private final WalletRepository walletRepository;
    
    public WalletTestController(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }
    
    @PostMapping
    public ResponseEntity<Wallet> createWallet(@RequestParam(defaultValue = "1000000") BigDecimal balance) {
        String walletId = "wallet_" + UUID.randomUUID().toString().substring(0, 8);
        
        Wallet savedWallet = walletRepository.save(new Wallet(walletId, balance));
        return ResponseEntity.ok(savedWallet);
    }
    
    @GetMapping("/{walletId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable String walletId) {
        return walletRepository.findById(walletId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}