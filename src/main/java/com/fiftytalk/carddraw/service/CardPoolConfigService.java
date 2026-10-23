package com.fiftytalk.carddraw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiftytalk.carddraw.config.StaticCardPoolConfig;
import com.fiftytalk.carddraw.domain.CardStatus;
import com.fiftytalk.carddraw.domain.PoolStatus;
import com.fiftytalk.carddraw.domain.Rarity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CardPoolConfigService {
    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final int EXPECTED_SLOT_TOTAL_BASIS_POINTS = 10000;

    private final Map<String, StaticCardPoolConfig> configsByVersion;

    public CardPoolConfigService(
            ObjectMapper objectMapper,
            ResourcePatternResolver resourcePatternResolver,
            @Value("${card-draw.static-pool-config-location:classpath*:card-pool/*.json}") String configLocation
    ) throws IOException {
        Resource[] resources = resourcePatternResolver.getResources(configLocation);
        if (resources.length == 0) {
            throw new IllegalStateException("لم يتم العثور على إعدادات مجموعة البطاقات الثابتة: " + configLocation);
        }

        Map<String, StaticCardPoolConfig> loadedConfigs = new LinkedHashMap<>();
        for (Resource resource : sorted(resources)) {
            StaticCardPoolConfig config = objectMapper.readValue(resource.getInputStream(), StaticCardPoolConfig.class);
            validate(config, resource.getDescription());
            String version = config.pool().version();
            if (loadedConfigs.putIfAbsent(version, config) != null) {
                throw new IllegalStateException("إصدار مجموعة البطاقات الثابتة مكرر: " + version);
            }
        }
        configsByVersion = Map.copyOf(loadedConfigs);
    }

    public List<StaticCardPoolConfig> allPools() {
        return configsByVersion.values().stream()
                .sorted(Comparator.comparing(
                        (StaticCardPoolConfig config) -> config.pool().displaySortNo(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    public List<StaticCardPoolConfig> onlinePools() {
        return allPools().stream()
                .filter(config -> config.pool().status() == PoolStatus.ONLINE)
                .toList();
    }

    public Optional<StaticCardPoolConfig> findByVersion(String version) {
        if (version == null || version.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(configsByVersion.get(version.trim()));
    }

    public StaticCardPoolConfig requirePool(String version) {
        return findByVersion(version)
                .orElseThrow(() -> new IllegalArgumentException("مجموعة البطاقات الثابتة غير موجودة: " + version));
    }

    public StaticCardPoolConfig defaultOnlinePool() {
        return onlinePools().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("لا توجد مجموعة بطاقات ثابتة متاحة حاليًا"));
    }

    public List<StaticCardPoolConfig.Card> onlineCards(String version) {
        return requirePool(version).cards().stream()
                .filter(card -> card.status() == CardStatus.ONLINE)
                .sorted(Comparator.comparing(StaticCardPoolConfig.Card::sortNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(StaticCardPoolConfig.Card::cardId))
                .toList();
    }

    public List<StaticCardPoolConfig.Card> onlineCardsByRarity(String version, Rarity rarity) {
        return onlineCards(version).stream()
                .filter(card -> card.rarity() == rarity)
                .toList();
    }

    public List<StaticCardPoolConfig.SlotRule> slotRules(String version, int slotNo) {
        return requirePool(version).slotRules().stream()
                .filter(rule -> Objects.equals(rule.slotNo(), slotNo))
                .toList();
    }

    public List<StaticCardPoolConfig.NewCardProtectionRule> newCardProtectionRules(String version) {
        return requirePool(version).newCardProtectionRules().stream()
                .sorted(Comparator.comparing(StaticCardPoolConfig.NewCardProtectionRule::minCollectionRate))
                .toList();
    }

    private List<Resource> sorted(Resource[] resources) {
        List<Resource> sorted = new ArrayList<>(List.of(resources));
        sorted.sort(Comparator.comparing(Resource::getDescription));
        return sorted;
    }

    private void validate(StaticCardPoolConfig config, String source) {
        if (config.schemaVersion() == null || config.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalStateException(source + " schemaVersion يدعم فقط " + SUPPORTED_SCHEMA_VERSION);
        }
        if (config.pool() == null || isBlank(config.pool().version()) || isBlank(config.pool().name())) {
            throw new IllegalStateException(source + " لا يمكن ترك version/name لمجموعة البطاقات فارغًا");
        }
        if (config.quota() == null || config.quota().dailyFreeQuota() == null || config.quota().dailyFreeQuota() < 0) {
            throw new IllegalStateException(source + " إعداد dailyFreeQuota غير صالح");
        }
        if (config.cards() == null || config.cards().isEmpty()) {
            throw new IllegalStateException(source + " لا يمكن أن تكون قائمة البطاقات فارغة");
        }
        if (config.slotRules() == null || config.slotRules().isEmpty()) {
            throw new IllegalStateException(source + " لا يمكن ترك إعداد احتمالات المواضع فارغًا");
        }
        if (config.newCardProtectionRules() == null || config.newCardProtectionRules().isEmpty()) {
            throw new IllegalStateException(source + " لا يمكن ترك إعداد حماية البطاقة الجديدة فارغًا");
        }
        if (config.newCardProtectionStrategy() == null) {
            throw new IllegalStateException(source + " لا يمكن ترك إعداد استراتيجية حماية البطاقة الجديدة فارغًا");
        }

        validateCards(config, source);
        validateSlotRules(config, source);
        validateNewCardProtectionRules(config, source);
        validateNewCardProtectionStrategy(config, source);
    }

    private void validateCards(StaticCardPoolConfig config, String source) {
        Set<Long> cardIds = config.cards().stream()
                .map(StaticCardPoolConfig.Card::cardId)
                .collect(Collectors.toSet());
        if (cardIds.size() != config.cards().size()) {
            throw new IllegalStateException(source + " يوجد cardId مكرر");
        }

        for (StaticCardPoolConfig.Card card : config.cards()) {
            if (card.cardId() == null || isBlank(card.word()) || card.rarity() == null || card.status() == null) {
                throw new IllegalStateException(source + " حقول البطاقة غير مكتملة، cardId=" + card.cardId());
            }
        }

        Map<Rarity, Long> actualCounts = config.cards().stream()
                .collect(Collectors.groupingBy(StaticCardPoolConfig.Card::rarity, () -> new EnumMap<>(Rarity.class), Collectors.counting()));
        if (config.rarityCounts() != null) {
            for (Rarity rarity : Rarity.values()) {
                int expected = config.rarityCounts().getOrDefault(rarity, 0);
                int actual = actualCounts.getOrDefault(rarity, 0L).intValue();
                if (expected != actual) {
                    throw new IllegalStateException(source + " عدد الندرة غير متطابق: " + rarity + " expected=" + expected + ", actual=" + actual);
                }
            }
        }
    }

    private void validateSlotRules(StaticCardPoolConfig config, String source) {
        Map<Integer, Integer> slotTotals = config.slotRules().stream()
                .collect(Collectors.groupingBy(
                        StaticCardPoolConfig.SlotRule::slotNo,
                        Collectors.summingInt(StaticCardPoolConfig.SlotRule::probabilityBasisPoints)
                ));
        for (int slotNo = 1; slotNo <= 5; slotNo++) {
            int total = slotTotals.getOrDefault(slotNo, 0);
            if (total != EXPECTED_SLOT_TOTAL_BASIS_POINTS) {
                throw new IllegalStateException(source + " رقم " + slotNo + " يجب أن يكون إجمالي احتمال الموضع 10000، الحالي " + total);
            }
        }

        Map<Rarity, List<StaticCardPoolConfig.Card>> onlineCardsByRarity = config.cards().stream()
                .filter(card -> card.status() == CardStatus.ONLINE)
                .collect(Collectors.groupingBy(StaticCardPoolConfig.Card::rarity));
        for (StaticCardPoolConfig.SlotRule rule : config.slotRules()) {
            if (rule.slotNo() == null || rule.slotNo() < 1 || rule.slotNo() > 5 || rule.rarity() == null || rule.probabilityBasisPoints() == null || rule.probabilityBasisPoints() < 0) {
                throw new IllegalStateException(source + " إعداد احتمال الموضع غير صالح");
            }
            if (rule.probabilityBasisPoints() > 0 && onlineCardsByRarity.getOrDefault(rule.rarity(), List.of()).isEmpty()) {
                throw new IllegalStateException(source + " إعداد الاحتمال يشير إلى ندرة فارغة: " + rule.rarity());
            }
        }
    }

    private void validateNewCardProtectionRules(StaticCardPoolConfig config, String source) {
        for (StaticCardPoolConfig.NewCardProtectionRule rule : config.newCardProtectionRules()) {
            if (rule.minCollectionRate() == null || rule.maxCollectionRate() == null || rule.newCardWeightMultiplier() == null) {
                throw new IllegalStateException(source + " لا يمكن ترك حقول حماية البطاقة الجديدة فارغة");
            }
            if (rule.minCollectionRate() < 0 || rule.maxCollectionRate() > 100 || rule.minCollectionRate() > rule.maxCollectionRate()) {
                throw new IllegalStateException(source + " نطاق معدل جمع حماية البطاقة الجديدة غير صالح");
            }
            if (rule.newCardWeightMultiplier() <= 0) {
                throw new IllegalStateException(source + " يجب أن يكون معامل حماية البطاقة الجديدة أكبر من 0");
            }
        }
        validateContinuousProtectionRules(config.newCardProtectionRules(), source);
    }

    private void validateNewCardProtectionStrategy(StaticCardPoolConfig config, String source) {
        StaticCardPoolConfig.CollectionRateBand consecutive = config.newCardProtectionStrategy().consecutiveNoNewProtection();
        StaticCardPoolConfig.CollectionRateBand daily = config.newCardProtectionStrategy().dailyNewCardProtection();
        validateCollectionRateBand(consecutive, source);
        validateCollectionRateBand(daily, source);
        validateBandCoveredByProtectionRules(consecutive, config.newCardProtectionRules(), source);
        validateBandCoveredByProtectionRules(daily, config.newCardProtectionRules(), source);
    }

    private void validateCollectionRateBand(StaticCardPoolConfig.CollectionRateBand band, String source) {
        if (band == null || band.minCollectionRate() == null || band.maxCollectionRate() == null) {
            throw new IllegalStateException(source + " لا يمكن ترك نطاق استراتيجية حماية البطاقة الجديدة فارغًا");
        }
        if (band.minCollectionRate() < 0 || band.maxCollectionRate() > 100 || band.minCollectionRate() > band.maxCollectionRate()) {
            throw new IllegalStateException(source + " نطاق استراتيجية حماية البطاقة الجديدة غير صالح");
        }
    }

    private void validateContinuousProtectionRules(List<StaticCardPoolConfig.NewCardProtectionRule> rules, String source) {
        List<StaticCardPoolConfig.NewCardProtectionRule> sortedRules = rules.stream()
                .sorted(Comparator.comparing(StaticCardPoolConfig.NewCardProtectionRule::minCollectionRate))
                .toList();
        int expectedMin = 0;
        for (StaticCardPoolConfig.NewCardProtectionRule rule : sortedRules) {
            if (rule.minCollectionRate() != expectedMin) {
                throw new IllegalStateException(source + " 新卡权重区间存在重叠或空洞");
            }
            expectedMin = rule.maxCollectionRate() + 1;
        }
        if (expectedMin != 101) {
            throw new IllegalStateException(source + " 新卡权重区间必须覆盖 0-100");
        }
    }

    private void validateBandCoveredByProtectionRules(
            StaticCardPoolConfig.CollectionRateBand band,
            List<StaticCardPoolConfig.NewCardProtectionRule> rules,
            String source
    ) {
        int cursor = band.minCollectionRate();
        List<StaticCardPoolConfig.NewCardProtectionRule> sortedRules = rules.stream()
                .sorted(Comparator.comparing(StaticCardPoolConfig.NewCardProtectionRule::minCollectionRate))
                .toList();
        for (StaticCardPoolConfig.NewCardProtectionRule rule : sortedRules) {
            if (rule.maxCollectionRate() < cursor) {
                continue;
            }
            if (rule.minCollectionRate() > cursor) {
                break;
            }
            cursor = rule.maxCollectionRate() + 1;
            if (cursor > band.maxCollectionRate()) {
                return;
            }
        }
        throw new IllegalStateException(source + " 新卡保护策略区间未被权重区间完整覆盖");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
