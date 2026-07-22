package single.cjj.openapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class OpenApiSecretService {

    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String masterKeyBase64;

    public OpenApiSecretService(@Value("${matrix.openapi.master-key-base64:}") String masterKeyBase64) {
        this.masterKeyBase64 = masterKeyBase64;
    }

    public GeneratedCredential generateCredential() {
        byte[] keyBytes = new byte[18];
        secureRandom.nextBytes(keyBytes);
        GeneratedSecret secret = generateSecret();
        String appKey = "mk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        String appId = "app_" + UUID.randomUUID().toString().replace("-", "");
        return new GeneratedCredential(appId, appKey, secret.appSecret(), secret.appSecretCipher());
    }

    public GeneratedSecret generateSecret() {
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        String appSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        return new GeneratedSecret(appSecret, encrypt(appSecret));
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array()
            );
        } catch (Exception e) {
            throw new IllegalStateException("OpenAPI AppSecret 加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText);
            if (payload.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文格式错误");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, masterKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAPI AppSecret 解密失败", e);
        }
    }

    private SecretKeySpec masterKey() {
        if (!StringUtils.hasText(masterKeyBase64)) {
            throw new IllegalStateException("必须配置 MATRIX_OPENAPI_MASTER_KEY（Base64编码的16/24/32字节AES密钥）");
        }
        byte[] key = Base64.getDecoder().decode(masterKeyBase64);
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalStateException("MATRIX_OPENAPI_MASTER_KEY 解码后必须为16、24或32字节");
        }
        return new SecretKeySpec(key, "AES");
    }

    public record GeneratedCredential(String appId, String appKey, String appSecret, String appSecretCipher) {
    }

    public record GeneratedSecret(String appSecret, String appSecretCipher) {
    }
}
