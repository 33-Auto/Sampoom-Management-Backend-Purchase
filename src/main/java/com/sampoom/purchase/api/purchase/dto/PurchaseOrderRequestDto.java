package com.sampoom.purchase.api.purchase.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequestDto {
    private Long factoryId;
    private String factoryName;

    private LocalDateTime requiredAt;

    private String requesterName; // 요청자 이름 추가

    private List<PurchaseOrderItemDto> items;
}
