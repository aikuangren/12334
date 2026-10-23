package com.fiftytalk.carddraw.repository;

import com.fiftytalk.carddraw.domain.PoolStatus;
import com.fiftytalk.carddraw.entity.CardPool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardPoolRepository extends JpaRepository<CardPool, Long> {
    Optional<CardPool> findFirstByStatusOrderByIdDesc(PoolStatus status);
    Optional<CardPool> findFirstByStatusOrderByDisplaySortNoAscIdAsc(PoolStatus status);
    List<CardPool> findByStatusOrderByDisplaySortNoAscIdAsc(PoolStatus status);
    Optional<CardPool> findByVersion(String version);
}
