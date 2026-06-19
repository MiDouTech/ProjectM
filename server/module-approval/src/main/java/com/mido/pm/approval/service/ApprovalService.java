package com.mido.pm.approval.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.approval.domain.ApprovalContext;
import com.mido.pm.approval.domain.ApprovalEngine;
import com.mido.pm.approval.domain.ApproverResolver;
import com.mido.pm.approval.domain.FlowDefinition;
import com.mido.pm.approval.domain.FlowNode;
import com.mido.pm.approval.domain.NodeStatus;
import com.mido.pm.approval.dto.ActDTO;
import com.mido.pm.approval.dto.InstanceVO;
import com.mido.pm.approval.dto.PendingApprovalVO;
import com.mido.pm.approval.dto.SubmitDTO;
import com.mido.pm.approval.dto.TransferDTO;
import com.mido.pm.approval.dto.WithdrawDTO;
import com.mido.pm.approval.entity.ApprovalFlow;
import com.mido.pm.approval.entity.ApprovalInstance;
import com.mido.pm.approval.entity.ApprovalTask;
import com.mido.pm.approval.event.ApprovalEvents;
import com.mido.pm.approval.guard.NodeGuardRegistry;
import com.mido.pm.approval.mapper.ApprovalFlowMapper;
import com.mido.pm.approval.mapper.ApprovalInstanceMapper;
import com.mido.pm.approval.mapper.ApprovalTaskMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用审批引擎服务：提交 → 逐节点流转（会签/或签 + 条件路由 + 节点 guard）→ approve/reject。
 * 每个写操作同事务写 Outbox 事件（approval.submitted/node.approved/approved/rejected）。
 */
@Service
public class ApprovalService {

    /** 工作台「待我审批」卡返回上限 */
    private static final int MINE_LIMIT = 50;

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalInstanceMapper instanceMapper;
    private final ApprovalTaskMapper taskMapper;
    private final DomainEventPublisher eventPublisher;
    private final NodeGuardRegistry guardRegistry;
    private final ApproverResolver approverResolver;
    private final ObjectMapper objectMapper;

    public ApprovalService(ApprovalFlowMapper flowMapper, ApprovalInstanceMapper instanceMapper,
                           ApprovalTaskMapper taskMapper, DomainEventPublisher eventPublisher,
                           NodeGuardRegistry guardRegistry, ApproverResolver approverResolver,
                           ObjectMapper objectMapper) {
        this.flowMapper = flowMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.eventPublisher = eventPublisher;
        this.guardRegistry = guardRegistry;
        this.approverResolver = approverResolver;
        this.objectMapper = objectMapper;
    }

    /** 提交审批：解析活动节点 → 激活首节点(guard + 待办) → 发 approval.submitted。 */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(SubmitDTO dto) {
        ApprovalFlow flow = requireFlow(dto.flowId());
        FlowDefinition definition = parseDefinition(flow.getDefinition());
        ApprovalContext ctx = new ApprovalContext(dto.formData());

        List<FlowNode> active = ApprovalEngine.activeNodes(definition, ctx);
        if (active.isEmpty()) {
            throw new BizException(ErrorCode.CONFLICT, "审批流无可用节点");
        }
        FlowNode first = active.get(0);
        guardRegistry.run(first, ctx);

        ApprovalInstance instance = new ApprovalInstance();
        instance.setFlowId(flow.getId());
        instance.setBizType(dto.bizType() != null ? dto.bizType() : flow.getBizType());
        instance.setBizId(dto.bizId());
        instance.setStatus(ApprovalInstance.STATUS_PENDING);
        instance.setCurrentNode(first.key());
        instance.setFormData(writeJson(dto.formData()));
        instance.setApplicantId(currentUserId());
        instanceMapper.insert(instance);

        // 动态解析首节点审批人（USER/角色/部门主管/直属上级/发起人本人）
        List<Long> firstApprovers = approverResolver.resolve(first, instance.getApplicantId());
        createTasks(instance.getId(), first.key(), firstApprovers);

        eventPublisher.publish(ApprovalEvents.SUBMITTED, payload(
                "instanceId", instance.getId(), "flowId", flow.getId(),
                "bizType", instance.getBizType(), "bizId", instance.getBizId(),
                "applicantId", instance.getApplicantId(),
                "approverIds", firstApprovers,
                "ccIds", first.cc() == null ? List.of() : first.cc()));
        return instance.getId();
    }

