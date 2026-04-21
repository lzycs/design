package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.ResourceMarketItem;
import com.campus.learningspace.entity.ResourceMarketTrade;
import com.campus.learningspace.entity.User;
import com.campus.learningspace.mapper.ResourceMarketItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResourceMarketItemService extends ServiceImpl<ResourceMarketItemMapper, ResourceMarketItem> {

    @Autowired
    private UserService userService;

    @Autowired
    private ResourceMarketTradeService tradeService;

    @Autowired
    private CampusCreditService campusCreditService;

    public List<ResourceMarketItem> listMarket(String category, Boolean freeOnly) {
        LambdaQueryWrapper<ResourceMarketItem> w = new LambdaQueryWrapper<>();
        w.eq(ResourceMarketItem::getStatus, 1)
                .eq(Boolean.TRUE.equals(freeOnly), ResourceMarketItem::getIsFree, 1)
                .eq(category != null && !category.isBlank(), ResourceMarketItem::getCategory, category)
                .orderByDesc(ResourceMarketItem::getCreateTime);
        List<ResourceMarketItem> rows = list(w);
        fillPublisherNames(rows);
        return rows;
    }

    public List<ResourceMarketItem> listMyItems(Long userId) {
        LambdaQueryWrapper<ResourceMarketItem> w = new LambdaQueryWrapper<>();
        w.eq(ResourceMarketItem::getUserId, userId)
                .orderByDesc(ResourceMarketItem::getCreateTime);
        List<ResourceMarketItem> rows = list(w);
        fillPublisherNames(rows);
        return rows;
    }

    public List<ResourceMarketItem> listPendingAudit() {
        LambdaQueryWrapper<ResourceMarketItem> w = new LambdaQueryWrapper<>();
        w.eq(ResourceMarketItem::getStatus, 0)
                .orderByAsc(ResourceMarketItem::getCreateTime);
        List<ResourceMarketItem> rows = list(w);
        fillPublisherNames(rows);
        return rows;
    }

    @Transactional
    public ResourceMarketItem publish(ResourceMarketItem item) {
        if (item.getUserId() == null) throw new RuntimeException("缺少 userId");
        int score = campusCreditService.getScore(item.getUserId());
        if (score < 60) throw new RuntimeException("信用分过低，暂不可发布资源");
        if (item.getIsFree() == null) {
            item.setIsFree(0);
        }
        if (item.getIsFree() == 1) {
            item.setPrice(null);
        }
        if (item.getStatus() == null) item.setStatus(0);
        save(item);
        return item;
    }

    @Transactional
    public ResourceMarketItem audit(Long itemId, Long reviewerId, boolean approve, String remark) {
        ResourceMarketItem item = getById(itemId);
        if (item == null) throw new RuntimeException("资源不存在");
        User reviewer = userService.getById(reviewerId);
        if (reviewer == null || (reviewer.getRole() == null || (reviewer.getRole() != 3 && reviewer.getRole() != 5))) {
            throw new RuntimeException("仅管理员或审核员可审核");
        }
        item.setStatus(approve ? 1 : 2);
        item.setAuditBy(reviewerId);
        item.setAuditRemark(remark);
        item.setAuditTime(LocalDateTime.now());
        updateById(item);
        return item;
    }

    @Transactional
    public ResourceMarketTrade createTradeIntent(Long itemId, Long receiverId, String note, String meetingPlace) {
        ResourceMarketItem item = getById(itemId);
        if (item == null || item.getStatus() == null || item.getStatus() != 1) {
            throw new RuntimeException("该资源不可交易");
        }
        if (item.getUserId().equals(receiverId)) {
            throw new RuntimeException("不能交易自己发布的资源");
        }
        int score = campusCreditService.getScore(receiverId);
        if (score < 60) throw new RuntimeException("信用分过低，暂不可发起交易");

        item.setStatus(4);
        updateById(item);

        ResourceMarketTrade trade = new ResourceMarketTrade();
        trade.setItemId(itemId);
        trade.setPublisherId(item.getUserId());
        trade.setReceiverId(receiverId);
        trade.setStatus(1);
        trade.setMeetingPlace(meetingPlace);
        trade.setNote(note);
        trade.setConfirmPublisher(0);
        trade.setConfirmReceiver(0);
        tradeService.save(trade);
        return trade;
    }

    @Transactional
    public ResourceMarketTrade confirmHandover(Long tradeId, Long confirmerId) {
        ResourceMarketTrade trade = tradeService.getById(tradeId);
        if (trade == null) throw new RuntimeException("交易记录不存在");
        if (trade.getStatus() == null || trade.getStatus() != 1) throw new RuntimeException("该交易不可确认");

        if (confirmerId.equals(trade.getPublisherId())) {
            trade.setConfirmPublisher(1);
        } else if (confirmerId.equals(trade.getReceiverId())) {
            trade.setConfirmReceiver(1);
        } else {
            throw new RuntimeException("无确认权限");
        }

        if (trade.getConfirmPublisher() != null && trade.getConfirmPublisher() == 1
                && trade.getConfirmReceiver() != null && trade.getConfirmReceiver() == 1) {
            trade.setStatus(2);
            trade.setConfirmTime(LocalDateTime.now());
            ResourceMarketItem item = getById(trade.getItemId());
            if (item != null) {
                item.setStatus(5);
                updateById(item);
            }
        }
        tradeService.updateById(trade);
        return trade;
    }

    private void fillPublisherNames(List<ResourceMarketItem> rows) {
        if (rows == null || rows.isEmpty()) return;
        Set<Long> userIds = rows.stream()
                .map(ResourceMarketItem::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return;
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        if (userMap.isEmpty()) userMap = Collections.emptyMap();
        for (ResourceMarketItem row : rows) {
            User u = userMap.get(row.getUserId());
            if (u == null) {
                row.setPublisherName("未知用户");
            } else {
                String name = (u.getRealName() != null && !u.getRealName().isBlank())
                        ? u.getRealName()
                        : (u.getUsername() != null && !u.getUsername().isBlank() ? u.getUsername() : "未知用户");
                row.setPublisherName(name);
            }
        }
    }
}
