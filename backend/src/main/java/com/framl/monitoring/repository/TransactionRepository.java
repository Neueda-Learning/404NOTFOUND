package com.framl.monitoring.repository;

import com.framl.monitoring.entity.Transaction;
import com.framl.monitoring.enums.TransactionStatus;
import com.framl.monitoring.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.accountId = :accountId " +
           "AND t.status = 'COMPLETED' AND t.type IN :types " +
           "AND t.transactionTime BETWEEN :windowStart AND :windowEnd " +
           "AND t.transactionId <> :currentTxId")
    long countVelocityTransactions(@Param("accountId") String accountId,
                                   @Param("types") List<String> types,
                                   @Param("windowStart") Instant windowStart,
                                   @Param("windowEnd") Instant windowEnd,
                                   @Param("currentTxId") String currentTxId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.accountId = :accountId " +
           "AND t.status = 'COMPLETED' AND t.type = 'DEBIT' AND t.currency = :currency " +
           "AND t.transactionTime BETWEEN :dayStart AND :current " +
           "AND t.transactionId <> :currentTxId")
    BigDecimal sumDailyAmount(@Param("accountId") String accountId,
                               @Param("currency") String currency,
                               @Param("dayStart") Instant dayStart,
                               @Param("current") Instant current,
                               @Param("currentTxId") String currentTxId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.accountId = :accountId " +
           "AND t.status = 'COMPLETED' AND t.type = 'DEBIT' AND t.payeeId = :payeeId " +
           "AND (t.transactionTime < :txTime OR (t.transactionTime = :txTime AND t.transactionId < :txId))")
    long countPreviousPayeeTransactions(@Param("accountId") String accountId,
                                        @Param("payeeId") String payeeId,
                                        @Param("txTime") Instant txTime,
                                        @Param("txId") String txId);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:accountId IS NULL OR t.accountId = :accountId) AND " +
           "(:payeeId IS NULL OR t.payeeId = :payeeId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:fromTime IS NULL OR t.transactionTime >= :fromTime) AND " +
           "(:toTime IS NULL OR t.transactionTime <= :toTime) AND " +
           "(:q IS NULL OR t.transactionId LIKE :q OR t.accountId LIKE :q OR t.payeeId LIKE :q)")
    Page<Transaction> searchTransactions(@Param("accountId") String accountId,
                                          @Param("payeeId") String payeeId,
                                          @Param("status") TransactionStatus status,
                                          @Param("type") TransactionType type,
                                          @Param("fromTime") Instant fromTime,
                                          @Param("toTime") Instant toTime,
                                          @Param("q") String q,
                                          Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionTime >= :start AND t.transactionTime <= :end")
    long countByTimeRange(@Param("start") Instant start, @Param("end") Instant end);
}
