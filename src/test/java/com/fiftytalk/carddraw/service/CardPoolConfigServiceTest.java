package com.fiftytalk.carddraw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiftytalk.carddraw.config.DefaultDrawSlotRules;
import com.fiftytalk.carddraw.config.NewCardProtectionPolicy;
import com.fiftytalk.carddraw.config.StaticCardPoolConfig;
import com.fiftytalk.carddraw.domain.PoolStatus;
import com.fiftytalk.carddraw.domain.Rarity;
import com.fiftytalk.carddraw.entity.DrawSlotRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardPoolConfigServiceTest {

    @Test
    void loadsAndValidatesStaticPoolConfig() throws IOException {
        CardPoolConfigService service = new CardPoolConfigService(
                new ObjectMapper(),
                new PathMatchingResourcePatternResolver(),
                "classpath*:card-pool/*.json"
        );

        StaticCardPoolConfig config = service.requirePool("v1");
        StaticCardPoolConfig animalConfig = service.requirePool("v2");

        assertThat(config.pool().status()).isEqualTo(PoolStatus.ONLINE);
        assertThat(animalConfig.pool().status()).isEqualTo(PoolStatus.ONLINE);
        assertThat(service.onlinePools()).hasSize(3);
        assertThat(service.onlinePools()).extracting(pool -> pool.pool().version()).containsExactly("v3", "v2", "v1");
        assertThat(service.onlineCards("v1")).hasSize(120);
        assertThat(service.onlineCards("v2")).hasSize(80);
        assertThat(service.onlineCardsByRarity("v1", Rarity.XR)).hasSize(2);
        assertThat(service.onlineCardsByRarity("v1", Rarity.LR)).hasSize(2);
        assertThat(service.onlineCardsByRarity("v1", Rarity.UR)).hasSize(7);
        assertThat(service.onlineCardsByRarity("v1", Rarity.SSR)).hasSize(14);
        assertThat(service.onlineCardsByRarity("v1", Rarity.SR)).hasSize(35);
        assertThat(service.onlineCardsByRarity("v1", Rarity.R)).hasSize(38);
        assertThat(service.onlineCardsByRarity("v1", Rarity.N)).hasSize(22);
        assertThat(service.onlineCardsByRarity("v2", Rarity.XR)).hasSize(8);
        assertThat(service.onlineCardsByRarity("v2", Rarity.LR)).hasSize(8);
        assertThat(service.onlineCardsByRarity("v2", Rarity.UR)).hasSize(8);
        assertThat(service.onlineCardsByRarity("v2", Rarity.SSR)).hasSize(12);
        assertThat(service.onlineCardsByRarity("v2", Rarity.SR)).hasSize(12);
        assertThat(service.onlineCardsByRarity("v2", Rarity.R)).hasSize(32);
        assertThat(service.onlineCardsByRarity("v2", Rarity.N)).isEmpty();

        Map<Rarity, Long> actualCounts = service.onlineCards("v1").stream()
                .collect(Collectors.groupingBy(StaticCardPoolConfig.Card::rarity, Collectors.counting()));
        config.rarityCounts().forEach((rarity, expectedCount) ->
                assertThat(actualCounts.getOrDefault(rarity, 0L)).isEqualTo(expectedCount.longValue())
        );

        for (int slotNo = 1; slotNo <= 5; slotNo++) {
            int total = service.slotRules("v1", slotNo).stream()
                    .mapToInt(StaticCardPoolConfig.SlotRule::probabilityBasisPoints)
                    .sum();
            assertThat(total).isEqualTo(10000);
            int animalTotal = service.slotRules("v2", slotNo).stream()
                    .mapToInt(StaticCardPoolConfig.SlotRule::probabilityBasisPoints)
                    .sum();
            assertThat(animalTotal).isEqualTo(10000);
        }

        assertThat(staticSlotRuleMatrix(config.slotRules()))
                .isEqualTo(defaultSlotRuleMatrix());
        assertThat(protectionRuleSpecs(config.newCardProtectionRules()))
                .isEqualTo(defaultProtectionRuleSpecs());
        assertThat(protectionRuleSpecs(animalConfig.newCardProtectionRules()))
                .isEqualTo(defaultProtectionRuleSpecs());
        assertThat(protectionRuleSpecs(loadStaticConfig("static/assets/h5/config/card-pool-v1.json").newCardProtectionRules()))
                .isEqualTo(defaultProtectionRuleSpecs());
        assertThat(protectionRuleSpecs(loadStaticConfig("static/assets/h5/config/card-pool-v2.json").newCardProtectionRules()))
                .isEqualTo(defaultProtectionRuleSpecs());
        List<String> expectedStrategySpec = strategySpec(config.newCardProtectionStrategy());
        assertThat(strategySpec(animalConfig.newCardProtectionStrategy()))
                .isEqualTo(expectedStrategySpec);
        assertThat(strategySpec(loadStaticConfig("static/assets/h5/config/card-pool-v1.json").newCardProtectionStrategy()))
                .isEqualTo(expectedStrategySpec);
        assertThat(strategySpec(loadStaticConfig("static/assets/h5/config/card-pool-v2.json").newCardProtectionStrategy()))
                .isEqualTo(expectedStrategySpec);
        Map<Rarity, Integer> animalSlot5Rules = service.slotRules("v2", 5).stream()
                .collect(Collectors.toMap(StaticCardPoolConfig.SlotRule::rarity, StaticCardPoolConfig.SlotRule::probabilityBasisPoints));
        assertThat(animalSlot5Rules).containsEntry(Rarity.SR, 8000);
        assertThat(animalSlot5Rules).containsEntry(Rarity.SSR, 1900);
        assertThat(animalSlot5Rules).containsEntry(Rarity.UR, 50);
        assertThat(animalSlot5Rules).containsEntry(Rarity.LR, 30);
        assertThat(animalSlot5Rules).containsEntry(Rarity.XR, 20);
    }

    @Test
    void oceanPoolIsDefaultAndMatchesH5Config() throws IOException {
        CardPoolConfigService service = new CardPoolConfigService(
                new ObjectMapper(), new PathMatchingResourcePatternResolver(), "classpath*:card-pool/*.json"
        );
        StaticCardPoolConfig ocean = service.requirePool("v3");
        assertThat(service.defaultOnlinePool()).isEqualTo(ocean);
        assertThat(ocean).isEqualTo(loadStaticConfig("static/assets/h5/config/card-pool-v3.json"));
        assertThat(service.onlineCards("v3")).hasSize(120);
        assertThat(ocean.rarityCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(
                Rarity.R, 48, Rarity.SR, 18, Rarity.SSR, 18,
                Rarity.UR, 12, Rarity.LR, 12, Rarity.XR, 12
        ));
        assertThat(ocean.slotRules()).isEqualTo(service.requirePool("v2").slotRules());
        assertThat(ocean.newCardProtectionRules()).isEqualTo(service.requirePool("v2").newCardProtectionRules());
        assertThat(ocean.newCardProtectionStrategy()).isEqualTo(service.requirePool("v2").newCardProtectionStrategy());
        assertThat(ocean.cards().stream().filter(card -> card.word().equals("manta ray")))
                .extracting(StaticCardPoolConfig.Card::rarity).containsExactly(Rarity.SSR, Rarity.UR);
    }

    @Test
    void allowsOverlappingNewCardProtectionStrategyWhenRulesCoverBands(@TempDir Path tempDir) throws IOException {
        StaticCardPoolConfig baseConfig = loadStaticConfig("card-pool/v1.json");
        StaticCardPoolConfig config = copyWithStrategy(
                baseConfig,
                new StaticCardPoolConfig.NewCardProtectionStrategy(
                        new StaticCardPoolConfig.CollectionRateBand(51, 99),
                        new StaticCardPoolConfig.CollectionRateBand(95, 99)
                )
        );

        Path configPath = writeConfig(tempDir, config);

        CardPoolConfigService service = new CardPoolConfigService(
                new ObjectMapper(),
                new PathMatchingResourcePatternResolver(),
                "file:" + configPath.toAbsolutePath()
        );

        assertThat(strategySpec(service.requirePool("v1").newCardProtectionStrategy()))
                .containsExactly("51-99", "95-99");
    }

    @Test
    void rejectsNewCardProtectionStrategyNotCoveredByWeightRules(@TempDir Path tempDir) throws IOException {
        StaticCardPoolConfig baseConfig = loadStaticConfig("card-pool/v1.json");
        List<StaticCardPoolConfig.NewCardProtectionRule> incompleteRules = baseConfig.newCardProtectionRules().stream()
                .filter(rule -> rule.minCollectionRate() < 96)
                .toList();
        StaticCardPoolConfig invalidConfig = copyWithProtectionRules(baseConfig, incompleteRules);

        Path configPath = writeConfig(tempDir, invalidConfig);

        assertThatThrownBy(() -> new CardPoolConfigService(
                new ObjectMapper(),
                new PathMatchingResourcePatternResolver(),
                "file:" + configPath.toAbsolutePath()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("新卡权重区间必须覆盖 0-100");
    }

    private Map<Integer, Map<Rarity, Integer>> staticSlotRuleMatrix(List<StaticCardPoolConfig.SlotRule> rules) {
        return rules.stream().collect(Collectors.groupingBy(
                StaticCardPoolConfig.SlotRule::slotNo,
                Collectors.toMap(StaticCardPoolConfig.SlotRule::rarity, StaticCardPoolConfig.SlotRule::probabilityBasisPoints)
        ));
    }

    private Map<Integer, Map<Rarity, Integer>> defaultSlotRuleMatrix() {
        return DefaultDrawSlotRules.forPool(0L).stream().collect(Collectors.groupingBy(
                DrawSlotRule::getSlotNo,
                Collectors.toMap(DrawSlotRule::getRarity, DrawSlotRule::getProbabilityBasisPoints)
        ));
    }

    private StaticCardPoolConfig loadStaticConfig(String path) throws IOException {
        return new ObjectMapper().readValue(
                new PathMatchingResourcePatternResolver().getResource("classpath:" + path).getInputStream(),
                StaticCardPoolConfig.class
        );
    }

    private Path writeConfig(Path tempDir, StaticCardPoolConfig config) throws IOException {
        Path configPath = tempDir.resolve("card-pool.json");
        new ObjectMapper().writeValue(configPath.toFile(), config);
        return configPath;
    }

    private StaticCardPoolConfig copyWithStrategy(
            StaticCardPoolConfig config,
            StaticCardPoolConfig.NewCardProtectionStrategy strategy
    ) {
        return new StaticCardPoolConfig(
                config.schemaVersion(),
                config.pool(),
                config.quota(),
                config.assets(),
                config.rarities(),
                config.rarityCounts(),
                config.slotRules(),
                config.newCardProtectionRules(),
                strategy,
                config.cards()
        );
    }

    private StaticCardPoolConfig copyWithProtectionRules(
            StaticCardPoolConfig config,
            List<StaticCardPoolConfig.NewCardProtectionRule> protectionRules
    ) {
        return new StaticCardPoolConfig(
                config.schemaVersion(),
                config.pool(),
                config.quota(),
                config.assets(),
                config.rarities(),
                config.rarityCounts(),
                config.slotRules(),
                protectionRules,
                config.newCardProtectionStrategy(),
                config.cards()
        );
    }

    private List<String> protectionRuleSpecs(List<StaticCardPoolConfig.NewCardProtectionRule> rules) {
        return rules.stream()
                .map(rule -> rule.minCollectionRate() + "-" + rule.maxCollectionRate() + ":" + rule.newCardWeightMultiplier())
                .toList();
    }

    private List<String> defaultProtectionRuleSpecs() {
        return NewCardProtectionPolicy.defaultRules().stream()
                .map(rule -> rule.minCollectionRate() + "-" + rule.maxCollectionRate() + ":" + rule.newCardWeightMultiplier())
                .toList();
    }

    private List<String> strategySpec(StaticCardPoolConfig.NewCardProtectionStrategy strategy) {
        return List.of(
                bandSpec(strategy.consecutiveNoNewProtection()),
                bandSpec(strategy.dailyNewCardProtection())
        );
    }

    private String bandSpec(StaticCardPoolConfig.CollectionRateBand band) {
        return band.minCollectionRate() + "-" + band.maxCollectionRate();
    }
}
