package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.*;
import com.campus.learningspace.service.AiMvpService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiController {
    private final AiMvpService aiMvpService;

    public AiController(AiMvpService aiMvpService) {
        this.aiMvpService = aiMvpService;
    }

    @PostMapping("/plan/generate")
    public Result<AiPlanGenerateResponse> generatePlan(@RequestBody AiPlanGenerateRequest req) {
        if (req == null) return Result.error(400, "请求不能为空");
        if (req.getUserId() == null) return Result.error(400, "缺少 userId");
        if (req.getStartDate() == null) return Result.error(400, "缺少 startDate");
        if (req.getGoalText() == null || req.getGoalText().trim().isEmpty()) return Result.error(400, "缺少 goalText");
        if (req.getDailyMinutes() == null || req.getDailyMinutes() <= 0) return Result.error(400, "dailyMinutes 必须大于 0");
        return Result.success(aiMvpService.generatePlan(req));
    }

    @PostMapping("/plan/generate-save")
    public Result<AiPlanGenerateResponse> generateAndSave(@RequestBody AiPlanGenerateRequest req) {
        if (req == null) return Result.error(400, "请求不能为空");
        if (req.getUserId() == null) return Result.error(400, "缺少 userId");
        if (req.getStartDate() == null) return Result.error(400, "缺少 startDate");
        if (req.getGoalText() == null || req.getGoalText().trim().isEmpty()) return Result.error(400, "缺少 goalText");
        if (req.getDailyMinutes() == null || req.getDailyMinutes() <= 0) return Result.error(400, "dailyMinutes 必须大于 0");
        return Result.success(aiMvpService.generateAndSave(req));
    }

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest req) {
        if (req == null) return Result.error(400, "请求不能为空");
        if (req.getUserId() == null) return Result.error(400, "缺少 userId");
        return Result.success(aiMvpService.chat(req));
    }

    @GetMapping("/deviation/check")
    public Result<AiDeviationCheckResponse> checkDeviation(
            @RequestParam Long userId,
            @RequestParam(required = false) String date
    ) {
        if (userId == null) return Result.error(400, "缺少 userId");
        java.time.LocalDate d = (date == null || date.isBlank()) ? java.time.LocalDate.now() : java.time.LocalDate.parse(date);
        return Result.success(aiMvpService.checkDeviation(userId, d));
    }
}

