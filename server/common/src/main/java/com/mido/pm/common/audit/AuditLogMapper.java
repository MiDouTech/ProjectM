package com.mido.pm.common.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志（活动流）Mapper。
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
