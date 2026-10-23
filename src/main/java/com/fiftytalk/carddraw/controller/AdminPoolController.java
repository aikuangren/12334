package com.fiftytalk.carddraw.controller;

import com.fiftytalk.carddraw.common.ApiResponse;
import com.fiftytalk.carddraw.common.BusinessException;
import com.fiftytalk.carddraw.domain.ActivityStatus;
import com.fiftytalk.carddraw.domain.PoolStatus;
import com.fiftytalk.carddraw.config.DefaultDrawSlotRules;
import com.fiftytalk.carddraw.dto.AdminPoolRequest;
import com.fiftytalk.carddraw.dto.AdminPoolSummary;
import com.fiftytalk.carddraw.dto.AdminSlotRuleUpdateRequest;
import com.fiftytalk.carddraw.entity.CardPool;
import com.fiftytalk.carddraw.entity.DrawActivity;
import com.fiftytalk.carddraw.entity.DrawSlotRule;
import com.fiftytalk.carddraw.entity.NewCardProtectionRule;
import com.fiftytalk.carddraw.repository.CardRepository;
import com.fiftytalk.carddraw.repository.CardPoolRepository;
import com.fiftytalk.carddraw.repository.DrawActivityRepository;
import com.fiftytalk.carddraw.repository.DrawSlotRuleRepository;
import com.fiftytalk.carddraw.repository.NewCardProtectionRuleRepository;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/card-draw")
public class AdminPoolController {
    private final CardPoolRepository poolRepository;
    private final CardRepository cardRepository;
    private final DrawActivityRepository activityRepository;
    private final DrawSlotRuleRepository slotRuleRepository;
    private final NewCardProtectionRuleRepository protectionRuleRepository;

    public AdminPoolController(
            CardPoolRepository poolRepository,
            CardRepository cardRepository,
            DrawActivityRepository activityRepository,
            DrawSlotRuleRepository slotRuleRepository,
            NewCardProtectionRuleRepository protectionRuleRepository
    ) {
        this.poolRepository = poolRepository;
        this.cardRepository = cardRepository;
        this.activityRepository = activityRepository;
        this.slotRuleRepository = slotRuleRepository;
        this.protectionRuleRepository = protectionRuleRepository;
    }

    @GetMapping("/pools")
    public ApiResponse<List<AdminPoolSummary>> pools() {
        List<AdminPoolSummary> pools = poolRepository.findAll().stream()
                .sorted(Comparator.comparing(CardPool::getDisplaySortNo).thenComparing(CardPool::getId))
                .map(pool -> AdminPoolSummary.from(
                        pool,
                        cardRepository.countByPoolVersion(pool.getVersion()),
                        false
                ))
                .toList();
        return ApiResponse.ok(pools);
    }

    @PostMapping("/pools")
    @Transactional
    public ApiResponse<AdminPoolSummary> createPool(@Valid @RequestBody AdminPoolRequest request) {
        String version = normalizeVersion(request.version());
        poolRepository.findByVersion(version).ifPresent(pool -> {
            throw new BusinessException("إصدار مجموعة البطاقات موجود مسبقًا: " + version);
        });
        PoolStatus status = request.status() == null ? PoolStatus.DRAFT : request.status();
        CardPool pool = poolRepository.save(new CardPool(
                version,
                request.name().trim(),
                normalizeText(request.packImageUrl()),
                normalizeSortNo(request.displaySortNo()),
                status
        ));
        copySlotRules(pool.getId(), request.copyRulesFromPoolId());
        return ApiResponse.ok(AdminPoolSummary.from(pool, 0, false));
    }

