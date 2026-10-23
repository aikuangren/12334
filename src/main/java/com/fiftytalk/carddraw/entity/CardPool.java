package com.fiftytalk.carddraw.entity;

import com.fiftytalk.carddraw.domain.PoolStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_pool")
public class CardPool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String version;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 512)
    private String packImageUrl;

    @Column(nullable = false)
    private Integer displaySortNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PoolStatus status = PoolStatus.DRAFT;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected CardPool() {
    }

    public CardPool(String version, String name, PoolStatus status) {
        this.version = version;
        this.name = name;
        this.status = status;
    }

    public CardPool(String version, String name, String packImageUrl, Integer displaySortNo, PoolStatus status) {
        this.version = version;
        this.name = name;
        this.packImageUrl = packImageUrl;
        this.displaySortNo = displaySortNo == null || displaySortNo < 1 ? 1 : displaySortNo;
        this.status = status;
    }

    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPackImageUrl() { return packImageUrl; }
    public void setPackImageUrl(String packImageUrl) { this.packImageUrl = packImageUrl; }
    public Integer getDisplaySortNo() { return displaySortNo; }
    public void setDisplaySortNo(Integer displaySortNo) { this.displaySortNo = displaySortNo == null || displaySortNo < 1 ? 1 : displaySortNo; }
    public PoolStatus getStatus() { return status; }
    public void setStatus(PoolStatus status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
