package com.sampoom.purchase.api.purchase.service;

import com.sampoom.purchase.api.purchase.dto.PurchaseOrderRequestDto;
import com.sampoom.purchase.api.purchase.dto.PurchaseOrderResponseDto;
import com.sampoom.purchase.api.purchase.entity.*;
import com.sampoom.purchase.api.purchase.repository.PurchaseOrderItemRepository;
import com.sampoom.purchase.api.purchase.repository.PurchaseOrderRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderItemRepository orderItemRepository;

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

        PurchaseOrder order = PurchaseOrder.builder()
                .code(generateOrderCode())
                .factoryId(requestDto.getFactoryId())
                .status(OrderStatus.ORDERED)
                .orderAt(LocalDateTime.now())
                .factoryName(requestDto.getFactoryName())
                .requiredAt(requestDto.getRequiredAt())
                .requesterName(requestDto.getRequesterName())
                .expectedAmount(expectedAmount)
                .urgency(urgency)
                .build();

        orderRepository.save(order);

        List<PurchaseOrderItem> orderItems = requestDto.getItems().stream()
                .map(itemDto -> PurchaseOrderItem.builder()
                        .purchaseOrder(order)
                        .materialCode(itemDto.getMaterialCode())
                        .materialName(itemDto.getMaterialName())
                        .unit(itemDto.getUnit())
                        .quantity(itemDto.getQuantity())
                        .unitPrice(itemDto.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        return PurchaseOrderResponseDto.from(order, orderItems);
    }

    private UrgencyLevel calculateUrgency(LocalDate requiredAt) {
        if (requiredAt == null) return UrgencyLevel.LOW;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), requiredAt);
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
        List<PurchaseOrderResponseDto> content = pageResult.getContent().stream()
                .map(order -> PurchaseOrderResponseDto.from(order, orderItemRepository.findByPurchaseOrderId(order.getId())))
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
        List<PurchaseOrderItem> items = orderItemRepository.findByPurchaseOrderId(orderId);
        return PurchaseOrderResponseDto.from(order, items);
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.ORDER_NOT_FOUND));
        orderRepository.delete(order);
    }

    private String generateOrderCode() {
        String datePart = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        String prefix = "ORD-" + datePart + "-";

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
