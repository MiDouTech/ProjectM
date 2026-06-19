package com.mido.pm.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.doc.entity.PmDoc;
import org.apache.ibatis.annotations.Mapper;

/** 文档节点 Mapper。 */
@Mapper
public interface PmDocMapper extends BaseMapper<PmDoc> {
}
