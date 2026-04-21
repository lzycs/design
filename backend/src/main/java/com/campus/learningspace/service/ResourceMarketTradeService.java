package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.ResourceMarketItem;
import com.campus.learningspace.entity.ResourceMarketTrade;
import com.campus.learningspace.entity.User;
import com.campus.learningspace.mapper.ResourceMarketItemMapper;
import com.campus.learningspace.mapper.ResourceMarketTradeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResourceMarketTradeService extends ServiceImpl<ResourceMarketTradeMapper, ResourceMarketTrade> {

    @Autowired
    private ResourceMarketItemMapper itemMapper;

    @Autowired
    private UserService userService;

    public List<ResourceMarketTrade> getUserTrades(Long userId) {
        LambdaQueryWrapper<ResourceMarketTrade> w = new LambdaQueryWrapper<>();
        w.and(q -> q.eq(ResourceMarketTrade::getPublisherId, userId)
                        .or()
                        .eq(ResourceMarketTrade::getReceiverId, userId))
                .orderByDesc(ResourceMarketTrade::getCreateTime);
        List<ResourceMarketTrade> rows = list(w);
        fillDisplayFields(rows);
        return rows;
    }

    private void fillDisplayFields(List<ResourceMarketTrade> rows) {
        if (rows == null || rows.isEmpty()) return;

        Set<Long> itemIds = rows.stream()
                .map(ResourceMarketTrade::getItemId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, ResourceMarketItem> itemMap = itemIds.isEmpty()
                ? Map.of()
                : itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(ResourceMarketItem::getId, i -> i, (a, b) -> a));

        Set<Long> publisherIds = rows.stream()
                .map(ResourceMarketTrade::getPublisherId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = publisherIds.isEmpty()
                ? Map.of()
                : userService.listByIds(publisherIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        for (ResourceMarketTrade row : rows) {
            ResourceMarketItem item = itemMap.get(row.getItemId());
            row.setItemTitle(item != null ? item.getTitle() : null);

            User publisher = userMap.get(row.getPublisherId());
            if (publisher == null) {
                row.setPublisherName("未知用户");
            } else {
                String name = (publisher.getRealName() != null && !publisher.getRealName().isBlank())
                        ? publisher.getRealName()
                        : (publisher.getUsername() != null && !publisher.getUsername().isBlank()
                        ? publisher.getUsername()
                        : "未知用户");
                row.setPublisherName(name);
            }
        }
    }
}
