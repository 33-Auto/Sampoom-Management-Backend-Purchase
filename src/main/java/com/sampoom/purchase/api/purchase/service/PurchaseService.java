package com.sampoom.purchase.api.purchase.service;

import com.sampoom.purchase.api.purchase.dto.PurchaseOrderRequestDto;
import com.sampoom.purchase.api.purchase.dto.PurchaseOrderResponseDto;
import com.sampoom.purchase.api.purchase.entity.*;
import com.sampoom.purchase.api.purchase.repository.PurchaseOrderItemRepository;
import com.sampoom.purchase.api.purchase.repository.PurchaseOrderRepository;
import com.sampoom.purchase.common.event.PurchaseEventService;
import com.sampoom.purchase.common.exception.NotFoundException;
import com.sampoom.purchase.common.response.ErrorStatus;
import com.sampoom.purchase.common.response.PageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderItemRepository orderItemRepository;
    private final PurchaseEventService purchaseEventService;

    @Transactional
    public PurchaseOrderResponseDto createMaterialOrder(PurchaseOrderRequestDto requestDto) {
        // 예상 금액 합산: Σ(unitPrice * quantity)
        BigDecimal expectedAmount = requestDto.getItems() == null ? BigDecimal.ZERO :
                requestDto.getItems().stream()
                        .map(i -> (i.getUnitPrice() == null ? BigDecimal.ZERO : i.getUnitPrice())
                                .multiply(BigDecimal.valueOf(i.getQuantity() == null ? 0L : i.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 긴급도 계산: 오늘과 필요일 차이 기준
        UrgencyLevel urgency = calculateUrgency(requestDto.getRequiredAt());

        // 최대 계산된 리드타임 계산: (quantity/standardQuantity) * leadTimeDays 공식 적용
        Integer maxCalculatedLeadTime = requestDto.getItems() == null ? 0 :
                requestDto.getItems().stream()
                        .mapToInt(item -> calculateItemLeadTime(item))
                        .max()
                        .orElse(0);

        // 예정일 계산: 주문일 + 최대 계산된 리드타임
        LocalDateTime expectedDeliveryAt = LocalDateTime.now().plusDays(maxCalculatedLeadTime);

        PurchaseOrder order = PurchaseOrder.builder()
                .code(generateOrderCode())
                .factoryId(requestDto.getFactoryId())
                .status(OrderStatus.ORDERED)
                .orderAt(LocalDateTime.now())
                .factoryName(requestDto.getFactoryName())
                .requiredAt(requestDto.getRequiredAt())
                .expectedDeliveryAt(expectedDeliveryAt)
                .requesterName(requestDto.getRequesterName())
                .expectedAmount(expectedAmount)
                .urgency(urgency)
                .build();

        orderRepository.save(order);

        // lambda에서 사용하기 위해 final 변수로 복사
        final PurchaseOrder savedOrder = order;

        List<PurchaseOrderItem> orderItems = requestDto.getItems().stream()
                .map(itemDto -> PurchaseOrderItem.builder()
                        .purchaseOrder(savedOrder)
                        .materialCode(itemDto.getMaterialCode())
                        .materialName(itemDto.getMaterialName())
                        .unit(itemDto.getUnit())
                        .quantity(itemDto.getQuantity())
                        .standardQuantity(itemDto.getStandardQuantity())
                        .unitPrice(itemDto.getUnitPrice())
                        .leadTimeDays(itemDto.getLeadTimeDays())
                        .build())
                .collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        // order에 items 설정 (이벤트에서 사용하기 위해)
        PurchaseOrder orderWithItems = PurchaseOrder.builder()
                .id(savedOrder.getId())
                .code(savedOrder.getCode())
                .factoryId(savedOrder.getFactoryId())
                .factoryName(savedOrder.getFactoryName())
                .status(savedOrder.getStatus())
                .orderAt(savedOrder.getOrderAt())
                .receivedAt(savedOrder.getReceivedAt())
                .canceledAt(savedOrder.getCanceledAt())
                .requiredAt(savedOrder.getRequiredAt())
                .expectedDeliveryAt(savedOrder.getExpectedDeliveryAt())
                .requesterName(savedOrder.getRequesterName())
                .expectedAmount(savedOrder.getExpectedAmount())
                .urgency(savedOrder.getUrgency())
                .items(orderItems)
                .build();

        // 주문 생성 이벤트 발행
        purchaseEventService.recordOrderCreated(orderWithItems);

        return PurchaseOrderResponseDto.from(savedOrder, orderItems);
    }

    /**
     * 개별 아이템의 계산된 리드타임 구하기
     * (quantity/standardQuantity) * leadTimeDays 공식 적용
     */
    private int calculateItemLeadTime(com.sampoom.purchase.api.purchase.dto.PurchaseOrderItemDto item) {
        if (item.getStandardQuantity() == null || item.getStandardQuantity() == 0
            || item.getLeadTimeDays() == null || item.getQuantity() == null) {
            return item.getLeadTimeDays() == null ? 0 : item.getLeadTimeDays();
        }

        double ratio = (double) item.getQuantity() / item.getStandardQuantity();
        return (int) Math.ceil(ratio * item.getLeadTimeDays());
    }

    private UrgencyLevel calculateUrgency(LocalDateTime requiredAt) {
        if (requiredAt == null) return UrgencyLevel.LOW;
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), requiredAt);
        if (days <= 1) return UrgencyLevel.HIGH;
        if (days <= 3) return UrgencyLevel.MEDIUM;
        return UrgencyLevel.LOW;
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto getOrder(Long orderId) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND));
        List<PurchaseOrderItem> items = orderItemRepository.findByPurchaseOrderId(orderId);
        return PurchaseOrderResponseDto.from(order, items);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<PurchaseOrderResponseDto> getOrders(OrderStatus status, UrgencyLevel urgency, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderAt"));
        Page<PurchaseOrder> pageResult = orderRepository.search(status, urgency, query, pageable);
        List<Long> orderIds = pageResult.getContent().stream()
                                .map(PurchaseOrder::getId).toList();
                List<PurchaseOrderItem> allItems =
                                orderItemRepository.findByPurchaseOrderIdIn(orderIds);
                Map<Long, List<PurchaseOrderItem>> itemsByOrderId = allItems.stream()
                                .collect(Collectors.groupingBy(i -> i.getPurchaseOrder().getId()));
                List<PurchaseOrderResponseDto> content = pageResult.getContent().stream()
                                .map(order -> PurchaseOrderResponseDto.from(
                                        order,
                                        itemsByOrderId.getOrDefault(order.getId(), java.util.Collections.emptyList())))
                                .collect(Collectors.toList());
        return PageResponseDto.<PurchaseOrderResponseDto>builder()
                .content(content)
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }



    @Transactional
    public PurchaseOrderResponseDto cancelOrder(Long orderId) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND));
        order.cancel();
        orderRepository.save(order);

        // items 로드하여 이벤트에 포함
        List<PurchaseOrderItem> items = orderItemRepository.findByPurchaseOrderId(orderId);

        // order에 items 설정
        order = PurchaseOrder.builder()
                .id(order.getId())
                .code(order.getCode())
                .factoryId(order.getFactoryId())
                .factoryName(order.getFactoryName())
                .status(order.getStatus())
                .orderAt(order.getOrderAt())
                .receivedAt(order.getReceivedAt())
                .canceledAt(order.getCanceledAt())
                .requiredAt(order.getRequiredAt())
                .expectedDeliveryAt(order.getExpectedDeliveryAt())
                .requesterName(order.getRequesterName())
                .expectedAmount(order.getExpectedAmount())
                .urgency(order.getUrgency())
                .items(items)
                .build();

        // 주문 취소 이벤트 발행
        purchaseEventService.recordOrderCanceled(order);

        return PurchaseOrderResponseDto.from(order, items);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND));

        // items 로드하여 이벤트에 포함
        List<PurchaseOrderItem> items = orderItemRepository.findByPurchaseOrderId(orderId);

        // order에 items 설정
        order = PurchaseOrder.builder()
                .id(order.getId())
                .code(order.getCode())
                .factoryId(order.getFactoryId())
                .factoryName(order.getFactoryName())
                .status(order.getStatus())
                .orderAt(order.getOrderAt())
                .receivedAt(order.getReceivedAt())
                .canceledAt(order.getCanceledAt())
                .requiredAt(order.getRequiredAt())
                .expectedDeliveryAt(order.getExpectedDeliveryAt())
                .requesterName(order.getRequesterName())
                .expectedAmount(order.getExpectedAmount())
                .urgency(order.getUrgency())
                .items(items)
                .build();

        // 주문 삭제 이벤트 발행
        purchaseEventService.recordOrderDeleted(order);

        orderRepository.delete(order);
    }

    @Transactional
    public PurchaseOrderResponseDto receiveOrder(Long orderId) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND));
        order.receive();
        orderRepository.save(order);

        // items 로드하여 이벤트에 포함
        List<PurchaseOrderItem> items = orderItemRepository.findByPurchaseOrderId(orderId);

        // order에 items 설정
        order = PurchaseOrder.builder()
                .id(order.getId())
                .code(order.getCode())
                .factoryId(order.getFactoryId())
                .factoryName(order.getFactoryName())
                .status(order.getStatus())
                .orderAt(order.getOrderAt())
                .receivedAt(order.getReceivedAt())
                .canceledAt(order.getCanceledAt())
                .requiredAt(order.getRequiredAt())
                .expectedDeliveryAt(order.getExpectedDeliveryAt())
                .requesterName(order.getRequesterName())
                .expectedAmount(order.getExpectedAmount())
                .urgency(order.getUrgency())
                .items(items)
                .build();

        // 자재 입고 처리 이벤트 발행
        purchaseEventService.recordOrderReceived(order);

        return PurchaseOrderResponseDto.from(order, items);
    }

    private String generateOrderCode() {
        String datePart = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")); // YYMMDD 형식으로 변경
        String prefix = "PR-" + datePart + "-";

        String lastCode = orderRepository
                .findTopByCodeStartingWithOrderByCodeDesc(prefix)
                .map(PurchaseOrder::getCode)
                .orElse(null);

        int nextSeq = 1;
        if (lastCode != null && lastCode.startsWith(prefix)) {
            String seqStr = lastCode.substring(prefix.length());
            try {
                nextSeq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException ignored) {
                // keep as 1
            }
        }
        return prefix + String.format("%03d", nextSeq);
    }
}
