package com.mido.pm;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * 米多通用项目管理系统（mido-pm）后端启动入口。
 * 模块化单体：组件扫描 com.mido.pm 下全部模块；Mapper 仅扫描 @Mapper 标注的接口。
 * @EnableScheduling：开启定时任务（如 NPSS 价值验收到点扫描）。
 * @EnableAsync：开启异步执行（如企微消息外呼，不阻塞通知线程），复用 Boot 默认线程池。
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan(basePackages = "com.mido.pm", annotationClass = Mapper.class)
public class Application {

    public static void main(String[] args) {
        // 统一服务器时区：保证 due_date 逾期判断、NPSS 到点日、审计时间等日期逻辑跨环境一致。
        // 默认 Asia/Shanghai，可经 -Dmido.timezone 或环境变量 MIDO_TIMEZONE 覆盖。
        String tz = System.getProperty("mido.timezone");
        if (tz == null || tz.isBlank()) {
            tz = System.getenv().getOrDefault("MIDO_TIMEZONE", "Asia/Shanghai");
        }
        TimeZone.setDefault(TimeZone.getTimeZone(tz));

        SpringApplication.run(Application.class, args);
    }
}
