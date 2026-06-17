package com.mido.pm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S 类项目走完整立项审批的端到端测试（需 MySQL，本地 docker compose 起库后以 -Dmido.e2e=true 运行）。
 * 流程：登录 → 建 S 项目(Leader L3) → 提交立项审批(草稿→审批中) →
 *      S_STANDARD 四级审批(部门负责人→PMO→分管副总→总经理)逐级通过 →
 *      approval.approved 监听驱动 审批中→已注册 + 写 pmo_registered_at。
 * 反例：未注册项目不得流转到进行中（409）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "mido.e2e", matches = "true")
class ProjectInitiationE2ETest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void sClassFullApprovalRegistersProject() throws Exception {
        String admin = login("admin", "admin123");

        long projectId = createSProject(admin);
        long instanceId = submitApproval(admin, projectId);
        assertEquals("审批中", project(admin, projectId).path("status").asText());

        // S_STANDARD：部门负责人 → PMO → 分管副总 → 总经理
        approve(login("deptlead", "admin123"), instanceId);
        approve(login("pmo", "admin123"), instanceId);
        approve(login("vp", "admin123"), instanceId);
        approve(login("gm", "admin123"), instanceId);   // 全流程通过 → 驱动注册

        JsonNode registered = project(admin, projectId);
        assertEquals("已注册", registered.path("status").asText());
        assertFalse(registered.path("pmoRegisteredAt").isNull(), "应写 pmo_registered_at");
    }

    @Test
    void unregisteredProjectCannotStart() throws Exception {
        String admin = login("admin", "admin123");
        long projectId = createSProject(admin);   // 草稿
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/transition")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"进行中\"}"))
                .andExpect(status().isConflict());   // 非法状态流转
    }

    // ===== helpers =====

    private String login(String username, String password) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(resp).path("data").path("token").asText();
    }

    private long createSProject(String token) throws Exception {
        String resp = authedPost(token, "/api/v1/projects",
                "{\"name\":\"战略项目A\",\"category\":\"S\",\"leaderId\":1,\"budget\":500000}");
        return om.readTree(resp).path("data").asLong();
    }

    private long submitApproval(String token, long projectId) throws Exception {
        String resp = authedPost(token, "/api/v1/projects/" + projectId + "/submit-approval",
                "{\"objective\":\"达成战略目标\",\"valueHypothesis\":\"价值大于投入\",\"stakeholderDraft\":\"发起人/业务方\"}");
        return om.readTree(resp).path("data").asLong();
    }

    private void approve(String token, long instanceId) throws Exception {
        authedPost(token, "/api/v1/approvals/instances/" + instanceId + "/actions",
                "{\"action\":\"approve\",\"comment\":\"同意\"}");
    }

    private JsonNode project(String token, long projectId) throws Exception {
        String resp = mockMvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(resp).path("data");
    }

    private String authedPost(String token, String url, String body) throws Exception {
        return mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
