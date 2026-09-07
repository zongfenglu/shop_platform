package com.shopplatform.domain.mp.wechat;

import com.shopplatform.common.exception.BusinessException;
import com.shopplatform.common.result.ErrorCode;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 微信开放平台消息加解密。算法见官方「消息加解密技术方案」：
 * AES-256-CBC、PKCS7 按 32 字节补位、明文 = random(16) + network-order len + xml + appId。
 */
public final class WxMsgCrypt {

    private static final int BLOCK = 32;

    private final String token;
    private final byte[] aesKey;
    private final String appId;

    public WxMsgCrypt(String token, String encodingAesKey, String appId) {
        if (token == null || token.isBlank() || encodingAesKey == null || encodingAesKey.length() != 43) {
            throw new BusinessException(ErrorCode.WECHAT_COMPONENT_NOT_CONFIGURED, "消息加解密 Token / EncodingAESKey 未配置");
        }
        this.token = token;
        this.appId = appId == null ? "" : appId;
        this.aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
        if (this.aesKey.length != 32) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "EncodingAESKey 无效");
        }
    }

    public String verifyUrl(String signature, String timestamp, String nonce, String echostr) {
        if (!plainSignature(timestamp, nonce).equals(signature)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "微信回调签名不正确");
        }
        return echostr == null ? "" : echostr;
    }

    public String decrypt(String msgSignature, String timestamp, String nonce, String encrypt) {
        if (!msgSignature(timestamp, nonce, encrypt).equals(msgSignature)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "微信回调签名不正确");
        }
        try {
            byte[] original = aesDecrypt(Base64.getDecoder().decode(encrypt));
            byte[] bytes = pkcs7Decode(original);
            int len = ByteBuffer.wrap(bytes, 16, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            String xml = new String(bytes, 20, len, StandardCharsets.UTF_8);
            String fromAppId = new String(bytes, 20 + len, bytes.length - 20 - len, StandardCharsets.UTF_8);
            if (!appId.isEmpty() && !appId.equals(fromAppId)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "回调 AppId 不匹配");
            }
            return xml;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "微信回调解密失败");
        }
    }

    public String encrypt(String xml) {
        try {
            byte[] random = new byte[16];
            new SecureRandom().nextBytes(random);
            byte[] body = xml.getBytes(StandardCharsets.UTF_8);
            byte[] app = appId.getBytes(StandardCharsets.UTF_8);
            byte[] networkLen = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(body.length).array();
            byte[] raw = new byte[16 + 4 + body.length + app.length];
            System.arraycopy(random, 0, raw, 0, 16);
            System.arraycopy(networkLen, 0, raw, 16, 4);
            System.arraycopy(body, 0, raw, 20, body.length);
            System.arraycopy(app, 0, raw, 20 + body.length, app.length);
            return Base64.getEncoder().encodeToString(aesEncrypt(pkcs7Encode(raw)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信消息加密失败");
        }
    }

    public String msgSignature(String timestamp, String nonce, String encrypt) {
        return sha1(sorted(token, timestamp, nonce, encrypt));
    }

    public String plainSignature(String timestamp, String nonce) {
        return sha1(sorted(token, timestamp, nonce));
    }

    private byte[] aesEncrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16)));
        return cipher.doFinal(data);
    }

    private byte[] aesDecrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16)));
        return cipher.doFinal(data);
    }

    private static byte[] pkcs7Encode(byte[] src) {
        int pad = BLOCK - (src.length % BLOCK);
        byte[] dest = Arrays.copyOf(src, src.length + pad);
        Arrays.fill(dest, src.length, dest.length, (byte) pad);
        return dest;
    }

    private static byte[] pkcs7Decode(byte[] decrypted) {
        int pad = decrypted[decrypted.length - 1] & 0xFF;
        if (pad < 1 || pad > BLOCK) {
            pad = 0;
        }
        return Arrays.copyOf(decrypted, decrypted.length - pad);
    }

    private static String sorted(String... parts) {
        Arrays.sort(parts);
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(p == null ? "" : p);
        }
        return sb.toString();
    }

    private static String sha1(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "签名计算失败");
        }
    }
}
