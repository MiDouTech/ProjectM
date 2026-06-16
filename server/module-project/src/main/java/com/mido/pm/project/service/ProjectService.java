package com.mido.pm.project.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.project.domain.JobLevelGuard;
import com.mido.pm.project.domain.ProjectCategory;
import com.mido.pm.project.domain.ProjectStateMachine;
import com.mido.pm.project.domain.ProjectStatus;
import com.mido.pm.project.dto.ProjectCreateDTO;
import com.mido.pm.project.dto.ProjectQueryDTO;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.dto.ProjectUpdateDTO;
import com.mido.pm.project.dto.ProjectVO;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.event.ProjectEvents;
import com.mido.pm.project.mapper.PmProjectMapper;
import com.mido.pm.provider.identity.IdentityProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目服务：CRUD + 生命周期状态机流转。任何写操作同事务写 Outbox 事件（CLAUDE.md 规则 3）。
 */
@Service
public class ProjectService {

    private static final long MAX_PAGE_SIZE = 100L;
    /** NPSS 默认延后月数（npss-rule §2：结案+6~12 月，默认 9） */
    private static final int VALUE_REVIEW_MONTHS = 9;

    private final PmProjectMapper projectMapper;
    private final DomainEventPublisher eventPublisher;
    private final IdentityProvider identityProvider;

    public ProjectService(PmProjectMapper projectMapper, DomainEventPublisher eventPublisher,
                          IdentityProvider identityProvider) {
        this.projectMapper = projectMapper;
        this.eventPublisher = eventPublisher;
        this.identityProvider = identityProvider;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectCreateDTO dto) {
        ProjectCategory category = ProjectCategory.fromCode(dto.category());
        if (category == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法项目类型: " + dto.category());
        }
        PmProject p = new PmProject();
        p.setName(dto.name());
        p.setCategory(dto.category());
        p.setSubCategory(dto.subCategory());
        p.setLeaderId(dto.leaderId());
        p.setBudget(dto.budget());
        p.setTemplateId(dto.templateId());
        p.setDescription(dto.description());
        p.setStartDate(dto.startDate());
        p.setEndDate(dto.endDate());
        p.setStatus(ProjectStatus.DRAFT.getCode());
        p.setArchived(0);
        projectMapper.insert(p);

        eventPublisher.publish(ProjectEvents.CREATED, basePayload(p.getId())
                .add("name", p.getName()).add("category", p.getCategory()).build());
        return p.getId();
    }

    public ProjectVO get(Long id) {
        return toVO(requireExists(id));
    }

    public PageResult<ProjectVO> page(ProjectQueryDTO query) {
        long pageNo = query.page() == null || query.page() < 1 ? 1 : query.page();
        long size = query.size() == null || query.size() < 1 ? 20 : Math.min(query.size(), MAX_PAGE_SIZE);
        Page<PmProject> page = new Page<>(pageNo, size);
        var wrapper = Wrappers.<PmProject>lambdaQuery()
                .eq(StrUtil.isNotBlank(query.category()), PmProject::getCategory, query.category())
                .eq(StrUtil.isNotBlank(query.status()), PmProject::getStatus, query.status())
                .eq(query.leaderId() != null, PmProject::getLeaderId, query.leaderId())
                .like(StrUtil.isNotBlank(query.keyword()), PmProject::getName, query.keyword())
                .orderByDesc(PmProject::getId);
        Page<PmProject> result = projectMapper.selectPage(page, wrapper);
        List<ProjectVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal(), pageNo, size);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProjectUpdateDTO dto) {
        PmProject p = requireExists(id);
        p.setName(dto.name());
        p.setSubCategory(dto.subCategory());
        p.setLeaderId(dto.leaderId());
        p.setBudget(dto.budget());
        p.setDescription(dto.description());
        p.setStartDate(dto.startDate());
        p.setEndDate(dto.endDate());
        projectMapper.updateById(p);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        projectMapper.deleteById(id);
    }

    /**
     * 生命周期状态流转：校验合法流转 + guard（职级/审批结果）→ 落库 → 发事件（同事务 Outbox）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transition(Long id, ProjectTransitionDTO dto) {
        PmProject p = requireExists(id);
        ProjectStatus from = ProjectStatus.fromCode(p.getStatus());
        ProjectStatus to = ProjectStatus.fromCode(dto.targetStatus());
        if (to == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法目标状态: " + dto.targetStatus());
        }
        // 1) 合法流转校验
        ProjectStateMachine.assertTransit(from, to);

        // 2) guard 钩子
        if (to == ProjectStatus.REGISTERED) {
            // 审批结果 guard（严肃约束）：必须经立项审批通过方可注册。
            // Step 3 审批流在 approval.approved 后传 approvalPassed=true 驱动本流转；
            // 未通过/未走审批（null/false）一律拒绝，杜绝绕过审批直接注册。
            if (!Boolean.TRUE.equals(dto.approvalPassed())) {
                throw new BizException(ErrorCode.FORBIDDEN, "项目须经立项审批通过方可注册");
            }
            // 职级 guard（npss-rule §8）
            JobLevelGuard.assertLeaderQualified(ProjectCategory.fromCode(p.getCategory()), leaderJobLevel(p));
        }

        // 3) 副作用
        if (to == ProjectStatus.REGISTERED) {
            p.setPmoRegisteredAt(LocalDateTime.now());
        } else if (to == ProjectStatus.CLOSED) {
            p.setValueReviewDueDate(LocalDate.now().plusMonths(VALUE_REVIEW_MONTHS));
        }
        p.setStatus(to.getCode());
        projectMapper.updateById(p);

        // 4) 事件（同事务 Outbox）
        eventPublisher.publish(ProjectEvents.STATUS_CHANGED, basePayload(id)
                .add("from", from == null ? null : from.getCode())
                .add("to", to.getCode()).build());
        if (to == ProjectStatus.REGISTERED) {
            eventPublisher.publish(ProjectEvents.REGISTERED, basePayload(id).build());
        } else if (to == ProjectStatus.CLOSED) {
            eventPublisher.publish(ProjectEvents.CLOSED, basePayload(id)
                    .add("valueReviewDueDate", String.valueOf(p.getValueReviewDueDate())).build());
        }
    }

    private String leaderJobLevel(PmProject p) {
        if (p.getLeaderId() == null) {
            return null;
        }
        return identityProvider.loadById(p.getLeaderId())
                .map(up -> up.getJobLevel()).orElse(null);
    }

    private PmProject requireExists(Long id) {
        PmProject p = projectMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        return p;
    }

    private PayloadBuilder basePayload(Long projectId) {
        Long operatorId = UserContext.get() == null ? null : UserContext.get().getUserId();
        return new PayloadBuilder()
                .add("projectId", projectId)
                .add("operatorId", operatorId)
                .add("occurredAt", LocalDateTime.now().toString());
    }

    private ProjectVO toVO(PmProject p) {
        return new ProjectVO(p.getId(), p.getCode(), p.getName(), p.getDescription(),
                p.getCategory(), p.getSubCategory(), p.getTemplateId(), p.getLeaderId(),
                p.getStatus(), p.getBudget(), p.getActualCost(), p.getStartDate(), p.getEndDate(),
                p.getValueReviewDueDate(), p.getPmoRegisteredAt(), p.getCreateTime());
    }

    /** 事件载荷构造（保序，允许 null 值）。 */
    private static final class PayloadBuilder {
        private final Map<String, Object> map = new LinkedHashMap<>();

        PayloadBuilder add(String key, Object value) {
            map.put(key, value);
            return this;
        }

        Map<String, Object> build() {
            return map;
        }
    }
}
