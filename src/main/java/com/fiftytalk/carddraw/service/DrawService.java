package com.fiftytalk.carddraw.service;

import com.fiftytalk.carddraw.common.BusinessException;
import com.fiftytalk.carddraw.config.NewCardProtectionPolicy;
import com.fiftytalk.carddraw.config.StaticCardPoolConfig;
import com.fiftytalk.carddraw.domain.*;
import com.fiftytalk.carddraw.dto.*;
import com.fiftytalk.carddraw.entity.*;
import com.fiftytalk.carddraw.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DrawService {
    private static final Logger log = LoggerFactory.getLogger(DrawService.class);

    private final CardRepository cardRepository;
    private final CardPoolRepository poolRepository;
    private final DrawActivityRepository activityRepository;
    private final DrawSlotRuleRepository slotRuleRepository;
    private final NewCardProtectionRuleRepository protectionRuleRepository;
    private final UserCardRepository userCardRepository;
    private final DrawOrderRepository orderRepository;
    private final DrawOrderCardRepository orderCardRepository;
    private final QuotaService quotaService;
    private final BusinessClock clock;
    private final CardPoolConfigService cardPoolConfigService;

    public DrawService(
            CardRepository cardRepository,
            CardPoolRepository poolRepository,
            DrawActivityRepository activityRepository,
            DrawSlotRuleRepository slotRuleRepository,
            NewCardProtectionRuleRepository protectionRuleRepository,
            UserCardRepository userCardRepository,
            DrawOrderRepository orderRepository,
            DrawOrderCardRepository orderCardRepository,
            QuotaService quotaService,
            BusinessClock clock,
            CardPoolConfigService cardPoolConfigService
    ) {
        this.cardRepository = cardRepository;
        this.poolRepository = poolRepository;
        this.activityRepository = activityRepository;
        this.slotRuleRepository = slotRuleRepository;
        this.protectionRuleRepository = protectionRuleRepository;
        this.userCardRepository = userCardRepository;
        this.orderRepository = orderRepository;
        this.orderCardRepository = orderCardRepository;
        this.quotaService = quotaService;
        this.clock = clock;
        this.cardPoolConfigService = cardPoolConfigService;
    }

    @Transactional
    public ActivityResponse activity(Long userId, String poolVersion) {
        CardPool pool = drawPool(poolVersion);
        UserDrawQuota quota = quotaService.currentQuota(userId);
        String buttonText = quota.available() > 0 ? "اسحب" : "اذهب للدرس";
        String tipText = quota.available() > 0 ? "تتم إعادة ضبط محاولات السحب يوميًا عند الفجر" : "يمكن الحصول على محاولات سحب بعد إكمال الدرس/التدريب";
        return new ActivityResponse(pool.getId(), pool.getName(), pool.getId(), pool.getVersion(), QuotaDto.from(quota, quotaService.nextResetAt(quota)), orderRepository.countByUserId(userId), buttonText, tipText);
    }

    @Transactional
    public DrawResponse draw(Long userId, String poolVersion) {
        return draw(userId, poolVersion, null);
    }

    @Transactional
    public DrawResponse draw(Long userId, String poolVersion, String requestId) {
        CardPool pool = drawPool(poolVersion);
        String normalizedRequestId = normalizeRequestId(requestId);
        if (normalizedRequestId != null) {
            Optional<DrawOrder> existing = orderRepository.findByUserIdAndRequestId(userId, normalizedRequestId);
            if (existing.isPresent()) {
                return responseFromExistingOrder(userId, existing.get(), quotaService.currentQuota(userId));
            }
        }

        UserDrawQuota lockedQuota = quotaService.currentQuotaForUpdate(userId);
        if (normalizedRequestId != null) {
            Optional<DrawOrder> existing = orderRepository.findByUserIdAndRequestId(userId, normalizedRequestId);
            if (existing.isPresent()) {
                return responseFromExistingOrder(userId, existing.get(), lockedQuota);
            }
        }
        List<UserCard> beforeUserCards = userCardRepository.findByUserId(userId);
        Set<Long> ownedCardIds = beforeUserCards.stream().map(UserCard::getCardId).collect(Collectors.toSet());
        int collectionRate = collectionRate(userId, pool);
        StaticCardPoolConfig.NewCardProtectionStrategy protectionStrategy = protectionStrategyForPool(pool);
        boolean forceDailyNewCard = needsDailyNewCardProtection(userId, pool.getId(), lockedQuota, collectionRate, protectionStrategy);
        UserDrawQuota quotaAfterConsume = quotaService.consumeLockedQuota(lockedQuota);

        int drawNo = (int) orderRepository.countByUserId(userId) + 1;
        List<NewCardProtectionRule> protectionRules = protectionRuleRepository.findAllByOrderByMinCollectionRateAsc();
        List<DrawnCard> drawnCards = drawFiveCards(userId, pool, ownedCardIds, drawNo, protectionRules);

        boolean forceConsecutiveNewCard = needsConsecutiveNoNewProtection(userId, pool.getId(), collectionRate, protectionStrategy);
        if (drawnCards.stream().noneMatch(DrawnCard::isNewCard)) {
            if (forceDailyNewCard) {
                drawnCards = replaceOneWithNewCard(pool, ownedCardIds, drawnCards, drawNo, true);
            } else if (forceConsecutiveNewCard) {
                drawnCards = replaceOneWithNewCard(pool, ownedCardIds, drawnCards, drawNo, true);
            }
        }

        boolean hasNewCard = drawnCards.stream().anyMatch(DrawnCard::isNewCard);
        DrawOrder order = orderRepository.save(new DrawOrder(userId, pool.getId(), normalizedRequestId, drawNo, CostType.FREE, hasNewCard));

        List<DrawCardResult> resultCards = new ArrayList<>();
        for (DrawnCard drawn : drawnCards) {
            CardSaveResult savedCard = saveUserCard(userId, drawn.card());
            orderCardRepository.save(new DrawOrderCard(order.getId(), userId, drawn.slotNo(), drawn.card().getId(), drawn.card().getRarity(), savedCard.isNew()));
            resultCards.add(toDrawCardResult(drawn.slotNo(), drawn.card(), savedCard.isNew(), savedCard.userCard().getOwnedCount()));
        }

        return new DrawResponse(order.getId(), hasNewCard, resultCards, QuotaDto.from(quotaAfterConsume, quotaService.nextResetAt(quotaAfterConsume)), orderRepository.countByUserId(userId));
    }

    @Transactional(readOnly = true)
    public MyCardsResponse myCards(Long userId, String poolVersion) {
        CardPool pool = drawPool(poolVersion);
        List<Card> allCards = cardRepository.findByPoolVersionAndStatusOrderBySortNoAscIdAsc(pool.getVersion(), CardStatus.ONLINE);
        Map<Long, UserCard> ownedMap = userCardRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserCard::getCardId, Function.identity()));

        List<CardDto> cardDtos = allCards.stream()
                .map(card -> CardDto.from(card, Optional.ofNullable(ownedMap.get(card.getId())).map(UserCard::getOwnedCount).orElse(0), false))
                .toList();

        Map<String, MyCardsResponse.RarityProgress> progress = new LinkedHashMap<>();
        for (Rarity rarity : Rarity.values()) {
            int total = (int) allCards.stream().filter(card -> card.getRarity() == rarity).count();
            int obtained = (int) allCards.stream().filter(card -> card.getRarity() == rarity && ownedMap.containsKey(card.getId())).count();
            progress.put(rarity.name(), new MyCardsResponse.RarityProgress(obtained, total, total == 0 ? 0 : obtained * 100 / total));
        }
        return new MyCardsResponse(userId, ownedMap.size(), allCards.size(), progress, cardDtos);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(Long userId, String poolVersion) {
        CardPool pool = drawPool(poolVersion);
        List<DrawOrder> orders = orderRepository.findTop20ByUserIdAndPoolIdOrderByCreatedAtDesc(userId, pool.getId());
        List<Long> orderIds = orders.stream().map(DrawOrder::getId).toList();
        Map<Long, List<DrawOrderCard>> cardMap = orderCardRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(DrawOrderCard::getOrderId));
        Map<Long, Card> cards = cardRepository.findByIdIn(cardMap.values().stream()
                        .flatMap(Collection::stream)
                        .map(DrawOrderCard::getCardId)
                        .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Card::getId, Function.identity()));

        return orders.stream().map(order -> {
            List<DrawCardResult> results = cardMap.getOrDefault(order.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(DrawOrderCard::getSlotNo))
                    .map(row -> toDrawCardResult(row.getSlotNo(), cards.get(row.getCardId()), row.getIsNew(), null))
                    .toList();
            return new HistoryResponse(order.getId(), order.getDrawNo(), order.getCostType(), order.getHasNewCard(), order.getCreatedAt(), results);
        }).toList();
    }

    private List<DrawnCard> drawFiveCards(Long userId, CardPool pool, Set<Long> ownedCardIds, int drawNo, List<NewCardProtectionRule> protectionRules) {
        List<DrawnCard> drawn = new ArrayList<>();
        for (int slotNo = 1; slotNo <= 5; slotNo++) {
            Rarity rarity = drawRarityForSlot(pool.getId(), drawNo, slotNo);
            Card card = drawCardInRarity(userId, pool.getVersion(), rarity, ownedCardIds, drawn, slotNo, protectionRules);
            drawn.add(new DrawnCard(slotNo, card, !ownedCardIds.contains(card.getId())));
        }
        return drawn;
    }

    private Rarity drawRarityForSlot(Long poolId, int drawNo, int slotNo) {
        Optional<Rarity> fixedRarity = DrawGuaranteePolicy.fixedRarityForSlot(drawNo, slotNo);
        if (fixedRarity.isPresent()) return fixedRarity.get();
        Set<Rarity> allowedRarities = DrawGuaranteePolicy.allowedRaritiesForSlot(drawNo, slotNo);
        if (!allowedRarities.isEmpty()) return drawRarity(poolId, slotNo, allowedRarities);
        return drawRarity(poolId, slotNo);
    }

    private Rarity drawRarity(Long poolId, int slotNo) {
        List<DrawSlotRule> rules = slotRuleRepository.findByPoolIdAndSlotNo(poolId, slotNo);
        if (rules.isEmpty()) {
            throw new BusinessException("لم يتم إعداد احتمال الموضع: slotNo=" + slotNo);
        }
        return WeightedRandomPicker.pick(rules, DrawSlotRule::getProbabilityBasisPoints).getRarity();
    }

    private Rarity drawRarity(Long poolId, int slotNo, Set<Rarity> allowedRarities) {
        List<DrawSlotRule> rules = slotRuleRepository.findByPoolIdAndSlotNo(poolId, slotNo).stream()
                .filter(rule -> allowedRarities.contains(rule.getRarity()))
                .toList();
        if (rules.isEmpty()) {
            throw new BusinessException("لم يتم إعداد احتمال الموضع: slotNo=" + slotNo);
        }
        return WeightedRandomPicker.pick(rules, DrawSlotRule::getProbabilityBasisPoints).getRarity();
    }

    private Card drawCardInRarity(Long userId, String poolVersion, Rarity rarity, Set<Long> ownedCardIds, List<DrawnCard> currentPack, int slotNo, List<NewCardProtectionRule> protectionRules) {
        List<Card> candidates = cardRepository.findByPoolVersionAndRarityAndStatus(poolVersion, rarity, CardStatus.ONLINE);
        if (candidates.isEmpty()) {
            throw new BusinessException("مجموعة هذه الندرة فارغة: " + rarity);
        }
        List<Card> validCandidates = candidates.stream()
                .filter(card -> canAppearInCurrentPack(card, currentPack, slotNo))
                .toList();
        if (validCandidates.isEmpty()) {
            validCandidates = candidates;
        }
        double newCardMultiplier = newCardMultiplier(ownedCardIds, candidates, protectionRules);
        return WeightedRandomPicker.pick(validCandidates, card -> ownedCardIds.contains(card.getId()) ? 1.0 : newCardMultiplier);
    }

    private boolean canAppearInCurrentPack(Card card, List<DrawnCard> currentPack, int slotNo) {
        boolean alreadyInPack = currentPack.stream().anyMatch(drawn -> drawn.card().getId().equals(card.getId()));
        if (slotNo >= 4 && alreadyInPack) {
            return false;
        }
        long sameCardCount = currentPack.stream().filter(drawn -> drawn.card().getId().equals(card.getId())).count();
        if (sameCardCount >= 1) {
            boolean alreadyHasRepeatedGroup = currentPack.stream()
                    .collect(Collectors.groupingBy(drawn -> drawn.card().getId(), Collectors.counting()))
                    .values().stream().anyMatch(count -> count >= 2);
            return !alreadyHasRepeatedGroup;
        }
        return true;
    }

    private double newCardMultiplier(Set<Long> ownedCardIds, List<Card> rarityCardsInCurrentPool, List<NewCardProtectionRule> protectionRules) {
        if (rarityCardsInCurrentPool.isEmpty()) {
            return 1.0;
        }
        long obtained = rarityCardsInCurrentPool.stream()
                .filter(card -> ownedCardIds.contains(card.getId()))
                .count();
        int rate = (int) (obtained * 100 / rarityCardsInCurrentPool.size());
        return protectionRules.stream()
                .filter(rule -> rate >= rule.getMinCollectionRate() && rate <= rule.getMaxCollectionRate())
                .findFirst()
                .map(NewCardProtectionRule::getNewCardWeightMultiplier)
                .orElse(1.0);
    }

    private int collectionRate(Long userId, CardPool pool) {
        long total = cardRepository.countByPoolVersionAndStatus(pool.getVersion(), CardStatus.ONLINE);
        if (total == 0) {
            return 100;
        }
        long obtained = userCardRepository.countOwnedOnlineCardsByUserIdAndPoolVersion(userId, pool.getVersion(), CardStatus.ONLINE.name());
        return (int) (obtained * 100 / total);
    }

    private boolean needsConsecutiveNoNewProtection(Long userId, Long poolId, int collectionRate, StaticCardPoolConfig.NewCardProtectionStrategy protectionStrategy) {
        if (!isCollectionRateInBand(collectionRate, protectionStrategy.consecutiveNoNewProtection())) {
            return false;
        }
        List<DrawOrder> latest = orderRepository.findTop5ByUserIdAndPoolIdOrderByCreatedAtDesc(userId, poolId);
        return latest.size() >= 2 && latest.stream().limit(2).noneMatch(DrawOrder::getHasNewCard);
    }

    private boolean needsDailyNewCardProtection(Long userId, Long poolId, UserDrawQuota quotaBeforeConsume, int collectionRate, StaticCardPoolConfig.NewCardProtectionStrategy protectionStrategy) {
        if (!isCollectionRateInBand(collectionRate, protectionStrategy.dailyNewCardProtection())) {
            return false;
        }
        if (quotaBeforeConsume.getUsedToday() == null || quotaBeforeConsume.getUsedToday() != 4) {
            return false;
        }
        List<DrawOrder> todayOrders = dailyOrdersForProtection(userId, poolId, quotaBeforeConsume.getQuotaDate());
        return todayOrders.size() >= 4 && todayOrders.stream().limit(4).filter(DrawOrder::getHasNewCard).count() < 2;
    }

    private boolean isCollectionRateInBand(int collectionRate, StaticCardPoolConfig.CollectionRateBand band) {
        return collectionRate >= band.minCollectionRate() && collectionRate <= band.maxCollectionRate();
    }

    private StaticCardPoolConfig.NewCardProtectionStrategy protectionStrategyForPool(CardPool pool) {
        return cardPoolConfigService.findByVersion(pool.getVersion())
                .map(StaticCardPoolConfig::newCardProtectionStrategy)
                .orElseGet(() -> {
                    log.warn("Static card pool config missing for version {}, falling back to default new-card protection strategy", pool.getVersion());
                    return NewCardProtectionPolicy.defaultStrategy();
                });
    }

    private List<DrawOrder> dailyOrdersForProtection(Long userId, Long poolId, LocalDate quotaDate) {
        if (quotaDate.isAfter(clock.businessDate())) {
            return orderRepository.findTop5ByUserIdAndPoolIdOrderByCreatedAtDesc(userId, poolId);
        }
        return orderRepository.findByUserIdAndPoolIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                userId,
                poolId,
                clock.businessDayStart(quotaDate),
                clock.businessDayEnd(quotaDate)
        );
    }

    private List<DrawnCard> replaceOneWithNewCard(CardPool pool, Set<Long> ownedCardIds, List<DrawnCard> drawnCards, int drawNo, boolean allowAnyRarityFallback) {
        List<Card> onlineCardsInPool = cardRepository.findByPoolVersionAndStatusOrderBySortNoAscIdAsc(pool.getVersion(), CardStatus.ONLINE);
        Map<Rarity, List<Card>> onlineCardsByRarity = onlineCardsInPool.stream()
                .collect(Collectors.groupingBy(Card::getRarity, () -> new EnumMap<>(Rarity.class), Collectors.toList()));
        int replacementSlots = allowAnyRarityFallback ? drawnCards.size() : Math.min(4, drawnCards.size());
        for (int i = 0; i < replacementSlots; i++) {
            DrawnCard origin = drawnCards.get(i);
            List<Card> newCandidates = onlineCardsByRarity.getOrDefault(origin.card().getRarity(), List.of()).stream()
                    .filter(card -> !ownedCardIds.contains(card.getId()))
                    .filter(card -> canAppearInCurrentPack(card, drawnCards.stream().filter(d -> d != origin).toList(), origin.slotNo()))
                    .toList();
            if (!newCandidates.isEmpty()) {
                List<DrawnCard> replaced = new ArrayList<>(drawnCards);
                Card newCard = WeightedRandomPicker.pick(newCandidates, card -> 1.0);
                replaced.set(i, new DrawnCard(origin.slotNo(), newCard, true));
                return replaced;
            }
        }
        if (!allowAnyRarityFallback) {
            return drawnCards;
        }
        for (int i = 0; i < drawnCards.size(); i++) {
            DrawnCard origin = drawnCards.get(i);
            Set<Rarity> allowedRarities = allowedReplacementRarities(pool.getId(), drawNo, origin.slotNo());
            List<Card> newCandidates = onlineCardsInPool.stream()
                    .filter(card -> !ownedCardIds.contains(card.getId()))
                    .filter(card -> allowedRarities.contains(card.getRarity()))
                    .filter(card -> canAppearInCurrentPack(card, drawnCards.stream().filter(d -> d != origin).toList(), origin.slotNo()))
                    .toList();
            if (!newCandidates.isEmpty()) {
                List<DrawnCard> replaced = new ArrayList<>(drawnCards);
                Card newCard = WeightedRandomPicker.pick(newCandidates, card -> 1.0);
                replaced.set(i, new DrawnCard(origin.slotNo(), newCard, true));
                return replaced;
            }
        }
        return drawnCards;
    }

    private Set<Rarity> allowedReplacementRarities(Long poolId, int drawNo, int slotNo) {
        Optional<Rarity> fixedRarity = DrawGuaranteePolicy.fixedRarityForSlot(drawNo, slotNo);
        if (fixedRarity.isPresent()) {
            return EnumSet.of(fixedRarity.get());
        }
        Set<Rarity> guaranteedRarities = DrawGuaranteePolicy.allowedRaritiesForSlot(drawNo, slotNo);
        List<DrawSlotRule> slotRules = slotRuleRepository.findByPoolIdAndSlotNo(poolId, slotNo).stream()
                .filter(rule -> rule.getProbabilityBasisPoints() != null && rule.getProbabilityBasisPoints() > 0)
                .toList();
        if (!guaranteedRarities.isEmpty()) {
            return slotRules.stream()
                    .map(DrawSlotRule::getRarity)
                    .filter(guaranteedRarities::contains)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Rarity.class)));
        }
        return slotRules.stream()
                .map(DrawSlotRule::getRarity)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Rarity.class)));
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

    private DrawResponse responseFromExistingOrder(Long userId, DrawOrder order, UserDrawQuota quota) {
        List<DrawOrderCard> orderCards = orderCardRepository.findByOrderId(order.getId()).stream()
                .sorted(Comparator.comparing(DrawOrderCard::getSlotNo))
                .toList();
        Map<Long, Card> cards = cardRepository.findByIdIn(orderCards.stream()
                        .map(DrawOrderCard::getCardId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));
        Map<Long, Integer> ownedCountByCardId = userCardRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserCard::getCardId, UserCard::getOwnedCount));
        List<DrawCardResult> resultCards = orderCards.stream()
                .map(row -> toDrawCardResult(row.getSlotNo(), cards.get(row.getCardId()), row.getIsNew(), ownedCountByCardId.get(row.getCardId())))
                .toList();
        return new DrawResponse(order.getId(), order.getHasNewCard(), resultCards, QuotaDto.from(quota, quotaService.nextResetAt(quota)), orderRepository.countByUserId(userId));
    }

    private CardSaveResult saveUserCard(Long userId, Card card) {
        int inserted = userCardRepository.insertIfAbsent(userId, card.getId(), card.getRarity().name());
        if (inserted == 0) {
            int updated = userCardRepository.increaseOwnedCount(userId, card.getId());
            if (updated == 0) {
                throw new BusinessException("فشل تحديث الألبوم");
            }
        }
        UserCard userCard = userCardRepository.findByUserIdAndCardId(userId, card.getId())
                .orElseThrow(() -> new BusinessException("فشل تحديث الألبوم"));
        return new CardSaveResult(userCard, inserted > 0);
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        String normalized = requestId.trim();
        if (normalized.length() > 64) {
            throw new BusinessException("لا يمكن أن يتجاوز طول requestId 64");
        }
        return normalized;
    }

    private DrawCardResult toDrawCardResult(Integer slotNo, Card card, Boolean isNew, Integer ownedCount) {
        if (card == null) {
            return null;
        }
        return new DrawCardResult(slotNo, card.getId(), card.getWord(), card.getMeaning(), card.getArabicMeaning(), card.getAudioUrl(), card.getImageUrl(), card.getRarity(), isNew, ownedCount);
    }

    private record DrawnCard(Integer slotNo, Card card, boolean isNewCard) {
    }

    private record CardSaveResult(UserCard userCard, boolean isNew) {
    }
}
