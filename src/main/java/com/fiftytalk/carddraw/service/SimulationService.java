package com.fiftytalk.carddraw.service;

import com.fiftytalk.carddraw.common.BusinessException;
import com.fiftytalk.carddraw.domain.*;
import com.fiftytalk.carddraw.dto.SimulationResponse;
import com.fiftytalk.carddraw.entity.*;
import com.fiftytalk.carddraw.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimulationService {
    private final CardRepository cardRepository;
    private final CardPoolRepository poolRepository;
    private final DrawSlotRuleRepository slotRuleRepository;
    private final NewCardProtectionRuleRepository protectionRuleRepository;

    public SimulationService(
            CardRepository cardRepository,
            CardPoolRepository poolRepository,
            DrawSlotRuleRepository slotRuleRepository,
            NewCardProtectionRuleRepository protectionRuleRepository
    ) {
        this.cardRepository = cardRepository;
        this.poolRepository = poolRepository;
        this.slotRuleRepository = slotRuleRepository;
        this.protectionRuleRepository = protectionRuleRepository;
    }

    @Transactional(readOnly = true)
    public SimulationResponse simulate(int requestedPacks, String poolVersion) {
        int packs = Math.max(1, Math.min(requestedPacks, 100_000));
        CardPool pool = drawPool(poolVersion);
        List<Card> allCards = cardRepository.findByPoolVersionAndStatusOrderBySortNoAscIdAsc(pool.getVersion(), CardStatus.ONLINE);
        if (allCards.isEmpty()) {
            throw new BusinessException("لا توجد بطاقات متاحة في المجموعة الحالية");
        }

        Map<Rarity, List<Card>> cardsByRarity = allCards.stream().collect(Collectors.groupingBy(Card::getRarity));
        Map<Integer, List<DrawSlotRule>> rulesBySlot = slotRuleRepository.findByPoolIdOrderBySlotNoAscIdAsc(pool.getId()).stream()
                .collect(Collectors.groupingBy(DrawSlotRule::getSlotNo, LinkedHashMap::new, Collectors.toList()));
        List<NewCardProtectionRule> protectionRules = protectionRuleRepository.findAllByOrderByMinCollectionRateAsc();

        Set<Long> ownedCards = new HashSet<>();
        Map<Rarity, Integer> rarityCounts = new EnumMap<>(Rarity.class);
        Map<Integer, Map<Rarity, Integer>> slotRarityCounts = new LinkedHashMap<>();
        List<SimulationResponse.CollectionProgressPoint> progressPoints = new ArrayList<>();

        int invalidPackCount = 0;
        int slot4InvalidCount = 0;
        int slot5InvalidCount = 0;
        int cycleGuaranteeInvalidCount = 0;
        int duplicateGroupInvalidCount = 0;
        int sameCardMoreThanTwoInvalidCount = 0;
        int slot45DuplicateInvalidCount = 0;

        for (int packNo = 1; packNo <= packs; packNo++) {
            List<SimCard> pack = new ArrayList<>();
            for (int slotNo = 1; slotNo <= 5; slotNo++) {
                Rarity rarity = pickRarityForSlot(rulesBySlot, packNo, slotNo);
                Card card = pickCard(cardsByRarity, protectionRules, ownedCards, pack, slotNo, rarity);
                pack.add(new SimCard(slotNo, card));
                ownedCards.add(card.getId());
                rarityCounts.merge(rarity, 1, Integer::sum);
                slotRarityCounts.computeIfAbsent(slotNo, ignored -> new EnumMap<>(Rarity.class)).merge(rarity, 1, Integer::sum);
            }

            boolean slot4Valid = isAtLeast(pack.get(3).card().getRarity(), Rarity.R) && pack.get(3).card().getRarity().ordinal() <= Rarity.SSR.ordinal();
            Rarity slot5Rarity = pack.get(4).card().getRarity();
            boolean slot5Valid = DrawGuaranteePolicy.requiresHighRarity(packNo, DrawGuaranteePolicy.GUARANTEED_RARITY_SLOT_NO)
                    ? DrawGuaranteePolicy.allowedRaritiesForSlot(packNo, DrawGuaranteePolicy.GUARANTEED_RARITY_SLOT_NO).contains(slot5Rarity)
                    : isAtLeast(pack.get(4).card().getRarity(), Rarity.SR);
            Optional<Rarity> fixedRarity = DrawGuaranteePolicy.fixedRarityForSlot(packNo, DrawGuaranteePolicy.GUARANTEED_RARITY_SLOT_NO);
            boolean cycleGuarantee = fixedRarity
                    .map(rarity -> slot5Rarity == rarity)
                    .orElse(!DrawGuaranteePolicy.requiresHighRarity(packNo, DrawGuaranteePolicy.GUARANTEED_RARITY_SLOT_NO)
                            || DrawGuaranteePolicy.allowedRaritiesForSlot(packNo, DrawGuaranteePolicy.GUARANTEED_RARITY_SLOT_NO).contains(slot5Rarity));
            boolean duplicateGroupValid = duplicateGroupCount(pack) <= 1;
            boolean sameCardMaxTwo = pack.stream().collect(Collectors.groupingBy(item -> item.card().getId(), Collectors.counting()))
                    .values().stream().noneMatch(count -> count > 2);
            boolean slot45NoDuplicate = noDuplicateForSlot(pack, 4) && noDuplicateForSlot(pack, 5);

            if (!slot4Valid) slot4InvalidCount++;
            if (!slot5Valid) slot5InvalidCount++;
            if (!cycleGuarantee) cycleGuaranteeInvalidCount++;
            if (!duplicateGroupValid) duplicateGroupInvalidCount++;
            if (!sameCardMaxTwo) sameCardMoreThanTwoInvalidCount++;
            if (!slot45NoDuplicate) slot45DuplicateInvalidCount++;
            if (!slot4Valid || !slot5Valid || !cycleGuarantee || !duplicateGroupValid || !sameCardMaxTwo || !slot45NoDuplicate) {
                invalidPackCount++;
            }

            if (packNo == 1 || packNo == 7 || packNo == 14 || packNo == 21 || packNo == 28 || packNo == packs) {
                progressPoints.add(new SimulationResponse.CollectionProgressPoint(packNo, ownedCards.size(), allCards.size(), ownedCards.size() * 100 / allCards.size()));
            }
        }

        Map<String, Integer> rarityCountOutput = new LinkedHashMap<>();
        Map<String, Double> rarityRateOutput = new LinkedHashMap<>();
        for (Rarity rarity : Rarity.values()) {
            int count = rarityCounts.getOrDefault(rarity, 0);
            rarityCountOutput.put(rarity.name(), count);
            rarityRateOutput.put(rarity.name(), round(count * 100.0 / (packs * 5)));
        }

        Map<Integer, Map<String, Integer>> slotOutput = new LinkedHashMap<>();
        for (int slot = 1; slot <= 5; slot++) {
            Map<String, Integer> slotCounts = new LinkedHashMap<>();
            for (Rarity rarity : Rarity.values()) {
                slotCounts.put(rarity.name(), slotRarityCounts.getOrDefault(slot, Collections.emptyMap()).getOrDefault(rarity, 0));
            }
            slotOutput.put(slot, slotCounts);
        }

        SimulationResponse.RuleCheck ruleCheck = new SimulationResponse.RuleCheck(
                invalidPackCount,
                slot4InvalidCount,
                slot5InvalidCount,
                cycleGuaranteeInvalidCount,
                duplicateGroupInvalidCount,
                sameCardMoreThanTwoInvalidCount,
                slot45DuplicateInvalidCount
        );
        String conclusion = invalidPackCount == 0 ? "نجح تحقق القواعد: المواضع والضمان وحدود التكرار تطابق الإعدادات الحالية." : "توجد مشكلة في القواعد، يرجى مراجعة تفاصيل ruleCheck.";
        return new SimulationResponse(packs, packs * 5, ownedCards.size(), allCards.size(), rarityCountOutput, rarityRateOutput, slotOutput, ruleCheck, progressPoints, conclusion);
    }

    private List<DrawSlotRule> requiredRules(Map<Integer, List<DrawSlotRule>> rulesBySlot, int slotNo) {
        List<DrawSlotRule> rules = rulesBySlot.get(slotNo);
        if (rules == null || rules.isEmpty()) {
            throw new BusinessException("لم يتم إعداد احتمال الموضع: slotNo=" + slotNo);
        }
        return rules;
    }

    private Rarity pickRarityForSlot(Map<Integer, List<DrawSlotRule>> rulesBySlot, int packNo, int slotNo) {
        Optional<Rarity> fixedRarity = DrawGuaranteePolicy.fixedRarityForSlot(packNo, slotNo);
        if (fixedRarity.isPresent()) return fixedRarity.get();
        Set<Rarity> allowedRarities = DrawGuaranteePolicy.allowedRaritiesForSlot(packNo, slotNo);
        if (!allowedRarities.isEmpty()) {
            List<DrawSlotRule> rules = requiredRules(rulesBySlot, slotNo).stream()
                    .filter(rule -> allowedRarities.contains(rule.getRarity()))
                    .toList();
            if (rules.isEmpty()) {
                throw new BusinessException("لم يتم إعداد احتمال الموضع: slotNo=" + slotNo);
            }
            return WeightedRandomPicker.pick(rules, DrawSlotRule::getProbabilityBasisPoints).getRarity();
        }
        return WeightedRandomPicker.pick(requiredRules(rulesBySlot, slotNo), DrawSlotRule::getProbabilityBasisPoints).getRarity();
    }

    private Card pickCard(Map<Rarity, List<Card>> cardsByRarity, List<NewCardProtectionRule> protectionRules, Set<Long> ownedCards, List<SimCard> pack, int slotNo, Rarity rarity) {
        List<Card> candidates = cardsByRarity.getOrDefault(rarity, List.of());
        if (candidates.isEmpty()) {
            throw new BusinessException("مجموعة هذه الندرة فارغة: " + rarity);
        }
        List<Card> validCandidates = candidates.stream()
                .filter(card -> canAppear(card, pack, slotNo))
                .toList();
        if (validCandidates.isEmpty()) {
            validCandidates = candidates;
        }
        double multiplier = multiplier(cardsByRarity, protectionRules, ownedCards, rarity);
        return WeightedRandomPicker.pick(validCandidates, card -> ownedCards.contains(card.getId()) ? 1.0 : multiplier);
    }

    private boolean canAppear(Card card, List<SimCard> pack, int slotNo) {
        boolean alreadyInPack = pack.stream().anyMatch(item -> item.card().getId().equals(card.getId()));
        if (slotNo >= 4 && alreadyInPack) {
            return false;
        }
        long sameCardCount = pack.stream().filter(item -> item.card().getId().equals(card.getId())).count();
        if (sameCardCount >= 1) {
            return duplicateGroupCount(pack) == 0;
        }
        return true;
    }

    private double multiplier(Map<Rarity, List<Card>> cardsByRarity, List<NewCardProtectionRule> rules, Set<Long> ownedCards, Rarity rarity) {
        List<Card> totalCards = cardsByRarity.getOrDefault(rarity, List.of());
        if (totalCards.isEmpty()) {
            return 1.0;
        }
        long obtained = totalCards.stream().filter(card -> ownedCards.contains(card.getId())).count();
        int rate = (int) (obtained * 100 / totalCards.size());
        return rules.stream()
                .filter(rule -> rate >= rule.getMinCollectionRate() && rate <= rule.getMaxCollectionRate())
                .findFirst()
                .map(NewCardProtectionRule::getNewCardWeightMultiplier)
                .orElse(1.0);
    }

    private boolean isAtLeast(Rarity actual, Rarity floor) {
        return actual.ordinal() >= floor.ordinal();
    }

    private int duplicateGroupCount(List<SimCard> pack) {
        return (int) pack.stream()
                .collect(Collectors.groupingBy(item -> item.card().getId(), Collectors.counting()))
                .values().stream()
                .filter(count -> count >= 2)
                .count();
    }

    private boolean noDuplicateForSlot(List<SimCard> pack, int slotNo) {
        Long cardId = pack.stream().filter(item -> item.slotNo() == slotNo).findFirst().map(item -> item.card().getId()).orElse(null);
        return cardId == null || pack.stream().filter(item -> item.card().getId().equals(cardId)).count() == 1;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private CardPool drawPool(String poolVersion) {
        CardPool pool;
        if (poolVersion != null && !poolVersion.isBlank()) {
            pool = poolRepository.findByVersion(poolVersion.trim())
                    .orElseThrow(() -> new BusinessException("مجموعة البطاقات غير موجودة: " + poolVersion.trim()));
        } else {
            pool = poolRepository.findFirstByStatusOrderByDisplaySortNoAscIdAsc(PoolStatus.ONLINE)
                    .orElseThrow(() -> new BusinessException("لا توجد مجموعة بطاقات متاحة حاليًا"));
        }
        if (pool.getStatus() != PoolStatus.ONLINE) {
            throw new BusinessException("مجموعة البطاقات غير متاحة: " + pool.getVersion());
        }
        return pool;
    }

    private record SimCard(int slotNo, Card card) {
    }
}
