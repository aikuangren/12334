package com.fiftytalk.carddraw.repository;

import com.fiftytalk.carddraw.entity.DrawOrderCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DrawOrderCardRepository extends JpaRepository<DrawOrderCard, Long> {
    List<DrawOrderCard> findByOrderId(Long orderId);
    List<DrawOrderCard> findByOrderIdIn(Collection<Long> orderIds);
}

