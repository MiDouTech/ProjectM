package com.mido.pm.calendar.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.calendar.dto.ResourceCreateDTO;
import com.mido.pm.calendar.dto.ResourceVO;
import com.mido.pm.calendar.entity.PmCalendarResource;
import com.mido.pm.calendar.entity.PmSchedule;
import com.mido.pm.calendar.entity.PmScheduleResource;
import com.mido.pm.calendar.mapper.PmCalendarResourceMapper;
import com.mido.pm.calendar.mapper.PmScheduleMapper;
import com.mido.pm.calendar.mapper.PmScheduleResourceMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日历资源服务：资源台账（会议室/设备）的列表与新建，以及日程占用资源的「冲突检测 + 预订」。
 * 冲突 = 同一资源被另一未取消日程在时间上重叠（start &lt; otherEnd 且 end &gt; otherStart）。
 */
@Service
public class ResourceService {

    private final PmCalendarResourceMapper resourceMapper;
    private final PmScheduleResourceMapper scheduleResourceMapper;
    private final PmScheduleMapper scheduleMapper;

    public ResourceService(PmCalendarResourceMapper resourceMapper,
                           PmScheduleResourceMapper scheduleResourceMapper,
                           PmScheduleMapper scheduleMapper) {
        this.resourceMapper = resourceMapper;
        this.scheduleResourceMapper = scheduleResourceMapper;
        this.scheduleMapper = scheduleMapper;
    }

    public List<ResourceVO> list() {
        return resourceMapper.selectList(Wrappers.<PmCalendarResource>lambdaQuery()
                        .eq(PmCalendarResource::getStatus, "active")
                        .orderByAsc(PmCalendarResource::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ResourceCreateDTO dto) {
        PmCalendarResource r = new PmCalendarResource();
        r.setName(dto.name());
        r.setType(dto.type() == null || dto.type().isBlank() ? "room" : dto.type());
        r.setCapacity(dto.capacity());
        r.setLocation(dto.location());
        r.setStatus("active");
        resourceMapper.insert(r);
        return r.getId();
    }

    /**
     * 检测资源在 [start,end] 的占用冲突，无冲突则为日程预订（写 pm_schedule_resource）。
     *
     * @param excludeScheduleId 排除自身（更新场景），新建传 null
     */
    @Transactional(rollbackFor = Exception.class)
    public void bookOrThrow(Long scheduleId, List<Long> resourceIds, LocalDateTime start,
                            LocalDateTime end, Long excludeScheduleId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return;
        }
        for (Long resourceId : resourceIds.stream().distinct().toList()) {
            if (hasConflict(resourceId, start, end, excludeScheduleId)) {
                PmCalendarResource r = resourceMapper.selectById(resourceId);
                String name = r == null ? ("#" + resourceId) : r.getName();
                throw new BizException(ErrorCode.CONFLICT, "资源「" + name + "」在该时间段已被占用");
            }
            PmScheduleResource link = new PmScheduleResource();
            link.setScheduleId(scheduleId);
            link.setResourceId(resourceId);
            scheduleResourceMapper.insert(link);
        }
    }

    /** 清除某日程的资源占用（更新前先解绑）。 */
    @Transactional(rollbackFor = Exception.class)
    public void clearBookings(Long scheduleId) {
        scheduleResourceMapper.delete(Wrappers.<PmScheduleResource>lambdaQuery()
                .eq(PmScheduleResource::getScheduleId, scheduleId));
    }

    public List<Long> resourceIdsOf(Long scheduleId) {
        return scheduleResourceMapper.selectList(Wrappers.<PmScheduleResource>lambdaQuery()
                        .eq(PmScheduleResource::getScheduleId, scheduleId))
                .stream().map(PmScheduleResource::getResourceId).toList();
    }

    private boolean hasConflict(Long resourceId, LocalDateTime start, LocalDateTime end, Long excludeScheduleId) {
        List<Long> occupantIds = scheduleResourceMapper.selectList(Wrappers.<PmScheduleResource>lambdaQuery()
                        .eq(PmScheduleResource::getResourceId, resourceId))
                .stream().map(PmScheduleResource::getScheduleId)
                .filter(id -> excludeScheduleId == null || !excludeScheduleId.equals(id))
                .distinct().toList();
        if (occupantIds.isEmpty()) {
            return false;
        }
        Long overlapping = scheduleMapper.selectCount(Wrappers.<PmSchedule>lambdaQuery()
                .in(PmSchedule::getId, occupantIds)
                .eq(PmSchedule::getStatus, "confirmed")
                .lt(PmSchedule::getStartTime, end)
                .gt(PmSchedule::getEndTime, start));
        return overlapping != null && overlapping > 0;
    }

    private ResourceVO toVO(PmCalendarResource r) {
        return new ResourceVO(r.getId(), r.getName(), r.getType(), r.getCapacity(),
                r.getLocation(), r.getStatus());
    }
}
