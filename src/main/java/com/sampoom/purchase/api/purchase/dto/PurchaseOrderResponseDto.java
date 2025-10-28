package com.sampoom.purchase.api.purchase.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sampoom.purchase.api.purchase.entity.OrderStatus;
import com.sampoom.purchase.api.purchase.entity.PurchaseOrder;
import com.sampoom.purchase.api.purchase.entity.PurchaseOrderItem;
import com.sampoom.purchase.api.purchase.entity.UrgencyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponseDto {
    private Long id;
    private String orderCode;
    private LocalDateTime orderAt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate requiredAt; // 날짜만

    private Long factoryId;
    private String factoryName;
    private String requesterName;
    private UrgencyLevel urgency;
    private BigDecimal expectedAmount;
    private OrderStatus status;
    private List<PurchaseOrderItemDto> items;

    public static PurchaseOrderResponseDto from(PurchaseOrder order, List<PurchaseOrderItem> orderItems) {
        return PurchaseOrderResponseDto.builder()
                .id(order.getId())
                .status(order.getStatus())
                .orderCode(order.getCode())
                .orderAt(order.getOrderAt())
                .requiredAt(order.getRequiredAt())
                .factoryId(order.getFactoryId())
                .factoryName(order.getFactoryName())
                .requesterName(order.getRequesterName())
                .urgency(order.getUrgency())
                .expectedAmount(order.getExpectedAmount())
                .items(orderItems.stream().map(PurchaseOrderItemDto::from).collect(Collectors.toList()))
                .build();
    }
}
