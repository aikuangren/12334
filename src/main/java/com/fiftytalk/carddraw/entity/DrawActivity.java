package com.fiftytalk.carddraw.entity;

import com.fiftytalk.carddraw.domain.ActivityStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "draw_activity")
public class DrawActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false)
    private Long poolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ActivityStatus status = ActivityStatus.DRAFT;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected DrawActivity() {
    }

    public DrawActivity(String name, Long poolId, ActivityStatus status) {
        this.name = name;
        this.poolId = poolId;
        this.status = status;
    }

    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getPoolId() { return poolId; }
    public void setPoolId(Long poolId) { this.poolId = poolId; }
    public ActivityStatus getStatus() { return status; }
    public void setStatus(ActivityStatus status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