    @PutMapping("/pools/{id}")
    @Transactional
    public ApiResponse<AdminPoolSummary> updatePool(@PathVariable Long id, @Valid @RequestBody AdminPoolRequest request) {
        CardPool pool = poolRepository.findById(id).orElseThrow(() -> new BusinessException("مجموعة البطاقات غير موجودة"));
        String version = normalizeVersion(request.version());
        poolRepository.findByVersion(version)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("إصدار مجموعة البطاقات موجود مسبقًا: " + version);
                });
        pool.setVersion(version);
        pool.setName(request.name().trim());
        pool.setPackImageUrl(normalizeText(request.packImageUrl()));
        pool.setDisplaySortNo(normalizeSortNo(request.displaySortNo()));
        pool.setStatus(request.status() == null ? PoolStatus.DRAFT : request.status());
        CardPool saved = poolRepository.save(pool);
        return ApiResponse.ok(AdminPoolSummary.from(saved, cardRepository.countByPoolVersion(saved.getVersion()), false));
    }

    @GetMapping("/activities")
    public ApiResponse<List<DrawActivity>> activities() {
        return ApiResponse.ok(activityRepository.findAll());
    }

    @PostMapping("/activities")
    public ApiResponse<DrawActivity> saveActivity(@RequestBody DrawActivity activity) {
        return ApiResponse.ok(activityRepository.save(activity));
    }

    @GetMapping("/slot-rules")
    public ApiResponse<List<DrawSlotRule>> slotRules(@RequestParam Long poolId) {
        return ApiResponse.ok(slotRuleRepository.findByPoolIdOrderBySlotNoAscIdAsc(poolId));
    }

    @GetMapping("/slot-rules/defaults")
    public ApiResponse<List<DrawSlotRule>> defaultSlotRules() {
        return ApiResponse.ok(defaultSlotRules(0L));
    }

    @PutMapping("/slot-rules")
    @Transactional
    public ApiResponse<List<DrawSlotRule>> updateSlotRules(
            @RequestParam Long poolId,
            @Valid @RequestBody AdminSlotRuleUpdateRequest request
    ) {
        poolRepository.findById(poolId).orElseThrow(() -> new BusinessException("مجموعة البطاقات غير موجودة"));
        List<DrawSlotRule> newRules = request.rules().stream()
                .map(item -> new DrawSlotRule(poolId, item.slotNo(), item.rarity(), toBasisPoints(item)))
                .filter(rule -> rule.getProbabilityBasisPoints() > 0)
                .toList();
        validateSlotRules(newRules);
        slotRuleRepository.deleteAll(slotRuleRepository.findByPoolIdOrderBySlotNoAscIdAsc(poolId));
        slotRuleRepository.saveAll(newRules);
        return ApiResponse.ok(slotRuleRepository.findByPoolIdOrderBySlotNoAscIdAsc(poolId));
    }

    @GetMapping("/new-card-protection-rules")
    public ApiResponse<List<NewCardProtectionRule>> protectionRules() {
        return ApiResponse.ok(protectionRuleRepository.findAllByOrderByMinCollectionRateAsc());
    }

    private String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new BusinessException("لا يمكن ترك إصدار مجموعة البطاقات فارغًا");
        }
        return version.trim();
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int normalizeSortNo(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private void copySlotRules(Long targetPoolId, Long sourcePoolId) {
        List<DrawSlotRule> sourceRules = sourcePoolId == null
                ? defaultSlotRules(targetPoolId)
                : slotRuleRepository.findByPoolIdOrderBySlotNoAscIdAsc(sourcePoolId).stream()
                .map(rule -> new DrawSlotRule(targetPoolId, rule.getSlotNo(), rule.getRarity(), rule.getProbabilityBasisPoints()))
                .toList();
        if (sourceRules.isEmpty()) {
            sourceRules = defaultSlotRules(targetPoolId);
        }
        slotRuleRepository.saveAll(sourceRules);
    }

    private List<DrawSlotRule> defaultSlotRules(Long poolId) {
        return DefaultDrawSlotRules.forPool(poolId);
    }

    private int toBasisPoints(AdminSlotRuleUpdateRequest.Item item) {
        if (item.probabilityBasisPoints() != null) {
            return item.probabilityBasisPoints();
        }
        if (item.probabilityPercent() == null) {
            throw new BusinessException("لا يمكن ترك الاحتمال فارغًا");
        }
        return (int) Math.round(item.probabilityPercent() * 100);
    }

    private void validateSlotRules(List<DrawSlotRule> rules) {
        for (DrawSlotRule rule : rules) {
            if (rule.getSlotNo() == null || rule.getSlotNo() < 1 || rule.getSlotNo() > 5) {
                throw new BusinessException("يجب أن يكون موضع البطاقة من 1 إلى 5");
            }
            if (rule.getProbabilityBasisPoints() < 0) {
                throw new BusinessException("لا يمكن أن يكون الاحتمال أقل من 0");
            }
        }
        Map<Integer, Integer> slotTotals = rules.stream().collect(Collectors.groupingBy(
                DrawSlotRule::getSlotNo,
                Collectors.summingInt(DrawSlotRule::getProbabilityBasisPoints)
        ));
        for (int slot = 1; slot <= 5; slot++) {
            int total = slotTotals.getOrDefault(slot, 0);
            if (total != 10000) {
                throw new BusinessException("رقم " + slot + " يجب أن يكون إجمالي احتمال الموضع 100%، الحالي " + (total / 100.0) + "%");
            }
        }
    }
}
