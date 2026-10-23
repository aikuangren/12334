package com.fiftytalk.carddraw.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_draw_quota", uniqueConstraints = @UniqueConstraint(name = "uk_quota_user_date", columnNames = {"userId", "quotaDate"}))
public class UserDrawQuota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer freeQuota;

    @Column(nullable = false)
    private Integer extraQuota = 0;

    @Column(nullable = false)
    private Integer usedToday = 0;

    @Column(nullable = false)
    private Integer courseResetCountToday = 0;

    @Column(nullable = false)
    private Integer practiceExtraCountToday = 0;

    @Column(nullable = false)
    private LocalDate quotaDate;

    private LocalDateTime lastResetAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected UserDrawQuota() {
    }

    public UserDrawQuota(Long userId, Integer freeQuota, LocalDate quotaDate) {
        this.userId = userId;
        this.freeQuota = freeQuota;
        this.quotaDate = quotaDate;
    }

    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public int available() {
        return freeQuota + extraQuota;
    }

    public void consumeOne() {
        if (freeQuota > 0) {
            freeQuota--;
        } else {
            extraQuota--;
        }
        usedToday++;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Integer getFreeQuota() { return freeQuota; }
    public void setFreeQuota(Integer freeQuota) { this.freeQuota = freeQuota; }
    public Integer getExtraQuota() { return extraQuota; }
    public void setExtraQuota(Integer extraQuota) { this.extraQuota = extraQuota; }
    public Integer getUsedToday() { return usedToday; }
    public Integer getCourseResetCountToday() { return courseResetCountToday; }
    public void setCourseResetCountToday(Integer courseResetCountToday) { this.courseResetCountToday = courseResetCountToday; }
    public Integer getPracticeExtraCountToday() { return practiceExtraCountToday; }
    public void setPracticeExtraCountToday(Integer practiceExtraCountToday) { this.practiceExtraCountToday = practiceExtraCountToday; }
    public LocalDate getQuotaDate() { return quotaDate; }
    public LocalDateTime getLastResetAt() { return lastResetAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

