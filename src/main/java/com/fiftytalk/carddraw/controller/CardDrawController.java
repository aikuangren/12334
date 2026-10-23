package com.fiftytalk.carddraw.controller;

import com.fiftytalk.carddraw.common.ApiResponse;
import com.fiftytalk.carddraw.domain.PoolStatus;
import com.fiftytalk.carddraw.dto.*;
import com.fiftytalk.carddraw.repository.CardPoolRepository;
import com.fiftytalk.carddraw.service.DrawService;
import com.fiftytalk.carddraw.service.QuotaService;
import com.fiftytalk.carddraw.service.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/card-draw")
public class CardDrawController {
    private final CardPoolRepository poolRepository;
    private final DrawService drawService;
    private final QuotaService quotaService;
    private final SimulationService simulationService;

    public CardDrawController(CardPoolRepository poolRepository, DrawService drawService, QuotaService quotaService, SimulationService simulationService) {
        this.poolRepository = poolRepository;
        this.drawService = drawService;
        this.quotaService = quotaService;
        this.simulationService = simulationService;
    }

    @GetMapping("/pools")
    public ApiResponse<List<CardPoolDto>> pools() {
        return ApiResponse.ok(poolRepository.findByStatusOrderByDisplaySortNoAscIdAsc(PoolStatus.ONLINE).stream()
                .map(CardPoolDto::from)
                .toList());
    }

    @GetMapping("/activity")
    public ApiResponse<ActivityResponse> activity(
            @RequestParam Long userId,
            @RequestParam(required = false) String poolVersion
    ) {
        return ApiResponse.ok(drawService.activity(userId, poolVersion));
    }

    @PostMapping("/draw")
    public ApiResponse<DrawResponse> draw(
            @RequestParam Long userId,
            @RequestParam(required = false) String poolVersion,
            @RequestParam(required = false) String requestId
    ) {
        return ApiResponse.ok(drawService.draw(userId, poolVersion, requestId));
    }

    @GetMapping("/my-cards")
    public ApiResponse<MyCardsResponse> myCards(
            @RequestParam Long userId,
            @RequestParam(required = false) String poolVersion
    ) {
        return ApiResponse.ok(drawService.myCards(userId, poolVersion));
    }

    @GetMapping("/history")
    public ApiResponse<List<HistoryResponse>> history(
            @RequestParam Long userId,
            @RequestParam(required = false) String poolVersion
    ) {
        return ApiResponse.ok(drawService.history(userId, poolVersion));
    }

    @GetMapping("/simulation")
    public ApiResponse<SimulationResponse> simulation(
            @RequestParam(defaultValue = "10000") Integer packs,
            @RequestParam(required = false) String poolVersion
    ) {
        return ApiResponse.ok(simulationService.simulate(packs, poolVersion));
    }

    @PostMapping("/reset-by-course")
    public ApiResponse<QuotaDto> resetByCourse(@RequestParam Long userId) {
        var quota = quotaService.resetByCourse(userId);
        return ApiResponse.ok(QuotaDto.from(quota, quotaService.nextResetAt(quota)));
    }

    @PostMapping("/gain-by-practice")
    public ApiResponse<QuotaDto> gainByPractice(@RequestParam Long userId) {
        var quota = quotaService.addPracticeExtra(userId);
        return ApiResponse.ok(QuotaDto.from(quota, quotaService.nextResetAt(quota)));
    }

    @PostMapping("/simulate-next-day")
    public ApiResponse<QuotaDto> simulateNextDay(@RequestParam Long userId) {
        var quota = quotaService.simulateNextDay(userId);
        return ApiResponse.ok(QuotaDto.from(quota, quotaService.nextResetAt(quota)));
    }
}
