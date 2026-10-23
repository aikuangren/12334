package com.fiftytalk.carddraw.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "new_card_protection_rule")
public class NewCardProtectionRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer minCollectionRate;

    @Column(nullable = false)
    private Integer maxCollectionRate;

    @Column(nullable = false)
    private Double newCardWeightMultiplier;

    protected NewCardProtectionRule() {
    }

    public NewCardProtectionRule(Integer minCollectionRate, Integer maxCollectionRate, Double newCardWeightMultiplier) {
        this.minCollectionRate = minCollectionRate;
        this.maxCollectionRate = maxCollectionRate;
        this.newCardWeightMultiplier = newCardWeightMultiplier;
    }

    public Long getId() { return id; }
    public Integer getMinCollectionRate() { return minCollectionRate; }
    public Integer getMaxCollectionRate() { return maxCollectionRate; }
    public Double getNewCardWeightMultiplier() { return newCardWeightMultiplier; }
}

