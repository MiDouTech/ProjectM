package com.mido.pm.project.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
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
import com.mido.pm.project.entity.PmProjectMember;
import com.mido.pm.project.event.ProjectEvents;
import com.mido.pm.project.mapper.PmProjectMapper;
import com.mido.pm.project.mapper.PmProjectMemberMapper;
import com.mido.pm.provider.identity.IdentityProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 项目服务：CRUD + 生命周期状态机流转。任何写操作同事务写 Outbox 事件（CLAUDE.md 规则 3）。
 */
@Service
public class ProjectService {

    private static final long MAX_PAGE_SIZE = 100L;
    /** NPSS 默认延后月数（npss-rule §2：结案+6~12 月，默认 9） */
    private static final int VALUE_REVIEW_MONTHS = 9;
    /**
     * 允许手动流转的目标态（用户态）。注册(已注册)由审批通过事件驱动、价值验收/已评价由系统/NPSS 驱动，
     * 一律不可经公开 API 手动设置——严肃约束：杜绝伪造 approvalPassed 绕过审批直接注册。
     */
    private static final Set<String> MANUAL_TARGETS = Set.of(
            ProjectStatus.IN_PROGRESS.getCode(),
            ProjectStatus.RESULT_VERIFY.getCode(),
            ProjectStatus.CLOSED.getCode());

    /** 工作台「我参与的项目」上限 */
    private static final int MINE_LIMIT = 50;

    private final PmProjectMapper projectMapper;
    private final PmProjectMemberMapper memberMapper;
    private final DomainEventPublisher eventPublisher;
    private final IdentityProvider identityProvider;
    private final AuditLogService auditLogService;

    public ProjectService(PmProjectMapper projectMapper, PmProjectMemberMapper memberMapper,
                          DomainEventPublisher eventPublisher,
                          IdentityProvider identityProvider, AuditLogService auditLogService) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.eventPublisher = eventPublisher;
        this.identityProvider = identityProvider;
        this.auditLogService = auditLogService;
    }

    /**
     * 我参与的项目（工作台卡）：我负责(leader) ∪ 我是成员，去重、按 id 倒序，上限 {@value #MINE_LIMIT}。
     */
    public List<ProjectVO> myProjects() {
        Long me = currentUserId();
        List<Long> memberPids = memberMapper.selectList(Wrappers.<PmProjectMember>lambdaQuery()
                        .select(PmProjectMember::getProjectId)
                        .eq(PmProjectMember::getUserId, me))
                .stream().map(PmProjectMember::getProjectId).distinct().toList();
        var wrapper = Wrappers.<PmProject>lambdaQuery();
        wrapper.and(w -> {
            w.eq(PmProject::getLeaderId, me);
            if (!memberPids.isEmpty()) {
                w.or().in(PmProject::getId, memberPids);
            }
        });
        wrapper.orderByDesc(PmProject::getId).last("limit " + MINE_LIMIT);
        return projectMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    private Long currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
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
        auditLogService.record(AuditActions.TARGET_PROJECT, p.getId(), AuditActions.CREATED, null);
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
        // 编辑前后字段差异 → 活动流（一次写一条，含 changes 列表）
        List<Map<String, Object>> changes = new ArrayList<>();
        addChange(changes, "name", p.getName(), dto.name());
        addChange(changes, "subCategory", p.getSubCategory(), dto.subCategory());
        addChange(changes, "leaderId", p.getLeaderId(), dto.leaderId());
        addBudgetChange(changes, p.getBudget(), dto.budget());
        addChange(changes, "description", p.getDescription(), dto.description());
        addChange(changes, "startDate", p.getStartDate(), dto.startDate());
        addChange(changes, "endDate", p.getEndDate(), dto.endDate());

        p.setName(dto.name());
        p.setSubCategory(dto.subCategory());
        p.setLeaderId(dto.leaderId());
        p.setBudget(dto.budget());
        p.setDescription(dto.description());
        p.setStartDate(dto.startDate());
        p.setEndDate(dto.endDate());
        projectMapper.updateById(p);

        if (!changes.isEmpty()) {
            auditLogService.record(AuditActions.TARGET_PROJECT, id, AuditActions.UPDATED,
                    Map.of("changes", changes));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        projectMapper.deleteById(id);
    }

    /**
     * 手动流转（公开 API 入口）：仅允许用户态目标，注册/系统态拒绝，再委托 {@link #transition}。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transitionManual(Long id, ProjectTransitionDTO dto) {
        if (!MANUAL_TARGETS.contains(dto.targetStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN,
                    "状态「" + dto.targetStatus() + "」由审批/系统驱动，不可手动流转");
        }
        transition(id, dto);
    }

    /**
     * 生命周期状态流转（内部入口）：校验合法流转 + guard（职级/审批结果）→ 落库 → 发事件（同事务 Outbox）。
     * 由审批监听器、立项提交等可信调用方使用；公开手动流转走 {@link #transitionManual}。
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
        auditLogService.record(AuditActions.TARGET_PROJECT, id, AuditActions.STATUS_CHANGED,
                statusDetail(from == null ? null : from.getCode(), to.getCode()));
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

    /** 记一个字段变更（值不等才记；from/to 允许 null，故不用 Map.of）。 */
    private void addChange(List<Map<String, Object>> changes, String field, Object oldVal, Object newVal) {
        if (Objects.equals(oldVal, newVal)) {
            return;
        }
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("field", field);
        change.put("from", oldVal);
        change.put("to", newVal);
        changes.add(change);
    }

    /** 预算按数值比较（忽略 BigDecimal 标度差异）。 */
    private void addBudgetChange(List<Map<String, Object>> changes, BigDecimal oldVal, BigDecimal newVal) {
        boolean changed = (oldVal == null) != (newVal == null)
                || (oldVal != null && oldVal.compareTo(newVal) != 0);
        if (changed) {
            addChange(changes, "budget", oldVal, newVal);
        }
    }

    /** 状态变更明细 {from,to}（from 允许 null）。 */
    private Map<String, Object> statusDetail(String from, String to) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("from", from);
        detail.put("to", to);
        return detail;
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
