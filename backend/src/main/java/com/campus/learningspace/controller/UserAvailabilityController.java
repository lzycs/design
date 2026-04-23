package com.campus.learningspace.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.UserAvailability;
import com.campus.learningspace.service.UserAvailabilityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-availability")
@CrossOrigin
public class UserAvailabilityController {
    private final UserAvailabilityService userAvailabilityService;

    public UserAvailabilityController(UserAvailabilityService userAvailabilityService) {
        this.userAvailabilityService = userAvailabilityService;
    }

    @GetMapping("/{userId}")
    public Result<List<UserAvailability>> get(@PathVariable Long userId) {
        return Result.success(userAvailabilityService.list(new LambdaQueryWrapper<UserAvailability>()
                .eq(UserAvailability::getUserId, userId)
                .eq(UserAvailability::getDeleted, 0)));
    }

    /**
     * body: { userId, items: [{ weekDay, slotId }, ...] }
     */
    @PutMapping
    public Result<Boolean> saveAll(@RequestBody Map<String, Object> body) {
        Object userIdObj = body == null ? null : body.get("userId");
        if (userIdObj == null) return Result.error(400, "缺少 userId");
        Long userId = Long.valueOf(userIdObj.toString());

        Object itemsObj = body.get("items");
        if (!(itemsObj instanceof List<?> items)) {
            return Result.error(400, "缺少 items");
        }

        // 简化实现：先清空再插入（逻辑删除表，直接 removeById 会触发逻辑删）
        userAvailabilityService.remove(new LambdaQueryWrapper<UserAvailability>()
                .eq(UserAvailability::getUserId, userId)
                .eq(UserAvailability::getDeleted, 0));

        for (Object it : items) {
            if (!(it instanceof Map<?, ?> m)) continue;
            Object wdObj = m.get("weekDay");
            Object slotIdObj = m.get("slotId");
            if (wdObj == null || slotIdObj == null) continue;
            int wd = Integer.parseInt(wdObj.toString());
            long slotId = Long.parseLong(slotIdObj.toString());
            if (wd < 1 || wd > 7) continue;

            UserAvailability ua = new UserAvailability();
            ua.setUserId(userId);
            ua.setWeekDay(wd);
            ua.setSlotId(slotId);
            userAvailabilityService.save(ua);
        }
        return Result.success(true);
    }
}

