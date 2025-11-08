package com.sampoom.purchase.api.purchase.dto;

import com.sampoom.purchase.api.purchase.entity.PurchaseOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItemDto {
    private String materialCode;
    private String materialName;
    private String unit;
    private Long quantity;
    private Long standardQuantity; // 표준 수량
    private BigDecimal unitPrice;
    private Integer leadTimeDays; // 자재 리드타임 (일 단위)

    public static PurchaseOrderItemDto from(PurchaseOrderItem item) {
        return PurchaseOrderItemDto.builder()
                .materialCode(item.getMaterialCode())
                .materialName(item.getMaterialName())
                .unit(item.getUnit())
                .quantity(item.getQuantity())
                .standardQuantity(item.getStandardQuantity())
                .unitPrice(item.getUnitPrice())
                .leadTimeDays(item.getLeadTimeDays())
                .build();
    }

    /**
     * 예정일 계산 메서드
     * (quantity/standardQuantity) * leadTimeDays 공식으로 계산
     * @param orderDate 주문일
     * @return 예정일
     */
    public LocalDate calculateExpectedDate(LocalDate orderDate) {
        if (standardQuantity == null || standardQuantity == 0 || leadTimeDays == null) {
            return orderDate;
        }

        double ratio = (double) quantity / standardQuantity;
        int calculatedLeadDays = (int) Math.ceil(ratio * leadTimeDays);

        return orderDate.plusDays(calculatedLeadDays);
    }
}
