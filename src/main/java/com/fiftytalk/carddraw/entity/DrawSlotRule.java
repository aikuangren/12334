package com.fiftytalk.carddraw.entity;

import com.fiftytalk.carddraw.domain.Rarity;
import jakarta.persistence.*;

@Entity
@Table(name = "draw_slot_rule", indexes = @Index(name = "idx_slot_pool_slot", columnList = "poolId,slotNo"))
public class DrawSlotRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long poolId;

    @Column(nullable = false)
    private Integer slotNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Rarity rarity;

    @Column(nullable = false)
    private Integer probabilityBasisPoints;

    protected DrawSlotRule() {
    }

    public DrawSlotRule(Long poolId, Integer slotNo, Rarity rarity, Integer probabilityBasisPoints) {
        this.poolId = poolId;
        this.slotNo = slotNo;
        this.rarity = rarity;
        this.probabilityBasisPoints = probabilityBasisPoints;
    }

    public Long getId() { return id; }
    public Long getPoolId() { return poolId; }
    public Integer getSlotNo() { return slotNo; }
    public Rarity getRarity() { return rarity; }
    public Integer getProbabilityBasisPoints() { return probabilityBasisPoints; }
}

