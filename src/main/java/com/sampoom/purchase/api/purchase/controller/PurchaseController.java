package com.sampoom.purchase.api.purchase.controller;

import com.sampoom.purchase.api.purchase.dto.PurchaseOrderRequestDto;
import com.sampoom.purchase.api.purchase.dto.PurchaseOrderResponseDto;
import com.sampoom.purchase.api.purchase.entity.OrderStatus;
import com.sampoom.purchase.api.purchase.entity.UrgencyLevel;
import com.sampoom.purchase.api.purchase.service.PurchaseService;
import com.sampoom.purchase.common.response.ApiResponse;
import com.sampoom.purchase.common.response.PageResponseDto;
import com.sampoom.purchase.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Purchase", description = "Purchase 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @Operation(summary = "자재 주문 생성", description = "공장에 필요한 자재 주문을 생성합니다.")
    @PostMapping()
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> createMaterialOrder(
            @RequestBody PurchaseOrderRequestDto requestDto) {
        return ApiResponse.success(SuccessStatus.CREATED,
                purchaseService.createMaterialOrder(requestDto));
    }



    @Operation(summary = "자재 주문 취소", description = "주문을 취소 처리합니다.")
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> cancelOrder(
            @PathVariable Long orderId) {
        return ApiResponse.success(SuccessStatus.OK, purchaseService.cancelOrder(orderId));
    }

    @Operation(summary = "자재 주문 입고 처리", description = "주문된 자재를 입고 처리합니다.")
    @PatchMapping("/{orderId}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> receiveOrder(
            @PathVariable Long orderId) {
        return ApiResponse.success(SuccessStatus.OK, purchaseService.receiveOrder(orderId));
    }

    @Operation(summary = "자재 주문 삭제", description = "주문을 삭제합니다(소프트 삭제).")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @PathVariable Long orderId) {
        purchaseService.deleteOrder(orderId);
        return ApiResponse.success_only(SuccessStatus.OK);
    }

    @Operation(summary = "자재 주문 단건 조회", description = "특정 자재 주문의 상세를 조회합니다.")
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> getOrder(
            @PathVariable Long orderId) {
        return ApiResponse.success(SuccessStatus.OK, purchaseService.getOrder(orderId));
    }

    @Operation(summary = "자재 주문 목록 조회", description = "주문 상태 필터와 검색(자재명/자재코드/주문코드), 긴급도 필터로 목록을 조회합니다.")
    @GetMapping()
    public ResponseEntity<ApiResponse<PageResponseDto<PurchaseOrderResponseDto>>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UrgencyLevel urgency,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(SuccessStatus.OK, purchaseService.getOrders(status, urgency, query, page, size));
    }
}
