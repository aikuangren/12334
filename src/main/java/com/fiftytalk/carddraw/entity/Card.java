package com.fiftytalk.carddraw.entity;

import com.fiftytalk.carddraw.domain.CardStatus;
import com.fiftytalk.carddraw.domain.Rarity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "card", indexes = {
        @Index(name = "idx_card_rarity_status", columnList = "rarity,status"),
        @Index(name = "idx_card_pool_version", columnList = "poolVersion")
})
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String word;

    @Column(length = 256)
    private String meaning;

    @Column(length = 256)
    private String arabicMeaning;

    @Column(length = 512)
    private String audioUrl;

    @Column(length = 512)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Rarity rarity;

    @Column(nullable = false, length = 32)
    private String poolVersion;

    private Integer sortNo = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CardStatus status = CardStatus.ONLINE;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected Card() {
    }

    public Card(String word, String meaning, String audioUrl, String imageUrl, Rarity rarity, String poolVersion, Integer sortNo) {
        this(word, meaning, null, audioUrl, imageUrl, rarity, poolVersion, sortNo);
    }

    public Card(String word, String meaning, String arabicMeaning, String audioUrl, String imageUrl, Rarity rarity, String poolVersion, Integer sortNo) {
        this.word = word;
        this.meaning = meaning;
        this.arabicMeaning = arabicMeaning;
        this.audioUrl = audioUrl;
        this.imageUrl = imageUrl;
        this.rarity = rarity;
        this.poolVersion = poolVersion;
        this.sortNo = sortNo;
    }

    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }
    public String getArabicMeaning() { return arabicMeaning; }
    public void setArabicMeaning(String arabicMeaning) { this.arabicMeaning = arabicMeaning; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }
    public String getPoolVersion() { return poolVersion; }
    public void setPoolVersion(String poolVersion) { this.poolVersion = poolVersion; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
