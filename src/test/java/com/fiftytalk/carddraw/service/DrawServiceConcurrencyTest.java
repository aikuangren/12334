package com.fiftytalk.carddraw.service;

import com.fiftytalk.carddraw.common.BusinessException;
import com.fiftytalk.carddraw.config.StaticCardPoolConfig;
import com.fiftytalk.carddraw.domain.CardStatus;
import com.fiftytalk.carddraw.domain.CostType;
import com.fiftytalk.carddraw.domain.PoolStatus;
import com.fiftytalk.carddraw.domain.Rarity;
import com.fiftytalk.carddraw.dto.DrawCardResult;
import com.fiftytalk.carddraw.dto.DrawResponse;
import com.fiftytalk.carddraw.entity.Card;
import com.fiftytalk.carddraw.entity.CardPool;
import com.fiftytalk.carddraw.entity.DrawOrder;
import com.fiftytalk.carddraw.entity.DrawSlotRule;
import com.fiftytalk.carddraw.entity.UserDrawQuota;
import com.fiftytalk.carddraw.repository.CardPoolRepository;
import com.fiftytalk.carddraw.repository.CardRepository;
import com.fiftytalk.carddraw.repository.DrawOrderRepository;
import com.fiftytalk.carddraw.repository.DrawSlotRuleRepository;
import com.fiftytalk.carddraw.repository.UserDrawQuotaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.AopTestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DrawServiceConcurrencyTest {
    @Autowired
    private DrawService drawService;

    @Autowired
    private DrawOrderRepository orderRepository;

    @Autowired
    private UserDrawQuotaRepository quotaRepository;

    @Autowired
    private CardPoolRepository poolRepository;

    @Autowired
    private DrawSlotRuleRepository slotRuleRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private BusinessClock clock;

    @Test
    void duplicateRequestIdReturnsSameOrderAndConsumesOnce() {
        long userId = 91001L;

        DrawResponse first = drawService.draw(userId, "v1", "same-request");
        DrawResponse second = drawService.draw(userId, "v1", "same-request");

        assertThat(second.orderId()).isEqualTo(first.orderId());
        assertThat(orderRepository.countByUserId(userId)).isEqualTo(1);
        assertThat(first.quota().availableQuota()).isEqualTo(4);
        assertThat(second.quota().availableQuota()).isEqualTo(4);
    }

    @Test
    void concurrentDrawsDoNotExceedDailyQuota() throws Exception {
        long userId = 91002L;
        int attempts = 10;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                int index = i;
                tasks.add(() -> {
                    try {
                        drawService.draw(userId, "v1", "concurrent-" + index);
                        return true;
                    } catch (BusinessException exception) {
                        if ("لا توجد محاولات سحب كافية".equals(exception.getMessage())) {
                            return false;
                        }
                        throw exception;
                    }
                });
            }

            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            int successCount = 0;
            for (Future<Boolean> future : futures) {
                try {
                    if (future.get()) {
                        successCount++;
                    }
                } catch (ExecutionException exception) {
                    throw new AssertionError(exception.getCause());
                }
            }

            UserDrawQuota quota = quotaRepository.findByUserIdAndQuotaDate(userId, drawService.activity(userId, "v1").quota().quotaDate())
                    .orElseThrow();
            assertThat(successCount).isEqualTo(5);
            assertThat(orderRepository.countByUserId(userId)).isEqualTo(5);
            assertThat(quota.available()).isZero();
            assertThat(quota.getUsedToday()).isEqualTo(5);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void dailyQuotaIsSharedAcrossPools() {
        long userId = 91005L;

        UserDrawQuota before = quotaService.currentQuota(userId);
        quotaService.consumeOne(userId);
        UserDrawQuota afterSecondConsume = quotaService.consumeOne(userId);

        assertThat(before.available()).isEqualTo(5);
        assertThat(afterSecondConsume.available()).isEqualTo(3);
        assertThat(afterSecondConsume.getUsedToday()).isEqualTo(2);
    }

    @Test
    void secondCumulativeDrawGuaranteesSsr() {
        long userId = 91004L;

        DrawResponse first = drawService.draw(userId, "v1", "second-guarantee-1");
        DrawResponse second = drawService.draw(userId, "v1", "second-guarantee-2");

        assertThat(first.totalDrawCount()).isEqualTo(1);
        assertThat(second.totalDrawCount()).isEqualTo(2);
        assertThat(slotCard(second, 5).rarity()).isEqualTo(Rarity.SSR);
    }

    @Test
    void everySixthDrawGuaranteesUrPlusInFifthSlot() {
        long userId = 91003L;
        List<DrawResponse> responses = new ArrayList<>();
        for (int index = 1; index <= 18; index++) {
            if (index > 1 && (index - 1) % 5 == 0) {
                quotaService.simulateNextDay(userId);
            }
            responses.add(drawService.draw(userId, "v1", "guarantee-" + index));
        }

        assertThat(slotCard(responses.get(5), 5).rarity().ordinal()).isGreaterThanOrEqualTo(Rarity.UR.ordinal());
        assertThat(slotCard(responses.get(11), 5).rarity().ordinal()).isGreaterThanOrEqualTo(Rarity.UR.ordinal());
        assertThat(slotCard(responses.get(17), 5).rarity().ordinal()).isGreaterThanOrEqualTo(Rarity.UR.ordinal());
        assertThat(responses.get(5).totalDrawCount()).isEqualTo(6);
        assertThat(responses.get(17).totalDrawCount()).isEqualTo(18);
    }

    @Test
    void dailyNewFallbackKeepsReplacementWithinSlotRarityRules() throws Exception {
        String poolVersion = "verify-slot-rarity-rules";
        CardPool pool = poolRepository.save(new CardPool(poolVersion, "Verify slot rarity rules", PoolStatus.ONLINE));
        for (int slotNo = 1; slotNo <= 5; slotNo++) {
            slotRuleRepository.save(new DrawSlotRule(pool.getId(), slotNo, Rarity.N, 10000));
        }
        List<Card> ownedCards = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            ownedCards.add(cardRepository.save(new Card("owned-n-" + index, "owned", null, null, Rarity.N, poolVersion, index)));
        }
        Card unavailableForSlots = cardRepository.save(new Card("unowned-ur", "new", null, null, Rarity.UR, poolVersion, 4));
        assertThat(unavailableForSlots.getStatus()).isEqualTo(CardStatus.ONLINE);

        Class<?> drawnCardClass = Class.forName("com.fiftytalk.carddraw.service.DrawService$DrawnCard");
        Constructor<?> constructor = drawnCardClass.getDeclaredConstructor(Integer.class, Card.class, boolean.class);
        constructor.setAccessible(true);
        List<Object> drawnCards = new ArrayList<>();
        for (int index = 0; index < ownedCards.size(); index++) {
            drawnCards.add(constructor.newInstance(index + 1, ownedCards.get(index), false));
        }
        Method replaceOneWithNewCard = DrawService.class.getDeclaredMethod(
                "replaceOneWithNewCard",
                CardPool.class,
                Set.class,
                List.class,
                int.class,
                boolean.class
        );
        replaceOneWithNewCard.setAccessible(true);

        DrawService targetDrawService = AopTestUtils.getTargetObject(drawService);
        Set<Long> ownedCardIds = ownedCards.stream().map(Card::getId).collect(java.util.stream.Collectors.toSet());
        List<?> replaced = (List<?>) replaceOneWithNewCard.invoke(targetDrawService, pool, ownedCardIds, drawnCards, 1, true);
        Method cardAccessor = drawnCardClass.getDeclaredMethod("card");
        Method isNewCardAccessor = drawnCardClass.getDeclaredMethod("isNewCard");

        assertThat(replaced).hasSize(5);
        for (Object item : replaced) {
            Card card = (Card) cardAccessor.invoke(item);
            assertThat(card.getRarity()).isEqualTo(Rarity.N);
            assertThat((Boolean) isNewCardAccessor.invoke(item)).isFalse();
        }
    }

    @Test
    void consecutiveNewFallbackCanReplaceAcrossRarityWithinSlotRules() throws Exception {
        String poolVersion = "verify-consecutive-cross-rarity";
        CardPool pool = poolRepository.save(new CardPool(poolVersion, "Verify consecutive cross rarity", PoolStatus.ONLINE));
        for (int slotNo = 1; slotNo <= 5; slotNo++) {
            slotRuleRepository.save(new DrawSlotRule(pool.getId(), slotNo, Rarity.N, 5000));
            slotRuleRepository.save(new DrawSlotRule(pool.getId(), slotNo, Rarity.R, 5000));
        }
        List<Card> ownedCards = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            ownedCards.add(cardRepository.save(new Card("owned-n-" + index, "owned", null, null, Rarity.N, poolVersion, index)));
        }
        cardRepository.save(new Card("new-r", "new", null, null, Rarity.R, poolVersion, 6));

        Class<?> drawnCardClass = Class.forName("com.fiftytalk.carddraw.service.DrawService$DrawnCard");
        Constructor<?> constructor = drawnCardClass.getDeclaredConstructor(Integer.class, Card.class, boolean.class);
        constructor.setAccessible(true);
        List<Object> drawnCards = new ArrayList<>();
        for (int index = 0; index < ownedCards.size(); index++) {
            drawnCards.add(constructor.newInstance(index + 1, ownedCards.get(index), false));
        }
        Method replaceOneWithNewCard = DrawService.class.getDeclaredMethod(
                "replaceOneWithNewCard",
                CardPool.class,
                Set.class,
                List.class,
                int.class,
                boolean.class
        );
        replaceOneWithNewCard.setAccessible(true);

        DrawService targetDrawService = AopTestUtils.getTargetObject(drawService);
        Set<Long> ownedCardIds = ownedCards.stream().map(Card::getId).collect(java.util.stream.Collectors.toSet());
        List<?> replaced = (List<?>) replaceOneWithNewCard.invoke(targetDrawService, pool, ownedCardIds, drawnCards, 1, true);
        Method isNewCardAccessor = drawnCardClass.getDeclaredMethod("isNewCard");

        assertThat(replaced).hasSize(5);
        assertThat(replaced).anyMatch(item -> {
            try {
                return (Boolean) isNewCardAccessor.invoke(item);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
    }

    @Test
    void drawFallsBackToDefaultProtectionStrategyWhenStaticPoolConfigIsMissing() {
        long userId = 91010L;
        String poolVersion = "verify-missing-static-config";
        CardPool pool = poolRepository.save(new CardPool(poolVersion, "Verify missing static config", PoolStatus.ONLINE));
        for (int slotNo = 1; slotNo <= 5; slotNo++) {
            slotRuleRepository.save(new DrawSlotRule(pool.getId(), slotNo, Rarity.N, 10000));
        }
        for (int index = 1; index <= 5; index++) {
            cardRepository.save(new Card("fallback-n-" + index, "owned", null, null, Rarity.N, poolVersion, index));
        }

        DrawResponse response = drawService.draw(userId, poolVersion, "missing-static-config-draw");

        assertThat(response.cards()).hasSize(5);
        assertThat(response.cards()).allMatch(card -> card.rarity() == Rarity.N);
    }

    @Test
    void dailyNewProtectionCountsOrdersWithinCurrentPoolOnly() throws Exception {
        long userId = 91007L;
        CardPool firstPool = poolRepository.save(new CardPool("verify-pool-scope-a", "Verify pool scope A", PoolStatus.ONLINE));
        CardPool secondPool = poolRepository.save(new CardPool("verify-pool-scope-b", "Verify pool scope B", PoolStatus.ONLINE));
        UserDrawQuota quota = dailyQuotaAfterFourDraws(userId);
        for (int drawNo = 1; drawNo <= 4; drawNo++) {
            orderRepository.save(new DrawOrder(userId, firstPool.getId(), drawNo, CostType.FREE, false));
        }

        Method needsDailyNewCardProtection = DrawService.class.getDeclaredMethod(
                "needsDailyNewCardProtection",
                Long.class,
                Long.class,
                UserDrawQuota.class,
                int.class,
                StaticCardPoolConfig.NewCardProtectionStrategy.class
        );
        needsDailyNewCardProtection.setAccessible(true);

        DrawService targetDrawService = AopTestUtils.getTargetObject(drawService);
        StaticCardPoolConfig.NewCardProtectionStrategy strategy = testProtectionStrategy();
        assertThat((Boolean) needsDailyNewCardProtection.invoke(targetDrawService, userId, firstPool.getId(), quota, 98, strategy)).isTrue();
        assertThat((Boolean) needsDailyNewCardProtection.invoke(targetDrawService, userId, firstPool.getId(), quota, 51, strategy)).isTrue();
        assertThat((Boolean) needsDailyNewCardProtection.invoke(targetDrawService, userId, firstPool.getId(), quota, 50, strategy)).isFalse();
        assertThat((Boolean) needsDailyNewCardProtection.invoke(targetDrawService, userId, secondPool.getId(), quota, 98, strategy)).isFalse();
    }

    @Test
    void dailyNewProtectionDoesNotTriggerWhenFirstFourDrawsAlreadyHaveTwoNewCardRounds() throws Exception {
        long userId = 91010L;
        CardPool pool = poolRepository.save(new CardPool("verify-daily-two-new-target", "Verify daily two new target", PoolStatus.ONLINE));
        UserDrawQuota quota = dailyQuotaAfterFourDraws(userId);
        orderRepository.save(new DrawOrder(userId, pool.getId(), 1, CostType.FREE, true));
        orderRepository.save(new DrawOrder(userId, pool.getId(), 2, CostType.FREE, false));
        orderRepository.save(new DrawOrder(userId, pool.getId(), 3, CostType.FREE, true));
        orderRepository.save(new DrawOrder(userId, pool.getId(), 4, CostType.FREE, false));

        Method needsDailyNewCardProtection = DrawService.class.getDeclaredMethod(
                "needsDailyNewCardProtection",
                Long.class,
                Long.class,
                UserDrawQuota.class,
                int.class,
                StaticCardPoolConfig.NewCardProtectionStrategy.class
        );
        needsDailyNewCardProtection.setAccessible(true);

        DrawService targetDrawService = AopTestUtils.getTargetObject(drawService);
        assertThat((Boolean) needsDailyNewCardProtection.invoke(targetDrawService, userId, pool.getId(), quota, 98, testProtectionStrategy())).isFalse();
    }

    @Test
    void dailyNewProtectionWorksForSimulatedFutureBusinessDate() throws Exception {
        long userId = 91008L;
        CardPool pool = poolRepository.save(new CardPool("verify-future-business-date", "Verify future business date", PoolStatus.ONLINE));
        LocalDate futureQuotaDate = clock.businessDate().plusDays(1);
        UserDrawQuota quota = new UserDrawQuota(userId, 5, futureQuotaDate);
        for (int index = 0; index < 4; index++) {
            quota.consumeOne();
        }
        for (int drawNo = 1; drawNo <= 4; drawNo++) {
            orderRepository.save(new DrawOrder(userId, pool.getId(), drawNo, CostType.FREE, false));
        }

        Method needsDailyNewCardProtection = DrawService.class.getDeclaredMethod(
                "needsDailyNewCardProtection",
                Long.class,
                Long.class,
                UserDrawQuota.class,
                int.class,
                StaticCardPoolConfig.NewCardProtectionStrategy.class
        );
        needsDailyNewCardProtection.setAccessible(true);
        DrawService targetDrawService = AopTestUtils.getTargetObject(drawService);

        assertThat((Boolean) needsDailyNewCardProtection.invoke(targetDrawService, userId, pool.getId(), quota, 98, testProtectionStrategy())).isTrue();
    }

    @Test
    void consecutiveNoNewProtectionOnlyWorksInMiddleLateCollectionRateBand() throws Exception {
        long userId = 91009L;
        CardPool pool = poolRepository.save(new CardPool("verify-consecutive-rate-band", "Verify consecutive rate band", PoolStatus.ONLINE));
        orderRepository.save(new DrawOrder(userId, pool.getId(), 1, CostType.FREE, false));
        orderRepository.save(new DrawOrder(userId, pool.getId(), 2, CostType.FREE, false));

        Method needsConsecutiveNoNewProtection = DrawService.class.getDeclaredMethod(
                "needsConsecutiveNoNewProtection",
                Long.class,
                Long.class,
                int.class,
                StaticCardPoolConfig.NewCardProtectionStrategy.class
        );
        needsConsecutiveNoNewProtection.setAccessible(true);
        DrawService targetDrawService = AopTestUtils.getTargetObject(drawService);
        StaticCardPoolConfig.NewCardProtectionStrategy strategy = testProtectionStrategy();

        assertThat((Boolean) needsConsecutiveNoNewProtection.invoke(targetDrawService, userId, pool.getId(), 50, strategy)).isFalse();
        assertThat((Boolean) needsConsecutiveNoNewProtection.invoke(targetDrawService, userId, pool.getId(), 75, strategy)).isTrue();
        assertThat((Boolean) needsConsecutiveNoNewProtection.invoke(targetDrawService, userId, pool.getId(), 94, strategy)).isTrue();
        assertThat((Boolean) needsConsecutiveNoNewProtection.invoke(targetDrawService, userId, pool.getId(), 95, strategy)).isTrue();
        assertThat((Boolean) needsConsecutiveNoNewProtection.invoke(targetDrawService, userId, pool.getId(), 99, strategy)).isTrue();
        assertThat((Boolean) needsConsecutiveNoNewProtection.invoke(targetDrawService, userId, pool.getId(), 100, strategy)).isFalse();
    }

    private DrawCardResult slotCard(DrawResponse response, int slotNo) {
        return response.cards().stream()
                .filter(card -> card.slotNo() == slotNo)
                .findFirst()
                .orElseThrow();
    }

    private void seedDailyNoNewState(long userId, CardPool pool) {
        UserDrawQuota quota = dailyQuotaAfterFourDraws(userId);
        quotaRepository.save(quota);
        for (int drawNo = 1; drawNo <= 4; drawNo++) {
            orderRepository.save(new DrawOrder(userId, pool.getId(), drawNo, CostType.FREE, false));
        }
    }

    private UserDrawQuota dailyQuotaAfterFourDraws(long userId) {
        UserDrawQuota quota = new UserDrawQuota(userId, 5, clock.businessDate());
        for (int index = 0; index < 4; index++) {
            quota.consumeOne();
        }
        return quota;
    }

    private StaticCardPoolConfig.NewCardProtectionStrategy testProtectionStrategy() {
        return new StaticCardPoolConfig.NewCardProtectionStrategy(
                new StaticCardPoolConfig.CollectionRateBand(51, 99),
                new StaticCardPoolConfig.CollectionRateBand(51, 99)
        );
    }
}
