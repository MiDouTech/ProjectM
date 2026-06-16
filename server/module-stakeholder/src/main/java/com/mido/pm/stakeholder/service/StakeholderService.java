package com.mido.pm.stakeholder.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.stakeholder.domain.DefaultWeightTemplate;
import com.mido.pm.stakeholder.domain.RoleWeight;
import com.mido.pm.stakeholder.domain.StakeholderRole;
import com.mido.pm.stakeholder.domain.WeightValidator;
import com.mido.pm.stakeholder.dto.MatrixPointVO;
import com.mido.pm.stakeholder.dto.SaveWeightsDTO;
import com.mido.pm.stakeholder.dto.StakeholderCreateDTO;
import com.mido.pm.stakeholder.dto.StakeholderUpdateDTO;
import com.mido.pm.stakeholder.dto.StakeholderVO;
import com.mido.pm.stakeholder.dto.WeightItemDTO;
import com.mido.pm.stakeholder.entity.PmStakeholder;
import com.mido.pm.stakeholder.event.StakeholderEvents;
import com.mido.pm.stakeholder.mapper.PmStakeholderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 干系人服务（P0）：CRUD、默认权重预置(§6)、权重保存硬校验(§4)、权力利益矩阵。
 */
@Service
public class StakeholderService {

    /** 权力/利益高低分界（1-5，≥3 为高） */
    private static final int HIGH = 3;

    private final PmStakeholderMapper mapper;
    private final DomainEventPublisher eventPublisher;

    public StakeholderService(PmStakeholderMapper mapper, DomainEventPublisher eventPublisher) {
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(StakeholderCreateDTO dto) {
        if (StakeholderRole.fromCode(dto.role()) == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法干系人角色: " + dto.role());
        }
        PmStakeholder s = new PmStakeholder();
        s.setProjectId(dto.projectId());
        s.setUserId(dto.userId());
        s.setExternalName(dto.externalName());
        s.setRole(dto.role());
        s.setCategory(dto.category());
        s.setPowerLevel(dto.powerLevel());
        s.setInterestLevel(dto.interestLevel());
        s.setNpssWeight(dto.npssWeight());
        mapper.insert(s);

        eventPublisher.publish(StakeholderEvents.REGISTERED, payload(
                "stakeholderId", s.getId(), "projectId", s.getProjectId(), "role", s.getRole()));
        return s.getId();
    }

    public StakeholderVO get(Long id) {
        return toVO(requireExists(id));
    }

    public List<StakeholderVO> list(Long projectId) {
        return mapper.selectList(Wrappers.<PmStakeholder>lambdaQuery()
                        .eq(PmStakeholder::getProjectId, projectId).orderByAsc(PmStakeholder::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, StakeholderUpdateDTO dto) {
        if (StakeholderRole.fromCode(dto.role()) == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法干系人角色: " + dto.role());
        }
        PmStakeholder s = requireExists(id);
        s.setRole(dto.role());
        s.setCategory(dto.category());
        s.setExternalName(dto.externalName());
        s.setPowerLevel(dto.powerLevel());
        s.setInterestLevel(dto.interestLevel());
        s.setNpssWeight(dto.npssWeight());
        mapper.updateById(s);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExists(id);
        mapper.deleteById(id);
    }

    /** 立项默认权重预置（§6），供前端预填，Leader 可微调。 */
    public List<RoleWeight> defaultWeights(String category, String subCategory) {
        return DefaultWeightTemplate.forProject(category, subCategory);
    }

    /** 保存/微调权重：先做 §4 硬校验(受益方≥50% 且和=100%)，通过才落库。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveWeights(SaveWeightsDTO dto) {
        List<PmStakeholder> stakeholders = mapper.selectList(
                Wrappers.<PmStakeholder>lambdaQuery().eq(PmStakeholder::getProjectId, dto.projectId()));
        if (stakeholders.isEmpty()) {
            throw new BizException(ErrorCode.CONFLICT, "项目暂无干系人，无法保存权重");
        }
        Map<Long, PmStakeholder> byId = stakeholders.stream()
                .collect(Collectors.toMap(PmStakeholder::getId, Function.identity()));

        List<WeightItemDTO> items = dto.items() == null ? List.of() : dto.items();
        for (WeightItemDTO item : items) {
            PmStakeholder s = byId.get(item.stakeholderId());
            if (s == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "干系人不属于该项目: " + item.stakeholderId());
            }
            s.setNpssWeight(item.npssWeight());
        }

        // 以全部干系人(含未变更者的现值)校验
        List<RoleWeight> weights = stakeholders.stream()
                .map(s -> new RoleWeight(s.getRole(), s.getNpssWeight())).toList();
        WeightValidator.validate(weights);

        for (WeightItemDTO item : items) {
            mapper.updateById(byId.get(item.stakeholderId()));
        }
    }

    /** 权力利益矩阵数据（四象限）。 */
    public List<MatrixPointVO> matrix(Long projectId) {
        return list(projectId).stream()
                .map(s -> new MatrixPointVO(s.id(),
                        s.externalName() != null ? s.externalName() : ("user-" + s.userId()),
                        s.role(), s.powerLevel(), s.interestLevel(),
                        quadrant(s.powerLevel(), s.interestLevel())))
                .toList();
    }

    private String quadrant(Integer power, Integer interest) {
        boolean highPower = power != null && power >= HIGH;
        boolean highInterest = interest != null && interest >= HIGH;
        if (highPower && highInterest) {
            return "重点管理";
        }
        if (highPower) {
            return "令其满意";
        }
        if (highInterest) {
            return "随时告知";
        }
        return "监督";
    }

    private PmStakeholder requireExists(Long id) {
        PmStakeholder s = mapper.selectById(id);
        if (s == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "干系人不存在");
        }
        return s;
    }

    private Map<String, Object> payload(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        map.put("occurredAt", LocalDateTime.now().toString());
        return map;
    }

    private StakeholderVO toVO(PmStakeholder s) {
        return new StakeholderVO(s.getId(), s.getProjectId(), s.getUserId(), s.getExternalName(),
                s.getRole(), s.getCategory(), s.getPowerLevel(), s.getInterestLevel(), s.getNpssWeight());
    }
}
