package com.sampoom.purchase.common.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseOutboxRepository extends JpaRepository<PurchaseOutbox, Long> {

    @Query("""
            SELECT o FROM PurchaseOutbox o 
            WHERE o.status = 'READY' 
               OR (o.status = 'FAILED' AND o.nextRetryAt <= CURRENT_TIMESTAMP AND o.retryCount < :maxRetry)
            ORDER BY o.occurredAt ASC
            LIMIT :batchSize
            """)
    List<PurchaseOutbox> pickReadyBatch(@Param("batchSize") int batchSize, @Param("maxRetry") int maxRetry);
}
