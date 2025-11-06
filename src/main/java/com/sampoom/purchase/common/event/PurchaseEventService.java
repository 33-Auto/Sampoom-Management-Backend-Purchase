package com.sampoom.purchase.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.purchase.api.purchase.entity.PurchaseOrder;
import com.sampoom.purchase.api.purchase.entity.PurchaseOrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseEventService {

    private final PurchaseOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordOrderReceived(PurchaseOrder order) {
        enqueueEvent("PurchaseOrderReceived", order, false);
    }

    @Transactional
    public void recordOrderCanceled(PurchaseOrder order) {
        enqueueEvent("PurchaseOrderCanceled", order, false);
    }

    @Transactional
    public void recordOrderCreated(PurchaseOrder order) {
        enqueueEvent("PurchaseOrderCreated", order, false);
    }

    @Transactional
    public void recordOrderDeleted(PurchaseOrder order) {
        enqueueEvent("PurchaseOrderDeleted", order, true);
    }

    // 공통 헬퍼 메서드
    private void enqueueEvent(String eventType, PurchaseOrder order, Boolean deleted) {
        // 자재 정보를 Material 리스트로 변환
        List<PurchaseEvent.Material> materials = order.getItems() != null ?
            order.getItems().stream()
                .map(this::convertToMaterial)
                .collect(Collectors.toList()) :
            List.of();

        PurchaseEvent evt = new PurchaseEvent(
                UUID.randomUUID().toString(),
                eventType,
                1L, // 버전 정보
                OffsetDateTime.now().toString(),
                new PurchaseEvent.Payload(
                        order.getId(),
                        order.getCode(),
                        order.getFactoryId(),
                        order.getFactoryName(),
                        order.getStatus().name(),
                        order.getReceivedAt() != null ? order.getReceivedAt().toString() : null,
                        deleted,
                        materials
                )
        );

        try {
            JsonNode payload = objectMapper.valueToTree(evt);
            outboxRepository.save(
                    PurchaseOutbox.ready(
                            order.getId(),
                            eventType,
                            UUID.fromString(evt.eventId()),
                            payload
                    )
            );
        } catch (Exception e) {
            throw new IllegalStateException("Serialize " + eventType + " event failed", e);
        }
    }

    private PurchaseEvent.Material convertToMaterial(PurchaseOrderItem item) {
        return new PurchaseEvent.Material(
                item.getMaterialCode(),
                item.getMaterialName(),
                item.getQuantity().intValue(), // Long을 Integer로 변환
                item.getUnit()
        );
    }
}
