package com.mido.pm.approval.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.approval.domain.FlowDefinition;
import com.mido.pm.approval.dto.FlowCreateDTO;
import com.mido.pm.approval.dto.FlowVO;
import com.mido.pm.approval.entity.ApprovalFlow;
import com.mido.pm.approval.mapper.ApprovalFlowMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审批流定义管理：创建（校验 definition JSON）、查询。
 */
@Service
public class ApprovalFlowService {

    private final ApprovalFlowMapper flowMapper;
    private final ObjectMapper objectMapper;

    public ApprovalFlowService(ApprovalFlowMapper flowMapper, ObjectMapper objectMapper) {
        this.flowMapper = flowMapper;
        this.objectMapper = objectMapper;
    }

    public Long create(FlowCreateDTO dto) {
        // 校验 definition 可解析为流程定义
        try {
            objectMapper.readValue(dto.definition(), FlowDefinition.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "流程定义 JSON 非法: " + e.getMessage());
        }
        ApprovalFlow flow = new ApprovalFlow();
        flow.setName(dto.name());
        flow.setBizType(dto.bizType());
        flow.setMode(dto.mode());
        flow.setDefinition(dto.definition());
        flowMapper.insert(flow);
        return flow.getId();
    }

    public List<FlowVO> list(String bizType) {
        return flowMapper.selectList(Wrappers.<ApprovalFlow>lambdaQuery()
                        .eq(StrUtil.isNotBlank(bizType), ApprovalFlow::getBizType, bizType)
                        .orderByDesc(ApprovalFlow::getId))
                .stream().map(this::toVO).toList();
    }

    public FlowVO get(Long id) {
        ApprovalFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "审批流不存在");
        }
        return toVO(flow);
    }

    private FlowVO toVO(ApprovalFlow f) {
        return new FlowVO(f.getId(), f.getName(), f.getBizType(), f.getMode(), f.getDefinition());
    }
}
