package com.mido.pm.common.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Outbox 事件表 Mapper。
 */
@Mapper
public interface DomainEventMapper extends BaseMapper<DomainEvent> {
}
