package com.mido.pm.calendar.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.calendar.dto.ParticipantInputDTO;
import com.mido.pm.calendar.dto.ParticipantVO;
import com.mido.pm.calendar.dto.RsvpDTO;
import com.mido.pm.calendar.dto.ScheduleCreateDTO;
import com.mido.pm.calendar.dto.ScheduleUpdateDTO;
import com.mido.pm.calendar.dto.ScheduleExceptionDTO;
import com.mido.pm.calendar.dto.ScheduleVO;
import com.mido.pm.calendar.domain.RecurrenceExpander;
import com.mido.pm.calendar.entity.PmSchedule;
import com.mido.pm.calendar.entity.PmScheduleException;
import com.mido.pm.calendar.entity.PmScheduleParticipant;
import com.mido.pm.calendar.event.CalendarEvents;
import com.mido.pm.calendar.mapper.PmScheduleExceptionMapper;
import com.mido.pm.calendar.mapper.PmScheduleMapper;
import com.mido.pm.calendar.mapper.PmScheduleParticipantMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 日程服务：日程 CRUD、按时间段查询（当前用户的日历 + 参与的日程）、RSVP 反馈。
 * 写操作发 calendar.schedule.* / calendar.rsvp.responded 事件（订阅方见 docs/domain-events.md）。
 */
@Service
public class ScheduleService {

    private static final Set<String> RSVP_STATUSES = Set.of("accepted", "tentative", "declined");
    private static final long NONE_SENTINEL = -1L;

    private final PmScheduleMapper scheduleMapper;
    private final PmScheduleParticipantMapper participantMapper;
    private final PmScheduleExceptionMapper exceptionMapper;
    private final CalendarService calendarService;
    private final ResourceService resourceService;
    private final DomainEventPublisher eventPublisher;

    public ScheduleService(PmScheduleMapper scheduleMapper,
                           PmScheduleParticipantMapper participantMapper,
                           PmScheduleExceptionMapper exceptionMapper,
                           CalendarService calendarService,
                           ResourceService resourceService,
                           DomainEventPublisher eventPublisher) {
        this.scheduleMapper = scheduleMapper;
        this.participantMapper = participantMapper;
        this.exceptionMapper = exceptionMapper;
        this.calendarService = calendarService;
        this.resourceService = resourceService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ScheduleCreateDTO dto) {
        validateRange(dto.startTime(), dto.endTime());
        Long calendarId = calendarService.resolveCalendarId(dto.calendarId());
        Long organizerId = currentUserId();

        PmSchedule s = new PmSchedule();
        s.setCalendarId(calendarId);
        s.setTitle(dto.title());
        s.setDescription(dto.description());
        s.setStartTime(dto.startTime());
        s.setEndTime(dto.endTime());
        s.setAllDay(boolToInt(dto.allDay(), 0));
        s.setLocation(dto.location());
        s.setRecurRule(blankToNull(dto.recurRule()));
        s.setAllowFeedback(boolToInt(dto.allowFeedback(), 1));
        s.setSourceType("manual");
        s.setOrganizerId(organizerId);
        s.setStatus("confirmed");
        scheduleMapper.insert(s);

        resourceService.bookOrThrow(s.getId(), dto.resourceIds(), s.getStartTime(), s.getEndTime(), null);
        List<Long> invited = saveParticipants(s.getId(), organizerId, dto.participants());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scheduleId", s.getId());
        payload.put("calendarId", calendarId);
        payload.put("organizerId", organizerId);
        payload.put("title", s.getTitle());
        payload.put("startTime", s.getStartTime().toString());
        payload.put("participantIds", invited);
        payload.put("occurredAt", LocalDateTime.now().toString());
        eventPublisher.publish(CalendarEvents.SCHEDULE_CREATED, payload);
        return s.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ScheduleUpdateDTO dto) {
        validateRange(dto.startTime(), dto.endTime());
        PmSchedule s = requireOrganizer(id);
        s.setTitle(dto.title());
        s.setDescription(dto.description());
        s.setStartTime(dto.startTime());
        s.setEndTime(dto.endTime());
        s.setAllDay(boolToInt(dto.allDay(), s.getAllDay()));
        s.setLocation(dto.location());
        s.setRecurRule(blankToNull(dto.recurRule()));
        s.setAllowFeedback(boolToInt(dto.allowFeedback(), s.getAllowFeedback()));
        scheduleMapper.updateById(s);

        resourceService.clearBookings(id);
        resourceService.bookOrThrow(id, dto.resourceIds(), s.getStartTime(), s.getEndTime(), id);
        if (dto.participants() != null) {
            replaceParticipants(id, s.getOrganizerId(), dto.participants());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scheduleId", id);
        payload.put("operatorId", currentUserId());
        payload.put("occurredAt", LocalDateTime.now().toString());
        eventPublisher.publish(CalendarEvents.SCHEDULE_UPDATED, payload);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PmSchedule s = requireOrganizer(id);
        scheduleMapper.deleteById(id);
        participantMapper.delete(Wrappers.<PmScheduleParticipant>lambdaQuery()
                .eq(PmScheduleParticipant::getScheduleId, id));
        resourceService.clearBookings(id);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scheduleId", id);
        payload.put("calendarId", s.getCalendarId());
        payload.put("operatorId", currentUserId());
        payload.put("occurredAt", LocalDateTime.now().toString());
        eventPublisher.publish(CalendarEvents.SCHEDULE_DELETED, payload);
    }

    /** 日程详情（含参与人）。仅日历归属者或参与人可见。 */
    public ScheduleVO get(Long id) {
        PmSchedule s = scheduleMapper.selectById(id);
        if (s == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "日程不存在");
        }
        return toVO(s, listParticipants(id), resourceService.resourceIdsOf(id));
    }

