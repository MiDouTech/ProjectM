package com.mido.pm;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 米多通用项目管理系统（mido-pm）后端启动入口。
 * 模块化单体：组件扫描 com.mido.pm 下全部模块；Mapper 仅扫描 @Mapper 标注的接口。
 */
@SpringBootApplication
@MapperScan(basePackages = "com.mido.pm", annotationClass = Mapper.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
