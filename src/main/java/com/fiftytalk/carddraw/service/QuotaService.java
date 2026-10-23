package com.fiftytalk.carddraw.service;

import com.fiftytalk.carddraw.common.BusinessException;
import com.fiftytalk.carddraw.config.CardDrawProperties;
import com.fiftytalk.carddraw.entity.UserDrawQuota;
import com.fiftytalk.carddraw.repository.UserDrawQuotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;

@Service
public class QuotaService {
    private final CardDrawProperties properties;
    private final BusinessClock clock;
    private final UserDrawQuotaRepository quotaRepository;

    public QuotaService(CardDrawProperties properties, BusinessClock clock, UserDrawQuotaRepository quotaRepository) {
        this.properties = properties;
        this.clock = clock;
        this.quotaRepository = quotaRepository;
    }

    @Transactional
    public UserDrawQuota currentQuota(Long userId) {
        LocalDate quotaDate = currentTestingDate(userId);
        ensureQuotaExists(userId, quotaDate);
        return quotaRepository.findByUserIdAndQuotaDate(userId, quotaDate)
                .orElseThrow(() -> new BusinessException("فشل تهيئة محاولات السحب"));
    }

    @Transactional
    public UserDrawQuota currentQuotaForUpdate(Long userId) {
        LocalDate quotaDate = currentTestingDate(userId);
        ensureQuotaExists(userId, quotaDate);
        return quotaRepository.findByUserIdAndQuotaDateForUpdate(userId, quotaDate)
                .orElseThrow(() -> new BusinessException("فشل تهيئة محاولات السحب"));
    }

    @Transactional
    public UserDrawQuota simulateNextDay(Long userId) {
        LocalDate nextDate = currentQuota(userId).getQuotaDate().plusDays(1);
        return quotaRepository.findByUserIdAndQuotaDate(userId, nextDate)
                .orElseGet(() -> quotaRepository.save(new UserDrawQuota(userId, properties.getDailyFreeQuota(), nextDate)));
    }

    @Transactional
    public UserDrawQuota consumeOne(Long userId) {
        UserDrawQuota quota = currentQuotaForUpdate(userId);
        return consumeLockedQuota(quota);
    }

    public UserDrawQuota consumeLockedQuota(UserDrawQuota quota) {
        if (quota.available() <= 0) {
            throw new BusinessException("لا توجد محاولات سحب كافية");
        }
        quota.consumeOne();
        return quotaRepository.save(quota);
    }

    @Transactional
    public UserDrawQuota resetByCourse(Long userId) {
        UserDrawQuota quota = currentQuotaForUpdate(userId);
        if (quota.getCourseResetCountToday() >= properties.getMaxCourseResetPerDay()) {
            throw new BusinessException("وصلت إعادة ضبط إكمال الدرس اليوم إلى الحد الأقصى");
        }
        if (quota.available() > 0) {
            throw new BusinessException("لا تزال هناك محاولات سحب، لا حاجة لإعادة الضبط");
        }
        quota.setFreeQuota(properties.getDailyFreeQuota());
        quota.setCourseResetCountToday(quota.getCourseResetCountToday() + 1);
        return quotaRepository.save(quota);
    }

    @Transactional
    public UserDrawQuota addPracticeExtra(Long userId) {
        UserDrawQuota quota = currentQuotaForUpdate(userId);
        if (quota.getPracticeExtraCountToday() >= properties.getMaxPracticeExtraPerDay()) {
            throw new BusinessException("وصلت محاولات التدريب الإضافية اليوم إلى الحد الأقصى");
        }
        quota.setExtraQuota(quota.getExtraQuota() + 1);
        quota.setPracticeExtraCountToday(quota.getPracticeExtraCountToday() + 1);
        return quotaRepository.save(quota);
    }

    public Instant nextResetAt(UserDrawQuota quota) {
        return clock.nextResetAt(quota.getQuotaDate());
    }

    private LocalDate currentTestingDate(Long userId) {
        LocalDate businessDate = clock.businessDate();
        return quotaRepository.findAll().stream()
                .filter(quota -> quota.getUserId().equals(userId))
                .map(UserDrawQuota::getQuotaDate)
                .max(LocalDate::compareTo)
                .filter(latestDate -> latestDate.isAfter(businessDate))
                .orElse(businessDate);
    }

    private void ensureQuotaExists(Long userId, LocalDate quotaDate) {
        quotaRepository.insertIfAbsent(userId, properties.getDailyFreeQuota(), quotaDate);
    }
}