    /**
     * 按时间段查询当前用户可见日程：归属其日历的，或其作为参与人的。两者皆空返回空。
     * 非循环：重叠判定 start_time &le; to 且 end_time &ge; from。
     * 循环：取全部循环主记录，按规则展开到 [from,to]（套用例外），每个实例一条 VO。
     */
    public List<ScheduleVO> range(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "查询时间范围非法");
        }
        List<Long> calendarIds = calendarService.myCalendarIds();
        List<Long> participatedIds = myParticipatedScheduleIds();
        if (calendarIds.isEmpty() && participatedIds.isEmpty()) {
            return List.of();
        }
        List<Long> cals = calendarIds.isEmpty() ? List.of(NONE_SENTINEL) : calendarIds;

        // 非循环：仅取与区间重叠的
        List<PmSchedule> singles = scheduleMapper.selectList(membership(cals, participatedIds)
                .and(w -> w.isNull(PmSchedule::getRecurRule).or().eq(PmSchedule::getRecurRule, ""))
                .le(PmSchedule::getStartTime, to)
                .ge(PmSchedule::getEndTime, from)
                .eq(PmSchedule::getStatus, "confirmed"));
        // 循环主记录：全部取出再展开
        List<PmSchedule> recurring = scheduleMapper.selectList(membership(cals, participatedIds)
                .isNotNull(PmSchedule::getRecurRule)
                .ne(PmSchedule::getRecurRule, "")
                .eq(PmSchedule::getStatus, "confirmed"));

        List<ScheduleVO> result = new ArrayList<>();
        for (PmSchedule s : singles) {
            result.add(toVO(s, null, null));
        }
        for (PmSchedule s : recurring) {
            List<PmScheduleException> exceptions = exceptionMapper.selectList(
                    Wrappers.<PmScheduleException>lambdaQuery().eq(PmScheduleException::getScheduleId, s.getId()));
            for (RecurrenceExpander.Occurrence o : RecurrenceExpander.expand(s, exceptions, from, to)) {
                result.add(occurrenceVO(s, o));
            }
        }
        result.sort((a, b) -> a.startTime().compareTo(b.startTime()));
        return result;
    }

    /** 创建循环日程的单次例外（取消/改期）。仅组织者。 */
    @Transactional(rollbackFor = Exception.class)
    public void addException(Long scheduleId, ScheduleExceptionDTO dto) {
        PmSchedule s = requireOrganizer(scheduleId);
        if (!RecurrenceExpander.isRecurring(s)) {
            throw new BizException(ErrorCode.CONFLICT, "非循环日程无例外");
        }
        String action = "modify".equals(dto.action()) ? "modify" : "cancel";
        PmScheduleException ex = new PmScheduleException();
        ex.setScheduleId(scheduleId);
        ex.setOccurDate(dto.occurDate());
        ex.setAction(action);
        if ("modify".equals(action) && dto.override() != null) {
            ex.setOverride(JSONUtil.toJsonStr(dto.override()));
        }
        exceptionMapper.insert(ex);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scheduleId", scheduleId);
        payload.put("occurDate", dto.occurDate().toString());
        payload.put("action", action);
        payload.put("operatorId", currentUserId());
        payload.put("occurredAt", LocalDateTime.now().toString());
        eventPublisher.publish(CalendarEvents.SCHEDULE_UPDATED, payload);
    }

    /** 参与人对日程作出 RSVP 反馈（参加/暂定/谢绝）。 */
    @Transactional(rollbackFor = Exception.class)
    public void rsvp(Long scheduleId, RsvpDTO dto) {
        if (!RSVP_STATUSES.contains(dto.status())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "反馈状态非法");
        }
        PmSchedule s = scheduleMapper.selectById(scheduleId);
        if (s == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "日程不存在");
        }
        if (s.getAllowFeedback() == null || s.getAllowFeedback() == 0) {
            throw new BizException(ErrorCode.FORBIDDEN, "该日程不允许反馈");
        }
        PmScheduleParticipant p = participantMapper.selectOne(Wrappers.<PmScheduleParticipant>lambdaQuery()
                .eq(PmScheduleParticipant::getScheduleId, scheduleId)
                .eq(PmScheduleParticipant::getUserId, currentUserId())
                .last("limit 1"));
        if (p == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "非参与人不可反馈");
        }
        p.setRsvpStatus(dto.status());
        participantMapper.updateById(p);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scheduleId", scheduleId);
        payload.put("userId", currentUserId());
        payload.put("rsvpStatus", dto.status());
        payload.put("occurredAt", LocalDateTime.now().toString());
        eventPublisher.publish(CalendarEvents.RSVP_RESPONDED, payload);
    }

    // ===== 内部 =====

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
    }

    private PmSchedule requireOrganizer(Long id) {
        PmSchedule s = scheduleMapper.selectById(id);
        if (s == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "日程不存在");
        }
        if (!currentUserId().equals(s.getOrganizerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅组织者可操作该日程");
        }
        return s;
    }

    /** 写入参与人：组织者固定 accepted，其余按入参 pending。返回受邀内部成员 id 列表。 */
    private List<Long> saveParticipants(Long scheduleId, Long organizerId, List<ParticipantInputDTO> inputs) {
        PmScheduleParticipant organizer = new PmScheduleParticipant();
        organizer.setScheduleId(scheduleId);
        organizer.setUserId(organizerId);
        organizer.setRole("organizer");
        organizer.setRsvpStatus("accepted");
        participantMapper.insert(organizer);

        List<Long> invited = new ArrayList<>();
        if (inputs != null) {
            for (ParticipantInputDTO in : inputs) {
                if (in.userId() != null && in.userId().equals(organizerId)) {
                    continue;
                }
                PmScheduleParticipant p = new PmScheduleParticipant();
                p.setScheduleId(scheduleId);
                p.setUserId(in.userId());
                p.setExternalName(in.externalName());
                p.setRole(in.role() == null || in.role().isBlank() ? "required" : in.role());
                p.setRsvpStatus("pending");
                participantMapper.insert(p);
                if (in.userId() != null) {
                    invited.add(in.userId());
                }
            }
        }
        return invited;
    }

    /** 覆盖参与人（先逻辑删旧，再按组织者+入参重建）。 */
    private void replaceParticipants(Long scheduleId, Long organizerId, List<ParticipantInputDTO> inputs) {
        participantMapper.delete(Wrappers.<PmScheduleParticipant>lambdaQuery()
                .eq(PmScheduleParticipant::getScheduleId, scheduleId));
        saveParticipants(scheduleId, organizerId, inputs);
    }

    private List<ParticipantVO> listParticipants(Long scheduleId) {
        return participantMapper.selectList(Wrappers.<PmScheduleParticipant>lambdaQuery()
                        .eq(PmScheduleParticipant::getScheduleId, scheduleId)
                        .orderByAsc(PmScheduleParticipant::getId))
                .stream()
                .map(p -> new ParticipantVO(p.getId(), p.getUserId(), p.getExternalName(),
                        p.getRole(), p.getRsvpStatus()))
                .toList();
    }

    private List<Long> myParticipatedScheduleIds() {
        return participantMapper.selectList(Wrappers.<PmScheduleParticipant>lambdaQuery()
                        .select(PmScheduleParticipant::getScheduleId)
                        .eq(PmScheduleParticipant::getUserId, currentUserId()))
                .stream().map(PmScheduleParticipant::getScheduleId).toList();
    }

    private ScheduleVO toVO(PmSchedule s, List<ParticipantVO> participants, List<Long> resourceIds) {
        return new ScheduleVO(s.getId(), s.getCalendarId(), s.getTitle(), s.getDescription(),
                s.getStartTime(), s.getEndTime(), s.getAllDay(), s.getLocation(),
                s.getAllowFeedback(), s.getSourceType(), s.getSourceId(), s.getOrganizerId(),
                s.getStatus(), participants, resourceIds, RecurrenceExpander.isRecurring(s), null);
    }

    /** 循环展开实例 VO：标题/时间/地点取展开结果，其余沿用主记录。 */
    private ScheduleVO occurrenceVO(PmSchedule s, RecurrenceExpander.Occurrence o) {
        return new ScheduleVO(s.getId(), s.getCalendarId(), o.title(), s.getDescription(),
                o.start(), o.end(), s.getAllDay(), o.location(),
                s.getAllowFeedback(), s.getSourceType(), s.getSourceId(), s.getOrganizerId(),
                s.getStatus(), null, null, true, o.occurrenceDate());
    }

    /** 成员关系谓词：归属当前用户日历(cals) 或 当前用户参与(participatedIds)。每次新建 wrapper。 */
    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PmSchedule> membership(
            List<Long> cals, List<Long> participatedIds) {
        return Wrappers.<PmSchedule>lambdaQuery().and(w -> {
            w.in(PmSchedule::getCalendarId, cals);
            if (!participatedIds.isEmpty()) {
                w.or().in(PmSchedule::getId, participatedIds);
            }
        });
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private int boolToInt(Boolean v, Integer fallback) {
        if (v == null) {
            return fallback == null ? 0 : fallback;
        }
        return v ? 1 : 0;
    }

    private Long currentUserId() {
        return UserContext.currentUserId();
    }
}
