package com.mido.pm.approval.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.approval.domain.ApprovalContext;
import com.mido.pm.approval.domain.ApprovalEngine;
import com.mido.pm.approval.domain.FlowDefinition;
import com.mido.pm.approval.domain.FlowNode;
import com.mido.pm.approval.domain.NodeStatus;
import com.mido.pm.approval.dto.ActDTO;
import com.mido.pm.approval.dto.InstanceVO;
import com.mido.pm.approval.dto.PendingApprovalVO;
import com.mido.pm.approval.dto.SubmitDTO;
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
    private final ObjectMapper objectMapper;

    public ApprovalService(ApprovalFlowMapper flowMapper, ApprovalInstanceMapper instanceMapper,
                           ApprovalTaskMapper taskMapper, DomainEventPublisher eventPublisher,
                           NodeGuardRegistry guardRegistry, ObjectMapper objectMapper) {
        this.flowMapper = flowMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.eventPublisher = eventPublisher;
        this.guardRegistry = guardRegistry;
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

        createTasks(instance.getId(), first);

        eventPublisher.publish(ApprovalEvents.SUBMITTED, payload(
                "instanceId", instance.getId(), "flowId", flow.getId(),
                "bizType", instance.getBizType(), "bizId", instance.getBizId(),
                "applicantId", instance.getApplicantId(),
                "approverIds", first.approvers() == null ? List.of() : first.approvers()));
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
        ApprovalTask task = taskMapper.selectOne(Wrappers.<ApprovalTask>lambdaQuery()
                .eq(ApprovalTask::getInstanceId, instanceId)
                .eq(ApprovalTask::getNode, current.key())
                .eq(ApprovalTask::getApproverId, approverId)
                .isNull(ApprovalTask::getAction));
        if (task == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "你在当前节点无待办或已处理");
        }

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
            eventPublisher.publish(ApprovalEvents.REJECTED, payload(
                    "instanceId", instanceId, "node", current.key(),
                    "approverId", approverId, "comment", dto.comment(),
                    "applicantId", instance.getApplicantId()));
            return;
        }

        // approve：判定当前节点
        Set<Long> approved = approvedApprovers(instanceId, current.key());
        NodeStatus status = ApprovalEngine.evaluateNode(current, approved, false);
        if (status != NodeStatus.PASSED) {
            return; // 会签未齐，等待其余审批人
        }

        int nextIndex = active.indexOf(current) + 1;
        FlowNode next = nextIndex < active.size() ? active.get(nextIndex) : null;

        // 携带下一节点审批人，供通知监听器多通道通知"轮到你审批"（无下一节点则为空）
        eventPublisher.publish(ApprovalEvents.NODE_APPROVED, payload(
                "instanceId", instanceId, "node", current.key(), "approverId", approverId,
                "nextNode", next == null ? null : next.key(),
                "nextApproverIds", next == null || next.approvers() == null ? List.of() : next.approvers()));

        if (next != null) {
            guardRegistry.run(next, ctx);
            createTasks(instanceId, next);
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
        instance.setStatus(ApprovalInstance.STATUS_WITHDRAWN);
        instanceMapper.updateById(instance);
        eventPublisher.publish(ApprovalEvents.WITHDRAWN, payload(
                "instanceId", instanceId, "bizType", instance.getBizType(), "bizId", instance.getBizId(),
                "applicantId", instance.getApplicantId(), "reason", dto == null ? null : dto.reason()));
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
                        t.getNode(), inst.getApplicantId(), inst.getCreateTime()));
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
                nodeName, mode, pending, approved);
    }

    private void createTasks(Long instanceId, FlowNode node) {
        List<Long> approvers = node.approvers() == null ? List.of() : node.approvers();
        for (Long approverId : approvers) {
            ApprovalTask t = new ApprovalTask();
            t.setInstanceId(instanceId);
            t.setNode(node.key());
            t.setApproverId(approverId);
            taskMapper.insert(t);
        }
    }

    private Set<Long> approvedApprovers(Long instanceId, String node) {
        return taskMapper.selectList(Wrappers.<ApprovalTask>lambdaQuery()
                        .eq(ApprovalTask::getInstanceId, instanceId)
                        .eq(ApprovalTask::getNode, node)
                        .eq(ApprovalTask::getAction, ApprovalTask.ACTION_APPROVE))
                .stream().map(ApprovalTask::getApproverId).collect(Collectors.toSet());
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
