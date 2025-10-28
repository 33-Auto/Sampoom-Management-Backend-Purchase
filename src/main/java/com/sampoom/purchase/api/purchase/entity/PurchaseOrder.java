package com.sampoom.purchase.api.purchase.entity;

import com.sampoom.purchase.common.entitiy.SoftDeleteEntity;
import com.sampoom.purchase.common.exception.BadRequestException;
import com.sampoom.purchase.common.response.ErrorStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "purchase_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE purchase_order SET deleted = true, deleted_at = now() WHERE purchase_order_id = ?")
@SQLRestriction("deleted = false")
public class PurchaseOrder extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_order_id")
    private Long id;

    private String code;
    private LocalDateTime orderAt;
    private LocalDateTime receivedAt;
    private LocalDateTime canceledAt;
    private LocalDate requiredAt;


    @Enumerated(EnumType.STRING)
    private OrderStatus status;


    private Long factoryId;

    private String factoryName;

    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgency; // 긴급도

    private String requesterName;  // 요청자 이름

    @Column(precision = 19, scale = 2)
    private BigDecimal expectedAmount; // 예상 금액

    public void receive() {
        if (this.status != OrderStatus.ORDERED) {
            throw new BadRequestException(ErrorStatus.ORDER_ALREADY_PROCESSED);
        }
        this.status = OrderStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }

    public void cancel() {

        if (this.status != OrderStatus.ORDERED) {
            throw new BadRequestException(ErrorStatus.ORDER_ALREADY_PROCESSED);
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }
}
