package com.sampoom.purchase.common.event;

import java.util.List;

public record PurchaseEvent(
        String eventId,
        String eventType,
        Long version,
        String occurredAt,
        Payload payload
) {
    public record Payload(
            Long orderId,
            String orderCode,
            Long factoryId,
            String factoryName,
            String status,
            String receivedAt,
            Boolean deleted,
            List<Material> materials
    ) {}

    public record Material(
            String materialCode,
            String materialName,
            Integer quantity,
            String unit
    ) {}
}
