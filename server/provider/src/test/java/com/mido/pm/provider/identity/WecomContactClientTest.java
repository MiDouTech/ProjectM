package com.mido.pm.provider.identity;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/** 企微通讯录客户端单测：开关门控、部门/成员解析（覆写网络 seam）。 */
class WecomContactClientTest {

    private WecomContactClient enabledClient() {
        return spy(new WecomContactClient(true, "corp", "sec"));
    }

    @Test
    void disabledByDefault() {
        assertFalse(new WecomContactClient(false, "corp", "sec").enabled());
        assertFalse(new WecomContactClient(true, "", "sec").enabled());
        assertTrue(new WecomContactClient(true, "corp", "sec").enabled());
    }

    @Test
    void parsesDepartments() throws Exception {
        WecomContactClient client = enabledClient();
        doReturn(JSONUtil.parseObj("{\"errcode\":0,\"access_token\":\"tk\",\"expires_in\":7200}"))
                .doReturn(JSONUtil.parseObj(
                        "{\"errcode\":0,\"department\":[{\"id\":2,\"name\":\"研发部\",\"parentid\":1},"
                        + "{\"id\":3,\"name\":\"测试组\",\"parentid\":2}]}"))
                .when(client).getJson(anyString());

        List<WecomDept> depts = client.listDepartments();

        assertEquals(2, depts.size());
        assertEquals("研发部", depts.get(0).name());
        assertEquals(1L, depts.get(0).parentId());
        assertEquals(2L, depts.get(1).parentId());
    }

    @Test
    void parsesMembers() throws Exception {
        WecomContactClient client = enabledClient();
        doReturn(JSONUtil.parseObj("{\"errcode\":0,\"access_token\":\"tk\",\"expires_in\":7200}"))
                .doReturn(JSONUtil.parseObj(
                        "{\"errcode\":0,\"userlist\":[{\"userid\":\"zhangsan\",\"name\":\"张三\","
                        + "\"mobile\":\"13800000000\",\"department\":[2,3]}]}"))
                .when(client).getJson(anyString());

        List<WecomMember> members = client.listMembers();

        assertEquals(1, members.size());
        assertEquals("zhangsan", members.get(0).userId());
        assertEquals("张三", members.get(0).name());
        assertEquals(List.of(2L, 3L), members.get(0).departmentIds());
    }
}