    /** 审批动作：记录任务 → 判定节点(会签/或签) → 推进/结案/驳回，并发对应事件。 */
    @Transactional(rollbackFor = Exception.class)
    public void act(Long instanceId, ActDTO dto) {
        ApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "审批实例不存在");
        }
        if (!ApprovalInstance.STATUS_PENDING.equals(instance.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "审批已结束，不可再处理");
        }
        ApprovalFlow flow = requireFlow(instance.getFlowId());
        FlowDefinition definition = parseDefinition(flow.getDefinition());
        ApprovalContext ctx = new ApprovalContext(readMap(instance.getFormData()));
        List<FlowNode> active = ApprovalEngine.activeNodes(definition, ctx);

        FlowNode current = active.stream()
                .filter(n -> n.key().equals(instance.getCurrentNode())).findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.SYSTEM_ERROR, "当前节点不在活动节点中"));

        Long approverId = currentUserId();
        ApprovalTask task = requireMyPendingTask(instanceId, current.key(), approverId);

        boolean approve = ApprovalTask.ACTION_APPROVE.equals(dto.action());
        boolean reject = ApprovalTask.ACTION_REJECT.equals(dto.action());
        if (!approve && !reject) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法审批动作: " + dto.action());
        }
        task.setAction(dto.action());
        task.setComment(dto.comment());
        task.setActedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        if (reject) {
            instance.setStatus(ApprovalInstance.STATUS_REJECTED);
            instanceMapper.updateById(instance);
            // 携带 bizType/bizId，供业务侧（如立项）监听驳回事件驱动状态回退
            eventPublisher.publish(ApprovalEvents.REJECTED, payload(
                    "instanceId", instanceId, "bizType", instance.getBizType(), "bizId", instance.getBizId(),
                    "node", current.key(), "approverId", approverId, "comment", dto.comment(),
                    "applicantId", instance.getApplicantId()));
            return;
        }

        // approve：基于当前节点动态待办判定（支持转交后由受让人推进）
        List<ApprovalTask> nodeTasks = nodeTasks(instanceId, current.key());
        boolean anyApproved = nodeTasks.stream().anyMatch(t -> ApprovalTask.ACTION_APPROVE.equals(t.getAction()));
        boolean anyPending = nodeTasks.stream().anyMatch(t -> t.getAction() == null);
        NodeStatus status = ApprovalEngine.evaluateNode(current, anyApproved, anyPending, false);
        if (status != NodeStatus.PASSED) {
            return; // 会签未齐，等待其余审批人
        }

        int nextIndex = active.indexOf(current) + 1;
        FlowNode next = nextIndex < active.size() ? active.get(nextIndex) : null;
        // 动态解析下一节点审批人（供通知 + 建待办，二者一致）
        List<Long> nextApprovers = next == null ? List.of()
                : approverResolver.resolve(next, instance.getApplicantId());

        // 携带下一节点审批人，供通知监听器多通道通知"轮到你审批"（无下一节点则为空）
        eventPublisher.publish(ApprovalEvents.NODE_APPROVED, payload(
                "instanceId", instanceId, "node", current.key(), "approverId", approverId,
                "nextNode", next == null ? null : next.key(),
                "nextApproverIds", nextApprovers,
                "nextCcIds", next == null || next.cc() == null ? List.of() : next.cc()));

        if (next != null) {
            guardRegistry.run(next, ctx);
            createTasks(instanceId, next.key(), nextApprovers);
            instance.setCurrentNode(next.key());
            instanceMapper.updateById(instance);
        } else {
            instance.setStatus(ApprovalInstance.STATUS_APPROVED);
            instanceMapper.updateById(instance);
            eventPublisher.publish(ApprovalEvents.APPROVED, payload(
                    "instanceId", instanceId, "bizType", instance.getBizType(), "bizId", instance.getBizId(),
                    "applicantId", instance.getApplicantId()));
        }
    }

    /**
     * 发起人撤回：仅 pending 实例、仅申请人本人可撤回；置 withdrawn 并发 approval.withdrawn
     * （项目侧据此回退草稿）。撤回后实例非 pending，审批人 act 会被拒、不再出现在「待我审批」。
     */
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long instanceId, WithdrawDTO dto) {
        ApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "审批实例不存在");
        }
        if (!ApprovalInstance.STATUS_PENDING.equals(instance.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "审批已结束，不可撤回");
        }
        Long me = currentUserId();
        if (instance.getApplicantId() == null || !instance.getApplicantId().equals(me)) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅发起人可撤回该审批");
        }
        // 当前节点待办审批人——撤回后通知他们「无需处理」（docs/domain-events.md：通知审批人）
        List<Long> approverIds = nodeTasks(instanceId, instance.getCurrentNode()).stream()
                .filter(t -> t.getAction() == null)
                .map(ApprovalTask::getApproverId).distinct().toList();
        instance.setStatus(ApprovalInstance.STATUS_WITHDRAWN);
        instanceMapper.updateById(instance);
        eventPublisher.publish(ApprovalEvents.WITHDRAWN, payload(
                "instanceId", instanceId, "bizType", instance.getBizType(), "bizId", instance.getBizId(),
                "applicantId", instance.getApplicantId(), "approverIds", approverIds,
                "reason", dto == null ? null : dto.reason()));
    }

    /**
     * 转交：当前审批人把自己在当前节点的待办交给他人。原待办标记 transfer 留痕，为受让人新建待办，
     * 发 approval.transferred（通知受让人）。节点是否通过按动态待办判定，故受让人通过即可推进。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long instanceId, TransferDTO dto) {
        if (dto == null || dto.toUserId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "转交对象不能为空");
        }
        ApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "审批实例不存在");
        }
        if (!ApprovalInstance.STATUS_PENDING.equals(instance.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "审批已结束，不可转交");
        }
        Long me = currentUserId();
        if (dto.toUserId().equals(me)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能转交给自己");
        }
        // 当前节点须仍为活动节点（与 act 一致）：避免条件路由失活后转交产生孤儿待办与误导通知
        FlowNode current = requireCurrentActiveNode(instance);
        ApprovalTask task = requireMyPendingTask(instanceId, current.key(), me);
        // 受让人在该节点已有待办则拒绝：避免重复待办致会签卡死、受让人 act 命中多行
        boolean alreadyAssigned = taskMapper.selectCount(Wrappers.<ApprovalTask>lambdaQuery()
                .eq(ApprovalTask::getInstanceId, instanceId)
                .eq(ApprovalTask::getNode, current.key())
                .eq(ApprovalTask::getApproverId, dto.toUserId())
                .isNull(ApprovalTask::getAction)) > 0;
        if (alreadyAssigned) {
            throw new BizException(ErrorCode.CONFLICT, "该用户在当前节点已有待办，无需转交");
        }
        // 原待办标记已转交（留痕），为受让人新建待办
        task.setAction(ApprovalTask.ACTION_TRANSFER);
        task.setComment(dto.comment());
        task.setActedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        ApprovalTask handoff = new ApprovalTask();
        handoff.setInstanceId(instanceId);
        handoff.setNode(current.key());
        handoff.setApproverId(dto.toUserId());
        taskMapper.insert(handoff);

        eventPublisher.publish(ApprovalEvents.TRANSFERRED, payload(
                "instanceId", instanceId, "bizType", instance.getBizType(), "bizId", instance.getBizId(),
                "node", current.key(), "fromUserId", me, "toUserId", dto.toUserId(),
                "comment", dto.comment()));
    }

    /**
     * 待我审批（工作台卡）：当前用户名下未处理(action 为空)的待办，且其实例仍 pending；按待办新→旧。
     */
    public List<PendingApprovalVO> myPending() {
        Long me = currentUserId();
        List<ApprovalTask> tasks = taskMapper.selectList(Wrappers.<ApprovalTask>lambdaQuery()
                .eq(ApprovalTask::getApproverId, me)
                .isNull(ApprovalTask::getAction)
                .orderByDesc(ApprovalTask::getId));
        if (tasks.isEmpty()) {
            return List.of();
        }
        List<Long> instanceIds = tasks.stream().map(ApprovalTask::getInstanceId).distinct().toList();
        Map<Long, ApprovalInstance> instById = instanceMapper.selectBatchIds(instanceIds).stream()
                .collect(Collectors.toMap(ApprovalInstance::getId, i -> i));
        // 一个实例至多一条（同一用户在同一实例多节点待办去重，避免前端 key 冲突）；上限 MINE_LIMIT
        Set<Long> seenInstances = new HashSet<>();
        List<PendingApprovalVO> result = new ArrayList<>();
        for (ApprovalTask t : tasks) {
            ApprovalInstance inst = instById.get(t.getInstanceId());
            if (inst != null && ApprovalInstance.STATUS_PENDING.equals(inst.getStatus())
                    && seenInstances.add(inst.getId())) {
                result.add(new PendingApprovalVO(inst.getId(), inst.getBizType(), inst.getBizId(),
                        t.getNode(), inst.getApplicantId(), bizTitle(inst), inst.getCreateTime()));
                if (result.size() >= MINE_LIMIT) {
                    break;
                }
            }
        }
        return result;
    }

    public InstanceVO getInstance(Long id) {
        ApprovalInstance i = instanceMapper.selectById(id);
        if (i == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "审批实例不存在");
        }
        return toVO(i);
    }

    /** 按业务反查最新审批实例（含「待谁审批」），无则 null。供项目侧刷新后展示审批进度。 */
    public InstanceVO findCurrentInstance(String bizType, Long bizId) {
        ApprovalInstance i = instanceMapper.selectOne(Wrappers.<ApprovalInstance>lambdaQuery()
                .eq(ApprovalInstance::getBizType, bizType)
                .eq(ApprovalInstance::getBizId, bizId)
                .orderByDesc(ApprovalInstance::getId)
                .last("LIMIT 1"));
        return i == null ? null : toVO(i);
    }

    // ===== 内部 =====

    /** 组装实例视图：解析当前节点名/会签模式，并按待办/已通过拆分审批人。 */
    private InstanceVO toVO(ApprovalInstance i) {
        String nodeName = null;
        String mode = null;
        List<Long> pending = List.of();
        List<Long> approved = List.of();
        if (i.getCurrentNode() != null) {
            ApprovalFlow flow = flowMapper.selectById(i.getFlowId());
            if (flow != null) {
                FlowNode node = parseDefinition(flow.getDefinition()).nodes().stream()
                        .filter(n -> i.getCurrentNode().equals(n.key())).findFirst().orElse(null);
                if (node != null) {
                    nodeName = node.name();
                    mode = node.mode();
                }
            }
            List<ApprovalTask> tasks = taskMapper.selectList(Wrappers.<ApprovalTask>lambdaQuery()
                    .eq(ApprovalTask::getInstanceId, i.getId())
                    .eq(ApprovalTask::getNode, i.getCurrentNode()));
            pending = tasks.stream().filter(t -> t.getAction() == null)
                    .map(ApprovalTask::getApproverId).toList();
            approved = tasks.stream().filter(t -> ApprovalTask.ACTION_APPROVE.equals(t.getAction()))
                    .map(ApprovalTask::getApproverId).toList();
        }
        return new InstanceVO(i.getId(), i.getFlowId(), i.getBizType(), i.getBizId(),
                i.getStatus(), i.getCurrentNode(), i.getApplicantId(),
                nodeName, mode, pending, approved, readMap(i.getFormData()));
    }

    /** 业务展示标题：取 formData.projectName/title（旧实例可能缺失，返回 null）。 */
    private String bizTitle(ApprovalInstance i) {
        Map<String, Object> data = readMap(i.getFormData());
        Object name = data.getOrDefault("projectName", data.get("title"));
        return name == null ? null : String.valueOf(name);
    }

    /** 为节点建待办：approverIds 为已解析的具体审批人用户 ID（动态审批人解析后）。 */
    private void createTasks(Long instanceId, String nodeKey, List<Long> approverIds) {
        if (approverIds == null) {
            return;
        }
        for (Long approverId : approverIds) {
            ApprovalTask t = new ApprovalTask();
            t.setInstanceId(instanceId);
            t.setNode(nodeKey);
            t.setApproverId(approverId);
            taskMapper.insert(t);
        }
    }

    /** 某实例某节点的全部待办（含已处理），用于按动态任务状态判定节点是否通过。 */
    private List<ApprovalTask> nodeTasks(Long instanceId, String node) {
        return taskMapper.selectList(Wrappers.<ApprovalTask>lambdaQuery()
                .eq(ApprovalTask::getInstanceId, instanceId)
                .eq(ApprovalTask::getNode, node));
    }

    /** 当前用户在某节点的未处理待办，无则抛 FORBIDDEN。act/transfer 共用。 */
    private ApprovalTask requireMyPendingTask(Long instanceId, String node, Long userId) {
        ApprovalTask task = taskMapper.selectOne(Wrappers.<ApprovalTask>lambdaQuery()
                .eq(ApprovalTask::getInstanceId, instanceId)
                .eq(ApprovalTask::getNode, node)
                .eq(ApprovalTask::getApproverId, userId)
                .isNull(ApprovalTask::getAction));
        if (task == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "你在当前节点无待办或已处理");
        }
        return task;
    }

    /** 解析实例当前节点并校验其仍为活动节点（条件路由后可能失活），失活抛 SYSTEM_ERROR。 */
    private FlowNode requireCurrentActiveNode(ApprovalInstance instance) {
        ApprovalFlow flow = requireFlow(instance.getFlowId());
        FlowDefinition definition = parseDefinition(flow.getDefinition());
        ApprovalContext ctx = new ApprovalContext(readMap(instance.getFormData()));
        return ApprovalEngine.activeNodes(definition, ctx).stream()
                .filter(n -> n.key().equals(instance.getCurrentNode())).findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.SYSTEM_ERROR, "当前节点不在活动节点中"));
    }

    private ApprovalFlow requireFlow(Long flowId) {
        ApprovalFlow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "审批流不存在");
        }
        return flow;
    }

    private FlowDefinition parseDefinition(String json) {
        try {
            return objectMapper.readValue(json, FlowDefinition.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "流程定义解析失败: " + e.getMessage());
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "表单数据序列化失败");
        }
    }

    private Long currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }

    private Map<String, Object> payload(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        map.put("occurredAt", LocalDateTime.now().toString());
        return map;
    }
}
