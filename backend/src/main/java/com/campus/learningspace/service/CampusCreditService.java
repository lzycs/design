package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.CampusCredit;
import com.campus.learningspace.entity.ResourceMarketTrade;
import com.campus.learningspace.entity.User;
import com.campus.learningspace.mapper.CampusCreditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CampusCreditService extends ServiceImpl<CampusCreditMapper, CampusCredit> {

    @Autowired
    private UserService userService;

    @Autowired
    private ResourceMarketTradeService tradeService;

    public CampusCredit refreshAndGet(Long userId) {
        User user = userService.getById(userId);
        int score = 100;
        if (user != null) {
            int violation = user.getViolationCount() == null ? 0 : user.getViolationCount();
            int weekly = user.getWeeklyReservationCount() == null ? 0 : user.getWeeklyReservationCount();
            score -= violation * 12;
            score += Math.min(weekly, 10);
        }

        LambdaQueryWrapper<ResourceMarketTrade> doneW = new LambdaQueryWrapper<>();
        doneW.eq(ResourceMarketTrade::getStatus, 2)
                .and(w -> w.eq(ResourceMarketTrade::getPublisherId, userId)
                        .or()
                        .eq(ResourceMarketTrade::getReceiverId, userId));
        int doneCount = (int) tradeService.count(doneW);
        score += Math.min(doneCount * 2, 12);

        if (score > 100) score = 100;
        if (score < 0) score = 0;

        String comment = score >= 85 ? "信用优秀" : score >= 70 ? "信用良好" : score >= 60 ? "信用一般" : "信用偏低";

        LambdaQueryWrapper<CampusCredit> w = new LambdaQueryWrapper<>();
        w.eq(CampusCredit::getUserId, userId);
        CampusCredit existing = getOne(w);
        if (existing == null) {
            existing = new CampusCredit();
            existing.setUserId(userId);
            existing.setCreditScore(score);
            existing.setScoreComment(comment);
            save(existing);
        } else {
            existing.setCreditScore(score);
            existing.setScoreComment(comment);
            updateById(existing);
        }
        return existing;
    }

    public int getScore(Long userId) {
        CampusCredit credit = refreshAndGet(userId);
        return credit.getCreditScore() == null ? 0 : credit.getCreditScore();
    }
}
