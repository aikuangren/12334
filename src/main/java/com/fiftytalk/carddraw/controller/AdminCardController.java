package com.fiftytalk.carddraw.controller;

import com.fiftytalk.carddraw.common.ApiResponse;
import com.fiftytalk.carddraw.common.BusinessException;
import com.fiftytalk.carddraw.config.WordCardSeedData;
import com.fiftytalk.carddraw.domain.CardStatus;
import com.fiftytalk.carddraw.dto.AdminCardImportResponse;
import com.fiftytalk.carddraw.dto.AdminCardRequest;
import com.fiftytalk.carddraw.entity.Card;
import com.fiftytalk.carddraw.repository.CardRepository;
import com.fiftytalk.carddraw.service.AdminCardImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/admin/cards")
public class AdminCardController {
    private final CardRepository cardRepository;
    private final AdminCardImportService importService;

    public AdminCardController(CardRepository cardRepository, AdminCardImportService importService) {
        this.cardRepository = cardRepository;
        this.importService = importService;
    }

    @GetMapping
    public ApiResponse<List<Card>> list(
            @RequestParam(required = false, defaultValue = "v1") String poolVersion,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) com.fiftytalk.carddraw.domain.Rarity rarity,
            @RequestParam(required = false) String keyword
    ) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Card> cards = cardRepository.findAll().stream()
                .filter(card -> card.getPoolVersion().equals(poolVersion))
                .filter(card -> status == null || card.getStatus() == status)
                .filter(card -> rarity == null || card.getRarity() == rarity)
                .filter(card -> normalizedKeyword.isEmpty()
                        || card.getWord().toLowerCase().contains(normalizedKeyword)
                        || (card.getMeaning() != null && card.getMeaning().toLowerCase().contains(normalizedKeyword))
                        || (card.getArabicMeaning() != null && card.getArabicMeaning().toLowerCase().contains(normalizedKeyword)))
                .sorted(Comparator.comparing(Card::getSortNo).thenComparing(Card::getId))
                .toList();
        return ApiResponse.ok(cards);
    }

    @PostMapping
    public ApiResponse<Card> create(@Valid @RequestBody AdminCardRequest request) {
        Card card = new Card(
                request.word(),
                request.meaning(),
                request.arabicMeaning(),
                request.audioUrl(),
                request.imageUrl(),
                request.rarity(),
                request.poolVersion(),
                request.sortNo() == null ? 0 : request.sortNo()
        );
        if (request.status() != null) {
            card.setStatus(request.status());
        }
        return ApiResponse.ok(cardRepository.save(card));
    }

    @PostMapping("/import-excel")
    public ApiResponse<AdminCardImportResponse> importExcel(
            @RequestParam MultipartFile file,
            @RequestParam(required = false, defaultValue = "v1") String poolVersion,
            @RequestParam(required = false, defaultValue = "true") boolean skipDuplicates
    ) {
        return ApiResponse.ok(importService.importExcel(file, poolVersion, skipDuplicates));
    }

    @PostMapping("/sync-seed-arabic")
    public ApiResponse<Map<String, Object>> syncSeedArabic() {
        Map<String, String> arabicByKey = new LinkedHashMap<>();
        for (WordCardSeedData.WordCardSeed seed : WordCardSeedData.cards()) {
            if (seed.arabicMeaning() != null && !seed.arabicMeaning().isBlank()) {
                arabicByKey.put(cardKey(seed.word(), seed.rarity().name()), seed.arabicMeaning());
            }
        }
        int updated = 0;
        int matched = 0;
        for (Card card : cardRepository.findAll()) {
            String arabicMeaning = arabicByKey.get(cardKey(card.getWord(), card.getRarity().name()));
            if (arabicMeaning == null) {
                continue;
            }
            matched++;
            if (!Objects.equals(card.getArabicMeaning(), arabicMeaning)) {
                card.setArabicMeaning(arabicMeaning);
                cardRepository.save(card);
                updated++;
            }
        }
        return ApiResponse.ok(Map.of("matched", matched, "updated", updated));
    }

    @PutMapping("/{id}")
    public ApiResponse<Card> update(@PathVariable Long id, @Valid @RequestBody AdminCardRequest request) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new BusinessException("البطاقة غير موجودة"));
        card.setWord(request.word());
        card.setMeaning(request.meaning());
        card.setArabicMeaning(request.arabicMeaning());
        card.setAudioUrl(request.audioUrl());
        card.setImageUrl(request.imageUrl());
        card.setRarity(request.rarity());
        card.setPoolVersion(request.poolVersion());
        card.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        if (request.status() != null) {
            card.setStatus(request.status());
        }
        return ApiResponse.ok(cardRepository.save(card));
    }

    @PostMapping("/{id}/offline")
    public ApiResponse<Card> offline(@PathVariable Long id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new BusinessException("البطاقة غير موجودة"));
        card.setStatus(CardStatus.OFFLINE);
        return ApiResponse.ok(cardRepository.save(card));
    }

    @PostMapping("/{id}/online")
    public ApiResponse<Card> online(@PathVariable Long id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new BusinessException("البطاقة غير موجودة"));
        card.setStatus(CardStatus.ONLINE);
        return ApiResponse.ok(cardRepository.save(card));
    }

    private String cardKey(String word, String rarity) {
        return (word == null ? "" : word.trim().toLowerCase(Locale.ROOT)) + "|" + rarity;
    }
}
