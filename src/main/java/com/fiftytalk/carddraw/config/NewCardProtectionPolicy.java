package com.fiftytalk.carddraw.config;

import com.fiftytalk.carddraw.entity.NewCardProtectionRule;

import java.util.List;

public final class NewCardProtectionPolicy {
    private NewCardProtectionPolicy() {
    }

    public record RuleSpec(int minCollectionRate, int maxCollectionRate, double newCardWeightMultiplier) {
        public NewCardProtectionRule toEntity() {
            return new NewCardProtectionRule(minCollectionRate, maxCollectionRate, newCardWeightMultiplier);
        }
    }

    public static List<RuleSpec> defaultRules() {
        return List.of(
                new RuleSpec(0, 30, 2.6),
                new RuleSpec(31, 50, 2.4),
                new RuleSpec(51, 70, 2.2),
                new RuleSpec(71, 85, 2.0),
                new RuleSpec(86, 95, 1.7),
                new RuleSpec(96, 99, 1.4),
                new RuleSpec(100, 100, 1.0)
        );
    }

    public static StaticCardPoolConfig.NewCardProtectionStrategy defaultStrategy() {
        return new StaticCardPoolConfig.NewCardProtectionStrategy(
                new StaticCardPoolConfig.CollectionRateBand(51, 99),
                new StaticCardPoolConfig.CollectionRateBand(51, 99)
        );
    }
}
