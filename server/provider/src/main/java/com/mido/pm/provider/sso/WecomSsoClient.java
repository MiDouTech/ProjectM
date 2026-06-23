package com.mido.pm.provider.sso;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * 企微 SSO 客户端：OAuth2 授权 URL 构建 + 用 code 换企微 userid（cgi-bin/auth/getuserinfo）。
 *
 * <p>开关 {@code mido.wecom.sso.enabled} 默认关；corpId/secret/agentId 走环境变量、不入库。
 * 无凭证或关闭时视为未启用。access_token 内存缓存（corpId+ssoSecret）。</p>
 */
@Component
public class WecomSsoClient {

    private static final Logger log = LoggerFactory.getLogger(WecomSsoClient.class);
    private static final String API = "https://qyapi.weixin.qq.com/cgi-bin";
    private static final String OAUTH = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final long EXPIRE_BUFFER_MS = 60_000L;

    private final boolean enabled;
    private final String corpId;
    private final String ssoSecret;
    private final String agentId;

    private final HttpClient http = HttpClient.newHttpClient();
    private final Object lock = new Object();
    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    public WecomSsoClient(
            @Value("${mido.wecom.sso.enabled:false}") boolean enabled,
            @Value("${mido.wecom.corp-id:}") String corpId,
            @Value("${mido.wecom.sso.secret:}") String ssoSecret,
            @Value("${mido.wecom.sso.agent-id:}") String agentId) {
        this.enabled = enabled;
        this.corpId = corpId;
        this.ssoSecret = ssoSecret;
        this.agentId = agentId;
    }

    /** 企微 SSO 是否就绪（开关开 + 有 corpId）。 */
    public boolean enabled() {
        return enabled && corpId != null && !corpId.isBlank();
    }

    /** 构建 OAuth2 网页授权 URL（snsapi_base，服务端用 code 换 userid）。 */
    public String authorizeUrl(String redirectUri, String state) {
        return OAUTH + "?appid=" + enc(corpId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code&scope=snsapi_base&state=" + enc(state)
                + "&agentid=" + enc(agentId) + "#wechat_redirect";
    }

    /** 用授权 code 换企微 userid；失败/非企微成员返回 null。 */
    public String userIdByCode(String code) {
        try {
            String token = accessToken();
            if (token == null) {
                return null;
            }
            JSONObject resp = getJson(API + "/auth/getuserinfo?access_token=" + token + "&code=" + enc(code));
            if (resp.getInt("errcode", -1) != 0) {
                log.warn("企微 getuserinfo 失败：errcode={} errmsg={}", resp.getInt("errcode", -1), resp.getStr("errmsg"));
                return null;
            }
            String userId = resp.getStr("userid");
            return userId == null || userId.isBlank() ? null : userId;
        } catch (Exception e) {
            log.warn("企微 SSO code 换 userid 失败：{}", e.getMessage());
            return null;
        }
    }

    String accessToken() throws Exception {
        synchronized (lock) {
            if (cachedToken != null && System.currentTimeMillis() < tokenExpireAt) {
                return cachedToken;
            }
            JSONObject resp = getJson(API + "/gettoken?corpid=" + enc(corpId) + "&corpsecret=" + enc(ssoSecret));
            if (resp.getInt("errcode", -1) != 0) {
                log.warn("企微 SSO gettoken 失败：errcode={} errmsg={}", resp.getInt("errcode", -1), resp.getStr("errmsg"));
                return null;
            }
            cachedToken = resp.getStr("access_token");
            long ttlSec = resp.getLong("expires_in", 7200L);
            tokenExpireAt = System.currentTimeMillis() + ttlSec * 1000L - EXPIRE_BUFFER_MS;
            return cachedToken;
        }
    }

    /** GET 并解析 JSON（网络 seam，单测可覆写）。 */
    protected JSONObject getJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return JSONUtil.parseObj(resp.body());
    }

    private String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }
}
