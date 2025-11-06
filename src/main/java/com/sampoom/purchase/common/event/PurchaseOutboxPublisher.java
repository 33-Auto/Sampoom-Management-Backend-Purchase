package com.sampoom.purchase.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseOutboxPublisher {

    private final PurchaseOutboxRepository repo;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PURCHASE = "purchase-events";
    private static final int BATCH = 100;
    private static final int MAX_RETRY = 10;
    private static final long BASE_BACKOFF_MS = 500;     // 0.5s
    private static final long MAX_BACKOFF_MS = 60_000;   // 60s


    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishBatch() {
        List<PurchaseOutbox> batch = repo.pickReadyBatch(BATCH, MAX_RETRY);
        if (batch.isEmpty()) return;

        for (PurchaseOutbox o : batch) {
            try {
                Object evt;
                String topic;

                // Purchase 이벤트 처리 - JsonNode를 직접 전송
                topic = TOPIC_PURCHASE;
                evt = o.getPayload(); // JsonNode를 직접 사용

                kafkaTemplate.send(topic, String.valueOf(o.getAggregateId()), evt)
                        .get(5, TimeUnit.SECONDS);

                o.markPublished();

            } catch (Exception e) {
                int nextRetry = o.getRetryCount() + 1;

                if (nextRetry >= MAX_RETRY) {
                    o.markDead(shorten(e.getMessage(), 2000));
                    log.error("Outbox DEAD id={} retry={} cause={}", o.getId(), o.getRetryCount(), e.toString());
                    continue;
                }

                long backoffMs = computeBackoffMs(nextRetry);
                LocalDateTime next = LocalDateTime.now().plusNanos(backoffMs * 1_000_000);
                o.markFailed(shorten(e.getMessage(), 2000), next);
                log.warn("Outbox publish failed id={} retry={} cause={}", o.getId(), o.getRetryCount(), e.toString());
            }
        }
    }

    private String shorten(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max);
    }

    private long computeBackoffMs(int retry) {
        double exp = Math.min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * Math.pow(2, Math.max(0, retry - 1)));
        double jitter = exp * (Math.random() * 0.1); // 0~10% 지터
        return (long) Math.min(MAX_BACKOFF_MS, exp + jitter);
    }
}
