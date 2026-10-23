package com.fiftytalk.carddraw.entity;

import com.fiftytalk.carddraw.domain.CostType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "draw_order", indexes = @Index(name = "idx_order_user_time", columnList = "userId,createdAt"))
public class DrawOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long poolId;

    @Column(length = 64)
    private String requestId;

    @Column(nullable = false)
    private Integer drawNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CostType costType;

    @Column(nullable = false)
    private Boolean hasNewCard;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected DrawOrder() {
    }

    public DrawOrder(Long userId, Long poolId, Integer drawNo, CostType costType, Boolean hasNewCard) {
        this(userId, poolId, null, drawNo, costType, hasNewCard);
    }

    public DrawOrder(Long userId, Long poolId, String requestId, Integer drawNo, CostType costType, Boolean hasNewCard) {
        this.userId = userId;
        this.poolId = poolId;
        this.requestId = requestId;
        this.drawNo = drawNo;
        this.costType = costType;
        this.hasNewCard = hasNewCard;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getPoolId() { return poolId; }
    public String getRequestId() { return requestId; }
    public Integer getDrawNo() { return drawNo; }
    public CostType getCostType() { return costType; }
    public Boolean getHasNewCard() { return hasNewCard; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
