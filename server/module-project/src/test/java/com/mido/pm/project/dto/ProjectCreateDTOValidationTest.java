package com.mido.pm.project.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 创建项目入参校验单测：负责人为新建必填项。
 * 覆盖空白创建（ProjectCreateDTO）与按模板创建（CreateFromTemplateDTO）两条入口。
 */
class ProjectCreateDTOValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void leaderIdRequiredOnBlankCreate() {
        ProjectCreateDTO missing = new ProjectCreateDTO("项目A", "O", null, null, null, null, null, null, null, null);
        assertTrue(hasLeaderIdViolation(validator.validate(missing)), "缺负责人应校验失败");

        ProjectCreateDTO ok = new ProjectCreateDTO("项目A", "O", null, 1L, null, null, null, null, null, null);
        assertFalse(hasLeaderIdViolation(validator.validate(ok)), "有负责人应通过");
    }

    @Test
    void leaderIdRequiredOnTemplateCreate() {
        CreateFromTemplateDTO missing = new CreateFromTemplateDTO(1L, "项目A", null, null, null, null, null);
        assertTrue(hasLeaderIdViolation(validator.validate(missing)), "缺负责人应校验失败");

        CreateFromTemplateDTO ok = new CreateFromTemplateDTO(1L, "项目A", 1L, null, null, null, null);
        assertFalse(hasLeaderIdViolation(validator.validate(ok)), "有负责人应通过");
    }

    private static boolean hasLeaderIdViolation(java.util.Set<? extends jakarta.validation.ConstraintViolation<?>> violations) {
        return violations.stream().anyMatch(v -> "leaderId".equals(v.getPropertyPath().toString()));
    }
}
