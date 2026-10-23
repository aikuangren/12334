package com.fiftytalk.carddraw.entity;

import com.fiftytalk.carddraw.domain.Rarity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "draw_order_card", indexes = @Index(name = "idx_order_card_order", columnList = "orderId"))
public class DrawOrderCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer slotNo;

    @Column(nullable = false)
    private Long cardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Rarity rarity;

    @Column(nullable = false)
    private Boolean isNew;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected DrawOrderCard() {
    }

    public DrawOrderCard(Long orderId, Long userId, Integer slotNo, Long cardId, Rarity rarity, Boolean isNew) {
        this.orderId = orderId;
        this.userId = userId;
        this.slotNo = slotNo;
        this.cardId = cardId;
        this.rarity = rarity;
        this.isNew = isNew;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Integer getSlotNo() { return slotNo; }
    public Long getCardId() { return cardId; }
    public Rarity getRarity() { return rarity; }
    public Boolean getIsNew() { return isNew; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

