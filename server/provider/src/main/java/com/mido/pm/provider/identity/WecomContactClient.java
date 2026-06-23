package com.mido.pm.provider.identity;

import cn.hutool.json.JSONArray;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 企微通讯录客户端：拉部门列表 + 成员列表（cgi-bin/department/list、cgi-bin/user/list）。
 *
 * <p>开关 {@code mido.wecom.contacts.enabled} 默认关；corpId/通讯录 secret 走环境变量、不入库。
 * access_token 内存缓存（corpId+通讯录 secret）。返回 provider 中立 DTO，由 org 侧 upsert 入库。</p>
 */
@Component
public class WecomContactClient {

    private static final Logger log = LoggerFactory.getLogger(WecomContactClient.class);
    private static final String API = "https://qyapi.weixin.qq.com/cgi-bin";
    private static final long EXPIRE_BUFFER_MS = 60_000L;

    private final boolean enabled;
    private final String corpId;
    private final String secret;

    private final HttpClient http = HttpClient.newHttpClient();
    private final Object lock = new Object();
    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    public WecomContactClient(
            @Value("${mido.wecom.contacts.enabled:false}") boolean enabled,
            @Value("${mido.wecom.corp-id:}") String corpId,
            @Value("${mido.wecom.contacts.secret:}") String secret) {
        this.enabled = enabled;
        this.corpId = corpId;
        this.secret = secret;
    }

    public boolean enabled() {
        return enabled && corpId != null && !corpId.isBlank();
    }

    /** 拉取全部部门。 */
    public List<WecomDept> listDepartments() {
        List<WecomDept> result = new ArrayList<>();
        try {
            String token = accessToken();
            if (token == null) {
                return result;
            }
            JSONObject resp = getJson(API + "/department/list?access_token=" + token);
            if (resp.getInt("errcode", -1) != 0) {
                log.warn("企微 department/list 失败：errcode={} errmsg={}", resp.getInt("errcode", -1), resp.getStr("errmsg"));
                return result;
            }
            JSONArray arr = resp.getJSONArray("department");
            if (arr != null) {
                for (Object o : arr) {
                    JSONObject d = (JSONObject) o;
                    result.add(new WecomDept(d.getLong("id", 0L), d.getStr("name"), d.getLong("parentid", 0L)));
                }
            }
        } catch (Exception e) {
            log.warn("企微部门同步拉取失败：{}", e.getMessage());
        }
        return result;
    }

    /** 拉取全部成员（根部门含子部门）。 */
    public List<WecomMember> listMembers() {
        List<WecomMember> result = new ArrayList<>();
        try {
            String token = accessToken();
            if (token == null) {
                return result;
            }
            JSONObject resp = getJson(API + "/user/list?access_token=" + token + "&department_id=1&fetch_child=1");
            if (resp.getInt("errcode", -1) != 0) {
                log.warn("企微 user/list 失败：errcode={} errmsg={}", resp.getInt("errcode", -1), resp.getStr("errmsg"));
                return result;
            }
            JSONArray arr = resp.getJSONArray("userlist");
            if (arr != null) {
                for (Object o : arr) {
                    JSONObject u = (JSONObject) o;
                    List<Long> deptIds = new ArrayList<>();
                    JSONArray depts = u.getJSONArray("department");
                    if (depts != null) {
                        for (Object d : depts) {
                            deptIds.add(((Number) d).longValue());
                        }
                    }
                    result.add(new WecomMember(u.getStr("userid"), u.getStr("name"), u.getStr("mobile"), deptIds));
                }
            }
        } catch (Exception e) {
            log.warn("企微成员同步拉取失败：{}", e.getMessage());
        }
        return result;
    }

    String accessToken() throws Exception {
        synchronized (lock) {
            if (cachedToken != null && System.currentTimeMillis() < tokenExpireAt) {
                return cachedToken;
            }
            JSONObject resp = getJson(API + "/gettoken?corpid=" + enc(corpId) + "&corpsecret=" + enc(secret));
            if (resp.getInt("errcode", -1) != 0) {
                log.warn("企微通讯录 gettoken 失败：errcode={} errmsg={}", resp.getInt("errcode", -1), resp.getStr("errmsg"));
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
