package com.fiftytalk.carddraw.repository;

import com.fiftytalk.carddraw.entity.DrawOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DrawOrderRepository extends JpaRepository<DrawOrder, Long> {
    long countByUserId(Long userId);
    Optional<DrawOrder> findByUserIdAndRequestId(Long userId, String requestId);
    List<DrawOrder> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
    List<DrawOrder> findTop20ByUserIdAndPoolIdOrderByCreatedAtDesc(Long userId, Long poolId);
    List<DrawOrder> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
    List<DrawOrder> findTop5ByUserIdAndPoolIdOrderByCreatedAtDesc(Long userId, Long poolId);
    List<DrawOrder> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(Long userId, LocalDateTime start, LocalDateTime end);
    List<DrawOrder> findByUserIdAndPoolIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(Long userId, Long poolId, LocalDateTime start, LocalDateTime end);
}
