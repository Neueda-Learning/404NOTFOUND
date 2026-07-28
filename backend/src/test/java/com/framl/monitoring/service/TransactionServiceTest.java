package com.framl.monitoring.service;

import com.framl.monitoring.dto.TransactionRequest;
import com.framl.monitoring.dto.TransactionResponse;
import com.framl.monitoring.entity.Alert;
import com.framl.monitoring.entity.Transaction;
import com.framl.monitoring.enums.AlertStatus;
import com.framl.monitoring.enums.TransactionStatus;
import com.framl.monitoring.enums.TransactionType;
import com.framl.monitoring.repository.AlertRepository;
import com.framl.monitoring.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private RuleEvaluationService ruleEvaluationService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, alertRepository, ruleEvaluationService);
    }

    @Test
    void ingest_throwsForDuplicateTransactionId() {
        TransactionRequest req = buildRequest(TransactionStatus.COMPLETED);
        when(transactionRepository.existsById(req.getTransactionId())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> transactionService.ingest(req));
        assertEquals("Duplicate transactionId: " + req.getTransactionId(), ex.getMessage());
    }

    @Test
    void ingest_completedTransactionEvaluatesRulesAndMarksLateArrival() {
        TransactionRequest req = buildRequest(TransactionStatus.COMPLETED);
        req.setTransactionTime(Instant.now().minusSeconds(10 * 60));

        when(transactionRepository.existsById(req.getTransactionId())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findByTransactionId(req.getTransactionId())).thenReturn(List.of());

        TransactionResponse response = transactionService.ingest(req);

        verify(ruleEvaluationService).evaluate(any(Transaction.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));

        assertEquals(req.getTransactionId(), response.getTransactionId());
        assertTrue(response.getLateArrival());
        assertNotNull(response.getEvaluatedAt());
        assertFalse(response.getHasAlert());
    }

    @Test
    void updateStatus_toCompletedEvaluatesWhenNotEvaluatedYet() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TXN-U1");
        tx.setStatus(TransactionStatus.PENDING);
        tx.setVersion(1);
        tx.setTransactionTime(Instant.now());
        tx.setReceivedAt(Instant.now());
        tx.setAccountId("ACC-1");
        tx.setType(TransactionType.DEBIT);
        tx.setAmount(new BigDecimal("12.34"));
        tx.setCurrency("USD");

        when(transactionRepository.findById("TXN-U1")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findByTransactionId("TXN-U1")).thenReturn(List.of());

        TransactionResponse response = transactionService.updateStatus("TXN-U1", TransactionStatus.COMPLETED, 1);

        verify(ruleEvaluationService).evaluate(tx);
        assertEquals(TransactionStatus.COMPLETED, response.getStatus());
        assertEquals(2, response.getVersion());
        assertNotNull(response.getEvaluatedAt());
    }

    @Test
    void updateStatus_throwsForTerminalStatusAndVersionConflict() {
        Transaction terminal = new Transaction();
        terminal.setTransactionId("TXN-T");
        terminal.setStatus(TransactionStatus.FAILED);
        terminal.setVersion(2);

        when(transactionRepository.findById("TXN-T")).thenReturn(Optional.of(terminal));

        IllegalStateException terminalEx = assertThrows(IllegalStateException.class,
                () -> transactionService.updateStatus("TXN-T", TransactionStatus.COMPLETED, 2));
        assertTrue(terminalEx.getMessage().contains("Cannot update terminal status"));

        Transaction nonTerminal = new Transaction();
        nonTerminal.setTransactionId("TXN-V");
        nonTerminal.setStatus(TransactionStatus.PENDING);
        nonTerminal.setVersion(5);

        when(transactionRepository.findById("TXN-V")).thenReturn(Optional.of(nonTerminal));

        IllegalStateException versionEx = assertThrows(IllegalStateException.class,
                () -> transactionService.updateStatus("TXN-V", TransactionStatus.COMPLETED, 4));
        assertEquals("Version conflict", versionEx.getMessage());
    }

    @Test
    void search_wrapsMappedPageResponse() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TXN-S1");
        tx.setAccountId("ACC-S1");
        tx.setType(TransactionType.CREDIT);
        tx.setAmount(new BigDecimal("44.10"));
        tx.setCurrency("USD");
        tx.setStatus(TransactionStatus.PENDING);
        tx.setTransactionTime(Instant.now());
        tx.setReceivedAt(Instant.now());

        Page<Transaction> page = new PageImpl<>(List.of(tx), PageRequest.of(0, 5), 1);
        when(transactionRepository.searchTransactions(eq("ACC-S1"), eq(null), eq(null), eq(null), eq(null), eq(null), eq("%abc%"), any()))
                .thenReturn(page);
        when(alertRepository.findByTransactionId("TXN-S1")).thenReturn(List.of());

        var responsePage = transactionService.search("ACC-S1", null, null, null, null, null, "abc", 0, 5);

        assertEquals(1, responsePage.getContent().size());
        assertEquals(1L, responsePage.getTotalElements());
        assertEquals("TXN-S1", responsePage.getContent().get(0).getTransactionId());
    }

    @Test
    void search_blankQueryIsTreatedAsNull() {
        Page<Transaction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(transactionRepository.searchTransactions(eq("ACC-S1"), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any()))
                .thenReturn(emptyPage);

        var responsePage = transactionService.search("ACC-S1", null, null, null, null, null, "   ", 0, 5);

        assertEquals(0, responsePage.getTotalElements());
        verify(transactionRepository).searchTransactions(eq("ACC-S1"), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any());
    }

    @Test
    void toResponse_setsAlertFieldsWhenAlertsExist() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TXN-A1");
        tx.setAccountId("ACC-A1");
        tx.setType(TransactionType.DEBIT);
        tx.setAmount(new BigDecimal("200"));
        tx.setCurrency("USD");
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setTransactionTime(Instant.now());
        tx.setReceivedAt(Instant.now());

        Alert alert = new Alert();
        alert.setAlertId("ALT-1");
        alert.setStatus(AlertStatus.OPEN);

        when(alertRepository.findByTransactionId("TXN-A1")).thenReturn(List.of(alert));

        TransactionResponse response = transactionService.toResponse(tx);

        assertTrue(response.getHasAlert());
        assertEquals(1, response.getAlertCount());
        assertEquals("ALT-1", response.getAlertId());
        assertEquals("OPEN", response.getAlertStatus());
        assertEquals(List.of("ALT-1"), response.getAlertIds());
    }

    @Test
    void updateStatus_doesNotReevaluateIfAlreadyEvaluated() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TXN-E1");
        tx.setStatus(TransactionStatus.PENDING);
        tx.setVersion(0);
        tx.setEvaluatedAt(Instant.now());
        tx.setTransactionTime(Instant.now());
        tx.setReceivedAt(Instant.now());
        tx.setAccountId("ACC-1");
        tx.setType(TransactionType.DEBIT);
        tx.setAmount(new BigDecimal("7"));
        tx.setCurrency("USD");

        when(transactionRepository.findById("TXN-E1")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRepository.findByTransactionId("TXN-E1")).thenReturn(List.of());

        transactionService.updateStatus("TXN-E1", TransactionStatus.COMPLETED, 0);

        verify(ruleEvaluationService, never()).evaluate(any(Transaction.class));
    }

    private TransactionRequest buildRequest(TransactionStatus status) {
        TransactionRequest req = new TransactionRequest();
        req.setTransactionId("TXN-NEW-1");
        req.setAccountId("ACC-1");
        req.setPayeeId("PAYEE-1");
        req.setPayeeName("Payee One");
        req.setType(TransactionType.DEBIT);
        req.setAmount(new BigDecimal("123.45"));
        req.setCurrency("USD");
        req.setStatus(status);
        req.setTransactionTime(Instant.now());
        req.setPaymentChannel("MOBILE");
        req.setCountry("US");
        req.setDescription("test transaction");
        return req;
    }
}
