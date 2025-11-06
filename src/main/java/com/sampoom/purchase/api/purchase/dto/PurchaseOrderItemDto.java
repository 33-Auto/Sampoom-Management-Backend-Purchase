package com.sampoom.purchase.api.purchase.dto;

import com.sampoom.purchase.api.purchase.entity.PurchaseOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItemDto {
    private String materialCode;
    private String materialName;
    private String unit;
    private Long quantity;
    private BigDecimal unitPrice;
    private Integer leadTimeDays; // 자재 리드타임 (일 단위)

    public static PurchaseOrderItemDto from(PurchaseOrderItem item) {
        return PurchaseOrderItemDto.builder()
                .materialCode(item.getMaterialCode())
                .materialName(item.getMaterialName())
                .unit(item.getUnit())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .leadTimeDays(item.getLeadTimeDays())
                .build();
    }
}
