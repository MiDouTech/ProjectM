package com.mido.pm.org.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.org.dto.DeptCreateDTO;
import com.mido.pm.org.dto.DeptUpdateDTO;
import com.mido.pm.org.dto.DeptVO;
import com.mido.pm.org.entity.SysDept;
import com.mido.pm.org.mapper.SysDeptMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门服务：树形 CRUD。parent_id=0 视为根。
 */
@Service
public class SysDeptService {

    private static final long ROOT = 0L;

    private final SysDeptMapper deptMapper;

    public SysDeptService(SysDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    /** 部门负责人 id（leader_id），供跨域(如简报评审人解析)使用；部门不存在或无负责人返回 null。 */
    public Long leaderOf(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SysDept dept = deptMapper.selectById(deptId);
        return dept == null ? null : dept.getLeaderId();
    }

    /** 返回部门树（当前租户）。 */
    public List<DeptVO> tree() {
        List<SysDept> all = deptMapper.selectList(null);
        Map<Long, DeptVO> nodes = new LinkedHashMap<>();
        for (SysDept d : all) {
            DeptVO vo = new DeptVO();
            vo.setId(d.getId());
            vo.setName(d.getName());
            vo.setParentId(d.getParentId());
            nodes.put(d.getId(), vo);
        }
        List<DeptVO> roots = new ArrayList<>();
        for (DeptVO vo : nodes.values()) {
            Long pid = vo.getParentId();
            DeptVO parent = (pid == null || pid == ROOT) ? null : nodes.get(pid);
            if (parent == null) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        return roots;
    }

    public Long create(DeptCreateDTO dto) {
        Long parentId = dto.parentId() == null ? ROOT : dto.parentId();
        if (parentId != ROOT && deptMapper.selectById(parentId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "上级部门不存在");
        }
        SysDept dept = new SysDept();
        dept.setName(dto.name());
        dept.setParentId(parentId);
        deptMapper.insert(dept);
        return dept.getId();
    }

    public void update(Long id, DeptUpdateDTO dto) {
        SysDept dept = requireExists(id);
        Long parentId = dto.parentId() == null ? ROOT : dto.parentId();
        if (parentId.equals(id)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "上级部门不能是自身");
        }
        dept.setName(dto.name());
        dept.setParentId(parentId);
        deptMapper.updateById(dept);
    }

    public void delete(Long id) {
        requireExists(id);
        Long childCount = deptMapper.selectCount(
                Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException(ErrorCode.CONFLICT, "存在下级部门，不能删除");
        }
        deptMapper.deleteById(id);
    }

    private SysDept requireExists(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        return dept;
    }
}
