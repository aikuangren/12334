package com.fiftytalk.carddraw.repository;

import com.fiftytalk.carddraw.domain.ActivityStatus;
import com.fiftytalk.carddraw.entity.DrawActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DrawActivityRepository extends JpaRepository<DrawActivity, Long> {
    Optional<DrawActivity> findFirstByStatusOrderByIdDesc(ActivityStatus status);
}

