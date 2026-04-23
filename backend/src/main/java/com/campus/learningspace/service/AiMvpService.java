package com.campus.learningspace.service;

import com.campus.learningspace.entity.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AiMvpService {
    private final MyCourseService myCourseService;
    private final TimeSlotService timeSlotService;
    private final UserAvailabilityService userAvailabilityService;
    private final StudyPlanService studyPlanService;
    private final ReservationService reservationService;
    private final ResourceMarketItemService resourceMarketItemService;
    private final TeamRequestService teamRequestService;
    private final com.campus.learningspace.llm.LlmGateway llmGateway;

    public AiMvpService(MyCourseService myCourseService,
                        TimeSlotService timeSlotService,
                        UserAvailabilityService userAvailabilityService,
                        StudyPlanService studyPlanService,
                        ReservationService reservationService,
                        ResourceMarketItemService resourceMarketItemService,
                        TeamRequestService teamRequestService,
                        com.campus.learningspace.llm.LlmGateway llmGateway) {
        this.myCourseService = myCourseService;
        this.timeSlotService = timeSlotService;
        this.userAvailabilityService = userAvailabilityService;
        this.studyPlanService = studyPlanService;
        this.reservationService = reservationService;
        this.resourceMarketItemService = resourceMarketItemService;
        this.teamRequestService = teamRequestService;
        this.llmGateway = llmGateway;
    }

    public AiPlanGenerateResponse generatePlan(AiPlanGenerateRequest req) {
        AiPlanGenerateResponse resp = new AiPlanGenerateResponse();
        resp.setStartDate(req.getStartDate());
        resp.setGoalType(req.getGoalType());
        resp.setGoalText(req.getGoalText());
        resp.setDailyMinutes(req.getDailyMinutes());

        int days = req.getDays() == null ? 7 : Math.max(1, Math.min(14, req.getDays()));
        List<AiPlanDayVO> list = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = req.getStartDate().plusDays(i);
            AiPlanDayVO day = new AiPlanDayVO();
            day.setDate(date);

            String suggested = pickSuggestedSlot(req.getUserId(), date);
            AiPlanTaskVO task = new AiPlanTaskVO();
            task.setTitle(buildTaskTitle(req, i));
            task.setMinutes(req.getDailyMinutes() == null ? 120 : Math.max(15, req.getDailyMinutes()));
            task.setSuggestedSlotLabel(suggested);

            day.setTasks(List.of(task));
            list.add(day);
        }
        resp.setDays(list);
        return resp;
    }

    /** 生成并保存为学习计划（study_plan），每一天存一条记录 */
    public AiPlanGenerateResponse generateAndSave(AiPlanGenerateRequest req) {
        AiPlanGenerateResponse resp = generatePlan(req);
        List<Long> savedIds = new ArrayList<>();
        if (resp.getDays() != null) {
            for (AiPlanDayVO d : resp.getDays()) {
                if (d == null || d.getDate() == null || d.getTasks() == null || d.getTasks().isEmpty()) continue;
                AiPlanTaskVO t = d.getTasks().get(0);
                StudyPlan plan = new StudyPlan();
                plan.setId(null);
                plan.setUserId(req.getUserId());
                plan.setTeamRequestId(null);
                plan.setReservationId(null);
                plan.setPlanDate(d.getDate());
                String title = t.getTitle() == null ? "" : t.getTitle().trim();
                plan.setTitle(title.isBlank() ? "AI 学习任务" : title);
                String desc = "AI 生成计划\n"
                        + "建议时段：" + (t.getSuggestedSlotLabel() == null ? "" : t.getSuggestedSlotLabel())
                        + "\n预计用时：" + (t.getMinutes() == null ? "" : t.getMinutes()) + " 分钟";
                plan.setDescription(desc);
                plan.setStatus(1);
                studyPlanService.save(plan);
                if (plan.getId() != null) savedIds.add(plan.getId());
            }
        }
        resp.setSavedStudyPlanIds(savedIds);
        return resp;
    }

    public AiChatResponse chat(AiChatRequest req) {
        String lastUser = "";
        if (req.getMessages() != null) {
            for (int i = req.getMessages().size() - 1; i >= 0; i--) {
                AiChatMessage m = req.getMessages().get(i);
                if (m != null && "user".equalsIgnoreCase(m.getRole())) {
                    lastUser = m.getContent() == null ? "" : m.getContent().trim();
                    break;
                }
            }
        }

        var llmMessages = (req.getMessages() == null) ? java.util.List.<com.campus.learningspace.llm.LlmMessage>of()
                : req.getMessages().stream().map(m -> {
                    com.campus.learningspace.llm.LlmMessage x = new com.campus.learningspace.llm.LlmMessage();
                    x.setRole(m.getRole());
                    x.setContent(m.getContent());
                    return x;
                }).toList();

        String text = llmGateway.chatText(req.getScene(), llmMessages)
                + "\n\n---\n（已附：资源市场/协作小组的规则推荐，后续可接知识库与真实大模型进一步增强）";

        AiChatResponse r = new AiChatResponse();
        r.setText(text);
        r.setProvider(llmGateway.provider());

        String kw = extractKeyword(lastUser);
        if (!kw.isBlank()) {
            r.setResources(searchResources(kw));
            r.setTeams(searchTeams(kw));
        }
        return r;
    }

    public AiDeviationCheckResponse checkDeviation(Long userId, LocalDate date) {
        if (date == null) date = LocalDate.now();
        AiDeviationCheckResponse r = new AiDeviationCheckResponse();
        r.setUserId(userId);
        r.setDate(date);

        // 1) 逾期学习计划：plan_date <= date 且 status=1（进行中）
        var overdue = studyPlanService.lambdaQuery()
                .eq(StudyPlan::getUserId, userId)
                .le(StudyPlan::getPlanDate, date)
                .eq(StudyPlan::getStatus, 1)
                .eq(StudyPlan::getDeleted, 0)
                .list()
                .stream()
                .map(StudyPlan::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        r.setOverdueStudyPlanIds(overdue);

        // 2) 今日预约未签到：reservation_date=date 且 status=1（待签到）且 start_time < now
        var now = java.time.LocalTime.now();
        var missed = reservationService.lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getReservationDate, date)
                .eq(Reservation::getStatus, 1)
                .lt(Reservation::getStartTime, now)
                .eq(Reservation::getDeleted, 0)
                .list()
                .stream()
                .map(Reservation::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        r.setMissedCheckinReservationIds(missed);
        return r;
    }

    private String extractKeyword(String q) {
        if (q == null) return "";
        String s = q.trim();
        if (s.isBlank()) return "";
        // 去掉常见标点，避免 like 全命中
        s = s.replaceAll("[\\p{Punct}，。！？、；：“”‘’（）()【】\\s]+", " ").trim();
        if (s.isBlank()) return "";
        // 取前 12 个字符作为关键词（简单粗筛）
        return s.length() > 12 ? s.substring(0, 12) : s;
    }

    private List<AiRecommendResourceVO> searchResources(String kw) {
        var rows = resourceMarketItemService.lambdaQuery()
                .eq(ResourceMarketItem::getStatus, 1)
                .and(w -> w.like(ResourceMarketItem::getTitle, kw).or().like(ResourceMarketItem::getDescription, kw))
                .orderByDesc(ResourceMarketItem::getCreateTime)
                .last("LIMIT 5")
                .list();
        if (rows == null || rows.isEmpty()) return List.of();
        // 补发布者名（service 内部有 fillPublisherNames，但未暴露 search 方法；这里复用 listMarket 的填充逻辑不可见）
        // 简化：直接返回基础字段，publisherName 可能为空（前端可降级显示）
        return rows.stream().map(it -> {
            AiRecommendResourceVO vo = new AiRecommendResourceVO();
            vo.setId(it.getId());
            vo.setTitle(it.getTitle());
            vo.setCategory(it.getCategory());
            vo.setIsFree(it.getIsFree());
            vo.setPrice(it.getPrice() == null ? null : it.getPrice().doubleValue());
            vo.setPublisherName(it.getPublisherName());
            return vo;
        }).toList();
    }

    private List<AiRecommendTeamVO> searchTeams(String kw) {
        var rows = teamRequestService.lambdaQuery()
                .in(TeamRequest::getStatus, 1, 2)
                .and(w -> w.like(TeamRequest::getTitle, kw).or().like(TeamRequest::getDescription, kw).or().like(TeamRequest::getTags, kw))
                .orderByDesc(TeamRequest::getCreateTime)
                .last("LIMIT 5")
                .list();
        if (rows == null || rows.isEmpty()) return List.of();
        return rows.stream().map(it -> {
            AiRecommendTeamVO vo = new AiRecommendTeamVO();
            vo.setId(it.getId());
            vo.setTitle(it.getTitle());
            vo.setStatus(it.getStatus());
            vo.setCurrentCount(it.getCurrentCount());
            vo.setExpectedCount(it.getExpectedCount());
            return vo;
        }).toList();
    }

    private String buildTaskTitle(AiPlanGenerateRequest req, int idx) {
        String g = req.getGoalText() == null ? "" : req.getGoalText().trim();
        if (g.isBlank()) g = "学习目标";
        String prefix = "学习任务";
        if ("exam".equalsIgnoreCase(req.getGoalType())) prefix = "考研任务";
        if ("cert".equalsIgnoreCase(req.getGoalType())) prefix = "考证任务";
        return prefix + "：" + g + "（第" + (idx + 1) + "天）";
    }

    /**
     * 从 time_slot 中选一个“尽量避开当天课程”的建议时段。
     * 简化策略：
     * - 若用户当天有课：优先选“最后一个不冲突的 time_slot”
     * - 否则：选第一个 time_slot
     */
    private String pickSuggestedSlot(Long userId, LocalDate date) {
        var allSlots = timeSlotService.getActiveSlots();
        if (allSlots == null || allSlots.isEmpty()) return "建议课后/周末空闲时段";

        // 若用户设置了可用时段：仅在可用集合内挑选
        int weekDay = date.getDayOfWeek().getValue();
        var avail = userAvailabilityService.lambdaQuery()
                .eq(com.campus.learningspace.entity.UserAvailability::getUserId, userId)
                .eq(com.campus.learningspace.entity.UserAvailability::getWeekDay, weekDay)
                .eq(com.campus.learningspace.entity.UserAvailability::getDeleted, 0)
                .list();
        var slots = (avail == null || avail.isEmpty())
                ? allSlots
                : allSlots.stream().filter(s -> avail.stream().anyMatch(a -> a.getSlotId() != null && a.getSlotId().equals(s.getId()))).toList();
        if (slots.isEmpty()) slots = allSlots;

        List<MyCourseItemVO> courses = myCourseService.getDayCourses(userId, date, null);
        if (courses == null || courses.isEmpty()) {
            return slots.get(0).getLabel();
        }

        return slots.stream()
                .filter(s -> !overlapsAnyCourse(s.getStartTime(), s.getEndTime(), courses))
                .max(Comparator.comparing(t -> t.getStartTime()))
                .map(TimeSlot::getLabel)
                .orElse(slots.get(slots.size() - 1).getLabel());
    }

    private boolean overlapsAnyCourse(LocalTime slotStart, LocalTime slotEnd, List<MyCourseItemVO> courses) {
        for (MyCourseItemVO c : courses) {
            if (c == null || c.getStartTime() == null || c.getEndTime() == null) continue;
            if (slotStart.isBefore(c.getEndTime()) && slotEnd.isAfter(c.getStartTime())) {
                return true;
            }
        }
        return false;
    }
}

