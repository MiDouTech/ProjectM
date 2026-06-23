package com.mido.pm.provider.sso;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/** 企微 SSO 客户端单测：授权 URL 构建、开关门控、code 换 userid（覆写网络 seam）。 */
class WecomSsoClientTest {

    @Test
    void disabledByDefault() {
        assertFalse(new WecomSsoClient(false, "corp", "sec", "10").enabled());
        assertFalse(new WecomSsoClient(true, "", "sec", "10").enabled(), "缺 corpId 视为未就绪");
        assertTrue(new WecomSsoClient(true, "corp", "sec", "10").enabled());
    }

    @Test
    void buildsAuthorizeUrl() {
        String url = new WecomSsoClient(true, "wwcorp", "sec", "1000002")
                .authorizeUrl("https://app.mido.com/wecom-callback", "st123");
        assertTrue(url.startsWith("https://open.weixin.qq.com/connect/oauth2/authorize"));
        assertTrue(url.contains("appid=wwcorp"));
        assertTrue(url.contains("scope=snsapi_base"));
        assertTrue(url.contains("agentid=1000002"));
        assertTrue(url.contains("state=st123"));
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fapp.mido.com%2Fwecom-callback"));
        assertTrue(url.endsWith("#wechat_redirect"));
    }

    @Test
    void userIdByCodeReturnsUserId() throws Exception {
        WecomSsoClient client = spy(new WecomSsoClient(true, "corp", "sec", "10"));
        doReturn(new JSONObject().set("errcode", 0).set("access_token", "tk").set("expires_in", 7200))
                .doReturn(new JSONObject().set("errcode", 0).set("userid", "zhangsan"))
                .when(client).getJson(anyString());

        assertEquals("zhangsan", client.userIdByCode("CODE"));
    }

    @Test
    void userIdByCodeNullOnError() throws Exception {
        WecomSsoClient client = spy(new WecomSsoClient(true, "corp", "sec", "10"));
        doReturn(new JSONObject().set("errcode", 0).set("access_token", "tk").set("expires_in", 7200))
                .doReturn(new JSONObject().set("errcode", 40029).set("errmsg", "invalid code"))
                .when(client).getJson(anyString());

        assertNull(client.userIdByCode("BAD"));
    }
}
