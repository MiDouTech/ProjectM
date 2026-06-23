package com.mido.pm.provider.message;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 企微客户端单测（覆写网络 seam）：access_token TTL 内复用缓存、gettoken 失败返回 null、push 发文本。
 */
class WecomMessageClientTest {

    private JSONObject tokenOk() {
        return new JSONObject().set("errcode", 0).set("access_token", "tk").set("expires_in", 7200);
    }

    @Test
    void accessTokenCachedWithinTtl() throws Exception {
        WecomMessageClient client = spy(new WecomMessageClient());
        doReturn(tokenOk()).when(client).getJson(anyString());

        assertEquals("tk", client.accessToken("corp", "sec"));
        assertEquals("tk", client.accessToken("corp", "sec"));

        verify(client, times(1)).getJson(anyString()); // 缓存命中，仅拉取一次
    }

    @Test
    void differentCorpRefetchesToken() throws Exception {
        WecomMessageClient client = spy(new WecomMessageClient());
        doReturn(tokenOk()).when(client).getJson(anyString());

        client.accessToken("corpA", "secA");
        client.accessToken("corpB", "secB"); // 不同 corp/secret → 不复用缓存

        verify(client, times(2)).getJson(anyString());
    }

    @Test
    void gettokenErrorReturnsNull() throws Exception {
        WecomMessageClient client = spy(new WecomMessageClient());
        doReturn(new JSONObject().set("errcode", 40013).set("errmsg", "invalid corpid"))
                .when(client).getJson(anyString());

        assertNull(client.accessToken("corp", "sec"));
    }

    @Test
    void pushSendsTextWhenTokenOk() throws Exception {
        WecomMessageClient client = spy(new WecomMessageClient());
        doReturn(tokenOk()).when(client).getJson(anyString());
        doNothing().when(client).postText(any(), any(), any(), any());

        client.push("corp", "sec", "1000002", "wxuser1", "标题", "正文");

        verify(client).postText("tk", "1000002", "wxuser1", "标题\n正文");
    }
}
