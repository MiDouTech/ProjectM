package com.mido.pm.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 Jackson 配置。
 *
 * <p>雪花 ID 为 19 位长整型，超出 JS {@code Number.MAX_SAFE_INTEGER}（16 位），
 * 若按 JSON 数字下发会被前端四舍五入失真，导致拿失真 ID 查询时报「不存在」。
 * 这里将<strong>包装类型 {@code Long}</strong>（所有主键/外键）统一序列化为字符串，前端全程当字符串透传。
 *
 * <p>注意：只命中包装 {@code Long}，<strong>不影响</strong>基本类型 {@code long}
 * （如 {@code PageResult} 的 total/page/size），分页组件仍得到数字；
 * 用 {@code serializerByType} 而非替换 modules，避免破坏 JavaTime 等默认模块。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance);
    }
}
