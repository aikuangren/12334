package com.fiftytalk.carddraw.entity;

import com.fiftytalk.carddraw.domain.Rarity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_card", uniqueConstraints = @UniqueConstraint(name = "uk_user_card", columnNames = {"userId", "cardId"}))
public class UserCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long cardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Rarity rarity;

    @Column(nullable = false)
    private Integer ownedCount = 1;

    private LocalDateTime firstObtainedAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected UserCard() {
    }

    public UserCard(Long userId, Long cardId, Rarity rarity) {
        this.userId = userId;
        this.cardId = cardId;
        this.rarity = rarity;
    }

    public void increase() {
        ownedCount++;
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCardId() { return cardId; }
    public Rarity getRarity() { return rarity; }
    public Integer getOwnedCount() { return ownedCount; }
    public LocalDateTime getFirstObtainedAt() { return firstObtainedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

