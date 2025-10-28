package com.sampoom.purchase.api.purchase.repository;

import com.sampoom.purchase.api.purchase.entity.OrderStatus;
import com.sampoom.purchase.api.purchase.entity.PurchaseOrder;
import com.sampoom.purchase.api.purchase.entity.UrgencyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("select distinct po from PurchaseOrder po left join PurchaseOrderItem i on i.purchaseOrder = po " +
            "where (:status is null or po.status = :status) " +
            "and (:urgency is null or po.urgency = :urgency) " +
            "and (:query is null or :query = '' or lower(po.code) like lower(concat('%', :query, '%')) " +
            "or lower(i.materialCode) like lower(concat('%', :query, '%')) " +
            "or lower(i.materialName) like lower(concat('%', :query, '%')))" )
    Page<PurchaseOrder> search(@Param("status") OrderStatus status,
                               @Param("urgency") UrgencyLevel urgency,
                               @Param("query") String query,
                               Pageable pageable);

    Optional<PurchaseOrder> findTopByCodeStartingWithOrderByCodeDesc(String prefix);
}
