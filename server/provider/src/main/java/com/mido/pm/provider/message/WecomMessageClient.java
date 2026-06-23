package com.mido.pm.provider.message;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * 企微应用消息 HTTP 客户端：access_token 缓存 + message/send 投递（JDK HttpClient，无新依赖）。
 *
 * <p>{@link #push} 标 {@code @Async}：在独立线程池外呼，不阻塞 AFTER_COMMIT 通知线程
 * （遵循 CLAUDE.md §4「禁业务线程直调企微」）。投递失败仅告警、不抛出。</p>
 */
@Component
public class WecomMessageClient {

    private static final Logger log = LoggerFactory.getLogger(WecomMessageClient.class);
    private static final String BASE = "https://qyapi.weixin.qq.com/cgi-bin";
    private static final long EXPIRE_BUFFER_MS = 60_000L;

    private final HttpClient http = HttpClient.newHttpClient();
    private final Object lock = new Object();
    private volatile String cachedToken;
    private volatile String cachedKey;
    private volatile long tokenExpireAt;

    /** 异步投递文本应用消息。touser 为企微 userid。 */
    @Async
    public void push(String corpId, String secret, String agentId, String touser, String title, String content) {
        try {
            String token = accessToken(corpId, secret);
            if (token == null) {
                return;
            }
            postText(token, agentId, touser, title + "\n" + content);
        } catch (Exception e) {
            log.warn("企微消息推送失败 touser={}: {}", touser, e.getMessage());
        }
    }

    /** 取/缓存 access_token；TTL 内复用缓存，过期或缺失则重新拉取。 */
    String accessToken(String corpId, String secret) throws Exception {
        String key = corpId + "" + secret;
        synchronized (lock) {
            // 缓存键含 corpId+secret：多 corp/换密钥时不复用他方令牌。
            if (cachedToken != null && key.equals(cachedKey) && System.currentTimeMillis() < tokenExpireAt) {
                return cachedToken;
            }
            JSONObject resp = getJson(BASE + "/gettoken?corpid=" + enc(corpId) + "&corpsecret=" + enc(secret));
            if (resp.getInt("errcode", -1) != 0) {
                log.warn("企微 gettoken 失败：errcode={} errmsg={}", resp.getInt("errcode", -1), resp.getStr("errmsg"));
                return null;
            }
            cachedToken = resp.getStr("access_token");
            cachedKey = key;
            long ttlSec = resp.getLong("expires_in", 7200L);
            tokenExpireAt = System.currentTimeMillis() + ttlSec * 1000L - EXPIRE_BUFFER_MS;
            return cachedToken;
        }
    }

    /** 调 message/send 发文本消息。 */
    protected void postText(String token, String agentId, String touser, String text) throws Exception {
        JSONObject body = new JSONObject()
                .set("touser", touser)
                .set("msgtype", "text")
                .set("agentid", parseAgent(agentId))
                .set("text", new JSONObject().set("content", text));
        JSONObject resp = postJson(BASE + "/message/send?access_token=" + token, body.toString());
        if (resp.getInt("errcode", -1) != 0) {
            log.warn("企微 message/send 失败：errcode={} errmsg={}", resp.getInt("errcode", -1), resp.getStr("errmsg"));
        }
    }

    /** GET 并解析 JSON（网络 seam，单测可覆写）。 */
    protected JSONObject getJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return JSONUtil.parseObj(resp.body());
    }

    /** POST JSON 并解析（网络 seam，单测可覆写）。 */
    protected JSONObject postJson(String url, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return JSONUtil.parseObj(resp.body());
    }

    private int parseAgent(String agentId) {
        try {
            return Integer.parseInt(agentId);
        } catch (Exception e) {
            return 0;
        }
    }

    private String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }
}
