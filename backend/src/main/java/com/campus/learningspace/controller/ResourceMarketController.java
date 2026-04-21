package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.CampusCredit;
import com.campus.learningspace.entity.ResourceMarketItem;
import com.campus.learningspace.entity.ResourceMarketTrade;
import com.campus.learningspace.service.CampusCreditService;
import com.campus.learningspace.service.ResourceMarketItemService;
import com.campus.learningspace.service.ResourceMarketTradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resource-market")
@CrossOrigin
public class ResourceMarketController {

    @Autowired
    private ResourceMarketItemService itemService;

    @Autowired
    private ResourceMarketTradeService tradeService;

    @Autowired
    private CampusCreditService campusCreditService;

    @GetMapping("/items")
    public Result<List<ResourceMarketItem>> listItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean freeOnly
    ) {
        return Result.success(itemService.listMarket(category, freeOnly));
    }

    @GetMapping("/items/my/{userId}")
    public Result<List<ResourceMarketItem>> myItems(@PathVariable Long userId) {
        return Result.success(itemService.listMyItems(userId));
    }

    @PostMapping("/items")
    public Result<ResourceMarketItem> publish(@RequestBody ResourceMarketItem item) {
        return Result.success(itemService.publish(item));
    }

    @GetMapping("/items/pending")
    public Result<List<ResourceMarketItem>> pendingAuditItems() {
        return Result.success(itemService.listPendingAudit());
    }

    @PostMapping("/items/{itemId}/audit")
    public Result<ResourceMarketItem> audit(
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> body
    ) {
        Long reviewerId = Long.valueOf(String.valueOf(body.get("reviewerId")));
        boolean approve = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approve", "false")));
        String remark = String.valueOf(body.getOrDefault("remark", ""));
        return Result.success(itemService.audit(itemId, reviewerId, approve, remark));
    }

    @PostMapping("/items/{itemId}/intent")
    public Result<ResourceMarketTrade> createTradeIntent(
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> body
    ) {
        Long userId = Long.valueOf(String.valueOf(body.get("userId")));
        String note = String.valueOf(body.getOrDefault("note", ""));
        String meetingPlace = String.valueOf(body.getOrDefault("meetingPlace", "图书馆服务台"));
        return Result.success(itemService.createTradeIntent(itemId, userId, note, meetingPlace));
    }

    @PostMapping("/trades/{tradeId}/confirm")
    public Result<ResourceMarketTrade> confirmHandover(
            @PathVariable Long tradeId,
            @RequestBody Map<String, Object> body
    ) {
        Long userId = Long.valueOf(String.valueOf(body.get("userId")));
        return Result.success(itemService.confirmHandover(tradeId, userId));
    }

    @GetMapping("/trades/user/{userId}")
    public Result<List<ResourceMarketTrade>> userTrades(@PathVariable Long userId) {
        return Result.success(tradeService.getUserTrades(userId));
    }

    @GetMapping("/credit/{userId}")
    public Result<CampusCredit> credit(@PathVariable Long userId) {
        return Result.success(campusCreditService.refreshAndGet(userId));
    }
}
