package com.mido.pm.common.security;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 敏感配置对称加密工具（AES）：用于企微等第三方凭证 secret 的入库加密 / 取用解密。
 *
 * <p>密钥取自环境变量 {@code mido.secret.enc-key}（部署时注入，禁止入库/提交真实值）；
 * 仓库内仅保留开发占位默认值。密钥经 MD5 归一为 16 字节，适配 AES-128。</p>
 *
 * <p>约定：明文 secret 仅在「保存时加密」「同步外呼时解密」两处出现，绝不随接口返回或写日志。</p>
 */
@Component
public class SecretCipher {

    private final AES aes;

    public SecretCipher(
            @Value("${mido.secret.enc-key:mido-pm-dev-secret-please-override}") String key) {
        // MD5 归一为 16 字节密钥；生产务必通过环境变量覆盖默认值。
        this.aes = SecureUtil.aes(DigestUtil.md5(key));
    }

    /** 加密；空值原样返回（不加密空串）。 */
    public String encrypt(String plain) {
        return (plain == null || plain.isBlank()) ? plain : aes.encryptHex(plain);
    }

    /** 解密；空值原样返回。 */
    public String decrypt(String cipher) {
        return (cipher == null || cipher.isBlank()) ? cipher : aes.decryptStr(cipher);
    }
}
