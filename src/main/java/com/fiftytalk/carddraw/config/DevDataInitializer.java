package com.fiftytalk.carddraw.config;

import com.fiftytalk.carddraw.domain.*;
import com.fiftytalk.carddraw.entity.*;
import com.fiftytalk.carddraw.repository.*;
import com.fiftytalk.carddraw.util.ResourceUrlUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevDataInitializer {
    @Bean
    CommandLineRunner initDevData(
            CardPoolRepository poolRepository,
            DrawActivityRepository activityRepository,
            DrawSlotRuleRepository slotRuleRepository,
            NewCardProtectionRuleRepository protectionRuleRepository,
            CardRepository cardRepository,
            CardDrawProperties properties
    ) {
        return args -> {
            if (poolRepository.count() > 0) {
                return;
            }

            CardPool pool = poolRepository.save(new CardPool(
                    "v1",
                    "كأس العالم الحماسي",
                    "https://cdn.example.com/card-pack/v1.png",
                    1,
                    PoolStatus.ONLINE
            ));
            activityRepository.save(new DrawActivity("كأس العالم الحماسي", pool.getId(), ActivityStatus.ONLINE));

            slotRuleRepository.saveAll(DefaultDrawSlotRules.forPool(pool.getId()));
            protectionRuleRepository.saveAll(NewCardProtectionPolicy.defaultRules().stream()
                    .map(NewCardProtectionPolicy.RuleSpec::toEntity)
                    .toList());

            seedCards(cardRepository, properties);
        };
    }

    private void seedCards(CardRepository cardRepository, CardDrawProperties properties) {
        int sort = 1;
        for (WordCardSeedData.WordCardSeed seed : WordCardSeedData.cards()) {
            cardRepository.save(new Card(
                    seed.word(),
                    seed.meaning(),
                    seed.arabicMeaning(),
                    ResourceUrlUtils.cdnUrl(properties.getCdnBaseUrl(), "uploads/card-draw/audio/v1/" + seed.word() + ".mp3"),
                    "https://cdn.example.com/card/" + seed.rarity().name().toLowerCase() + "/" + seed.word() + ".png",
                    seed.rarity(),
                    "v1",
                    sort++
            ));
        }
    }
}
