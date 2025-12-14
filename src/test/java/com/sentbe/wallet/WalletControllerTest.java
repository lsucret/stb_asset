package com.sentbe.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentbe.wallet.dto.WithdrawRequest;
import com.sentbe.wallet.repository.WalletRepository;
import com.sentbe.wallet.repository.WalletTransactionRepository;
import com.sentbe.wallet.domain.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WalletControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private WalletTransactionRepository transactionRepository;
    
    private static final String TEST_WALLET_ID = "test-wallet";
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100000.00");
    
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        
        // 테스트용 월렛 생성
        Wallet testWallet = new Wallet(TEST_WALLET_ID, INITIAL_BALANCE);
        walletRepository.save(testWallet);
    }
    
    @Test
    @DisplayName("정상 출금 요청 - 200 OK 응답")
    void successfulWithdrawRequest() throws Exception {
        // Given
        WithdrawRequest request = new WithdrawRequest(
            new BigDecimal("30000.00"), 
            "TXN_API_SUCCESS_001"
        );
        
        // When & Then
        mockMvc.perform(post("/api/wallets/{walletId}/withdraw", TEST_WALLET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(TEST_WALLET_ID))
                .andExpect(jsonPath("$.transactionId").value("TXN_API_SUCCESS_001"))
                .andExpect(jsonPath("$.amount").value(30000.00))
                .andExpect(jsonPath("$.balanceAfter").value(70000.00))
                .andExpect(jsonPath("$.status").value("10")); // SUCCESS
    }
    
    @Test
    @DisplayName("잔액 부족 - 400 Bad Request 응답")
    void insufficientBalanceRequest() throws Exception {
        // Given
        WithdrawRequest request = new WithdrawRequest(
            new BigDecimal("150000.00"), // 잔액보다 큰 금액
            "TXN_API_INSUFFICIENT_001"
        );
        
        // When & Then
        mockMvc.perform(post("/api/wallets/{walletId}/withdraw", TEST_WALLET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.walletId").value(TEST_WALLET_ID))
                .andExpect(jsonPath("$.status").value("20")); // INSUFFICIENT_BALANCE
    }
    
    @Test
    @DisplayName("존재하지 않는 월렛 - 404 Not Found 응답")
    void walletNotFoundRequest() throws Exception {
        // Given
        WithdrawRequest request = new WithdrawRequest(
            new BigDecimal("10000.00"),
            "TXN_API_NOT_FOUND_001"
        );
        
        // When & Then
        mockMvc.perform(post("/api/wallets/{walletId}/withdraw", "non-existent-wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("유효하지 않은 요청 - amount가 0 이하일 때 400 Bad Request")
    void invalidAmountRequest() throws Exception {
        // Given
        WithdrawRequest request = new WithdrawRequest(
            new BigDecimal("0.00"), // 0원
            "TXN_API_INVALID_001"
        );
        
        // When & Then
        mockMvc.perform(post("/api/wallets/{walletId}/withdraw", TEST_WALLET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("유효하지 않은 요청 - transactionId가 빈 문자열일 때 400 Bad Request")
    void invalidTransactionIdRequest() throws Exception {
        // Given
        WithdrawRequest request = new WithdrawRequest(
            new BigDecimal("10000.00"),
            "" // 빈 문자열
        );
        
        // When & Then
        mockMvc.perform(post("/api/wallets/{walletId}/withdraw", TEST_WALLET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("유효하지 않은 요청 - amount가 null일 때 400 Bad Request")
    void nullAmountRequest() throws Exception {
        // Given
        String requestJson = """
            {
                "transactionId": "TXN_API_NULL_001"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/wallets/{walletId}/withdraw", TEST_WALLET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("유효하지 않은 JSON 형식 - 400 Bad Request")
    void invalidJsonRequest() throws Exception {
        // Given
        String invalidJson = "{ invalid json }";
        
        // When & Then
        mockMvc.perform(post("/api/wallets/{walletId}/withdraw", TEST_WALLET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Content-Type이 없는 요청 - 415 Unsupported Media Type")
    void missingContentTypeRequest() throws Exception {
        // Given
        WithdrawRequest request = new WithdrawRequest(
            new BigDecimal("10000.00"),
            "TXN_API_NO_CONTENT_TYPE_001"
        );
        
        // When & Then
        mockMvc.perform(post("/api/wallets/{walletId}/withdraw", TEST_WALLET_ID)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }
}