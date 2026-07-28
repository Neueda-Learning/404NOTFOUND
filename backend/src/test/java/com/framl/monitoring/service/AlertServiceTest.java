package com.framl.monitoring.service;

import com.framl.monitoring.dto.*;
import com.framl.monitoring.entity.*;
import com.framl.monitoring.enums.*;
import com.framl.monitoring.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private AlertService alertService;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, alertHistoryRepository, 
                                        transactionRepository, transactionService);
    }

    @Test
    void search_returnsPaginatedAlerts() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.OPEN, AlertSeverity.HIGH);
        Page<Alert> page = new PageImpl<>(List.of(alert));
        
        when(alertRepository.searchAlerts(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        PageResponse<AlertResponse> result = alertService.search(
                AlertStatus.OPEN, AlertSeverity.HIGH, "ACC-001", 
                Instant.now().minusSeconds(3600), Instant.now(), "test", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(alertRepository).searchAlerts(
                eq(AlertStatus.OPEN), eq(AlertSeverity.HIGH), eq("ACC-001"),
                any(), any(), contains("test"), any(Pageable.class));
    }

    @Test
    void search_appliesPaginationAndSorting() {
        Page<Alert> emptyPage = Page.empty();
        when(alertRepository.searchAlerts(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(emptyPage);

        PageResponse<AlertResponse> result = alertService.search(
                null, null, "ACC-001", null, null, null, 2, 20);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        verify(alertRepository).searchAlerts(
                isNull(), isNull(), eq("ACC-001"),
                isNull(), isNull(), isNull(), argThat(pageable -> 
                    pageable.getPageNumber() == 2 && pageable.getPageSize() == 20));
    }

    @Test
    void getById_returnsAlertDetails() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.ACKNOWLEDGED, AlertSeverity.MEDIUM);
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));
        when(alertHistoryRepository.findByAlertAlertIdOrderByChangedAtAsc("ALT-2024-00001"))
                .thenReturn(List.of());

        AlertResponse result = alertService.getById("ALT-2024-00001");

        assertNotNull(result);
        assertEquals("ALT-2024-00001", result.getAlertId());
        assertEquals(AlertStatus.ACKNOWLEDGED, result.getStatus());
        verify(alertRepository).findById("ALT-2024-00001");
    }

    @Test
    void getById_throwsForMissingAlert() {
        when(alertRepository.findById("INVALID")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> alertService.getById("INVALID"));
        verify(alertRepository).findById("INVALID");
    }

    @Test
    void acknowledge_transitionsFromOpenToAcknowledged() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.OPEN, AlertSeverity.HIGH);
        alert.setVersion(1);
        
        AlertActionRequest req = new AlertActionRequest();
        req.setComment("Acknowledged");
        
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);
        when(alertHistoryRepository.findByAlertAlertIdOrderByChangedAtAsc("ALT-2024-00001"))
                .thenReturn(List.of());

        AlertResponse result = alertService.acknowledge("ALT-2024-00001", req);

        assertNotNull(result);
        verify(alertRepository).save(argThat(a -> a.getStatus() == AlertStatus.ACKNOWLEDGED 
                && a.getComment().equals("Acknowledged")));
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    @Test
    void acknowledge_throwsForInvalidStatusTransition() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.CLOSED, AlertSeverity.HIGH);
        
        AlertActionRequest req = new AlertActionRequest();
        
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));

        assertThrows(IllegalStateException.class, 
                () -> alertService.acknowledge("ALT-2024-00001", req));
    }

    @Test
    void acknowledge_throwsForVersionMismatch() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.OPEN, AlertSeverity.HIGH);
        alert.setVersion(2);
        
        AlertActionRequest req = new AlertActionRequest();
        req.setExpectedVersion(1);
        
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));

        assertThrows(IllegalStateException.class, 
                () -> alertService.acknowledge("ALT-2024-00001", req));
    }

    @Test
    void acknowledge_throwsForStatusMismatch() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.OPEN, AlertSeverity.HIGH);
        
        AlertActionRequest req = new AlertActionRequest();
        req.setExpectedStatus(AlertStatus.ACKNOWLEDGED);
        
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));

        assertThrows(IllegalStateException.class, 
                () -> alertService.acknowledge("ALT-2024-00001", req));
    }

    @Test
    void investigate_transitionsFromAcknowledgedToInvestigating() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.ACKNOWLEDGED, AlertSeverity.HIGH);
        alert.setVersion(2);
        
        AlertActionRequest req = new AlertActionRequest();
        req.setComment("Under investigation");
        
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);
        when(alertHistoryRepository.findByAlertAlertIdOrderByChangedAtAsc("ALT-2024-00001"))
                .thenReturn(List.of());

        AlertResponse result = alertService.investigate("ALT-2024-00001", req);

        assertNotNull(result);
        verify(alertRepository).save(argThat(a -> a.getStatus() == AlertStatus.INVESTIGATING));
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    @Test
    void close_transitionsFromInvestigatingToClosed() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.INVESTIGATING, AlertSeverity.HIGH);
        alert.setVersion(3);
        
        AlertActionRequest req = new AlertActionRequest();
        req.setComment("Issue resolved");
        req.setResolutionCode(ResolutionCode.TRUE_POSITIVE);
        req.setResolution("Transaction verified as true positive");
        req.setResolutionNotes("Customer confirmed transaction");
        
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);
        when(alertHistoryRepository.findByAlertAlertIdOrderByChangedAtAsc("ALT-2024-00001"))
                .thenReturn(List.of());

        AlertResponse result = alertService.close("ALT-2024-00001", req);

        assertNotNull(result);
        verify(alertRepository).save(argThat(a -> a.getStatus() == AlertStatus.CLOSED 
                && a.getResolutionCode() == ResolutionCode.TRUE_POSITIVE));
        verify(alertHistoryRepository).save(any(AlertHistory.class));
    }

    @Test
    void dismiss_preventsDismissingClosedAlert() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.CLOSED, AlertSeverity.HIGH);
        
        AlertActionRequest req = new AlertActionRequest();
        
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));

        assertThrows(IllegalStateException.class, 
                () -> alertService.dismiss("ALT-2024-00001", req));
    }

    @Test
    void dismiss_preventsDismissingDismissedAlert() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.DISMISSED, AlertSeverity.HIGH);
        
        AlertActionRequest req = new AlertActionRequest();
        
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));

        assertThrows(IllegalStateException.class, 
                () -> alertService.dismiss("ALT-2024-00001", req));
    }

    @Test
    void dismiss_allowsDismissingOpenAlert() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.OPEN, AlertSeverity.HIGH);
        alert.setVersion(1);
        
        AlertActionRequest req = new AlertActionRequest();
        req.setComment("False positive");
        req.setResolutionCode(ResolutionCode.FALSE_POSITIVE);
        
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);
        when(alertHistoryRepository.findByAlertAlertIdOrderByChangedAtAsc("ALT-2024-00001"))
                .thenReturn(List.of());

        AlertResponse result = alertService.dismiss("ALT-2024-00001", req);

        assertNotNull(result);
        verify(alertRepository).save(argThat(a -> a.getStatus() == AlertStatus.DISMISSED));
    }

    @Test
    void dismiss_allowsDismissingAcknowledgedAlert() {
        Alert alert = createTestAlert("ALT-2024-00001", AlertStatus.ACKNOWLEDGED, AlertSeverity.HIGH);
        alert.setVersion(2);
        
        AlertActionRequest req = new AlertActionRequest();
        
        when(alertRepository.findById("ALT-2024-00001")).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);
        when(alertHistoryRepository.findByAlertAlertIdOrderByChangedAtAsc("ALT-2024-00001"))
                .thenReturn(List.of());

        AlertResponse result = alertService.dismiss("ALT-2024-00001", req);

        assertNotNull(result);
        verify(alertRepository).save(any(Alert.class));
    }

    // Helper method to create test alert
    private Alert createTestAlert(String alertId, AlertStatus status, AlertSeverity severity) {
        Alert alert = new Alert();
        alert.setAlertId(alertId);
        alert.setTitle("Test Alert");
        alert.setDescription("Test alert description");
        alert.setAccountId("ACC-001");
        alert.setStatus(status);
        alert.setSeverity(severity);
        alert.setRiskScore(75);
        alert.setCreatedAt(Instant.now());
        alert.setUpdatedAt(Instant.now());
        alert.setVersion(1);
        alert.setAlertTransactions(new ArrayList<>());
        return alert;
    }
}
