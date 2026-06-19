package com.mido.pm.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.project.entity.PmProjectType;
import com.mido.pm.project.mapper.PmProjectTypeMapper;
import org.springframework.stereotype.Service;

/**
 * 项目类型解析：把项目的遗留 (category, subCategory) 映射到「项目类型」记录。
 *
 * <p>这是去硬编码后<b>唯一</b>保留的兼容映射点——把旧枚举字符串桥接到租户可配的类型表，
 * 所有规则（职级门槛 / 是否走 NPSS / 默认审批流）一律从解析出的类型属性读取，不再散落 if-else。
 * 后续项目创建改为直接按 type code/id 选型后，本桥接可进一步简化。</p>
 */
@Service
public class ProjectTypeResolver {

    private final PmProjectTypeMapper typeMapper;

    public ProjectTypeResolver(PmProjectTypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }

    /** 遗留 (category, subCategory) → 类型 code（O 三子类各自独立，S/I 即类型码）。 */
    public static String toCode(String category, String subCategory) {
        if ("O".equals(category)) {
            if ("定向整改".equals(subCategory)) {
                return "O_RECTIFY";
            }
            if ("专项督办".equals(subCategory)) {
                return "O_SUPERVISE";
            }
            return "O_NORMAL";
        }
        return category;
    }

    /** 按 (category, subCategory) 解析类型；找不到返回 null。 */
    public PmProjectType find(String category, String subCategory) {
        String code = toCode(category, subCategory);
        if (code == null) {
            return null;
        }
        return typeMapper.selectOne(Wrappers.<PmProjectType>lambdaQuery()
                .eq(PmProjectType::getCode, code).last("limit 1"));
    }

    /** 按 (category, subCategory) 解析类型；找不到抛 PARAM_ERROR。 */
    public PmProjectType require(String category, String subCategory) {
        PmProjectType type = find(category, subCategory);
        if (type == null) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "未找到项目类型: " + toCode(category, subCategory));
        }
        return type;
    }
}
