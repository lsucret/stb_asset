package com.sentbe.wallet.controller;

import com.sentbe.wallet.domain.Wallet;
import com.sentbe.wallet.domain.WalletTransaction;
import com.sentbe.wallet.dto.WithdrawRequest;
import com.sentbe.wallet.dto.WithdrawResponse;
import com.sentbe.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {
    
    private final WalletService walletService;
    
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }
    
    @PostMapping("/{walletId}/withdraw")
    public ResponseEntity<WithdrawResponse> withdraw(
            @PathVariable String walletId,
            @Valid @RequestBody WithdrawRequest request) {
        
        WalletTransaction transaction = walletService.withdraw(
            walletId, request.transactionId(), request.amount()
        );
        
        WithdrawResponse response = new WithdrawResponse(transaction);
        return ResponseEntity.ok(response);
    }
    

}