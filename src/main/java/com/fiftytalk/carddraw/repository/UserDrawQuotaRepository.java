package com.fiftytalk.carddraw.repository;

import com.fiftytalk.carddraw.entity.UserDrawQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UserDrawQuotaRepository extends JpaRepository<UserDrawQuota, Long> {
    Optional<UserDrawQuota> findByUserIdAndQuotaDate(Long userId, LocalDate quotaDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quota from UserDrawQuota quota where quota.userId = :userId and quota.quotaDate = :quotaDate")
    Optional<UserDrawQuota> findByUserIdAndQuotaDateForUpdate(@Param("userId") Long userId, @Param("quotaDate") LocalDate quotaDate);

    @Modifying
    @Query(value = """
            insert ignore into user_draw_quota (
                user_id,
                free_quota,
                extra_quota,
                used_today,
                course_reset_count_today,
                practice_extra_count_today,
                quota_date,
                last_reset_at,
                updated_at
            ) values (
                :userId,
                :freeQuota,
                0,
                0,
                0,
                0,
                :quotaDate,
                current_timestamp,
                current_timestamp
            )
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("freeQuota") Integer freeQuota, @Param("quotaDate") LocalDate quotaDate);
}
