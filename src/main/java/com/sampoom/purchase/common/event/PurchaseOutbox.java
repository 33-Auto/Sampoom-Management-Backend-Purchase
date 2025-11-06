package com.sampoom.purchase.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "purchase_outbox",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_purchase_outbox_event_id", columnNames = {"event_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PurchaseOutbox {

    @Id
    @Column(name = "purchase_outbox_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Long aggregateId;

    @Column(name = "event_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID eventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(columnDefinition = "text")
    private String lastError;

    private LocalDateTime publishedAt;

    private LocalDateTime lastTriedAt;

    private LocalDateTime nextRetryAt;

    // 상태 전환 메서드
    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.lastTriedAt = LocalDateTime.now();
        this.nextRetryAt = null;
    }

    public void markFailed(String error, LocalDateTime nextRetryAt) {
        this.status = OutboxStatus.FAILED;
        this.lastError = error;
        this.retryCount = (this.retryCount == null ? 1 : this.retryCount + 1);
        this.lastTriedAt = LocalDateTime.now();
        this.nextRetryAt = nextRetryAt;
    }

    public void markDead(String error) {
        this.status = OutboxStatus.DEAD;
        this.lastError = error;
        this.lastTriedAt = LocalDateTime.now();
    }

    public static PurchaseOutbox ready(Long aggregateId, String eventType, UUID eventId, JsonNode payloadJson) {
        return PurchaseOutbox.builder()
                .aggregateId(aggregateId)
                .eventType(eventType)
                .eventId(eventId)
                .payload(payloadJson)
                .occurredAt(LocalDateTime.now())
                .status(OutboxStatus.READY)
                .retryCount(0)
                .build();
    }
}
