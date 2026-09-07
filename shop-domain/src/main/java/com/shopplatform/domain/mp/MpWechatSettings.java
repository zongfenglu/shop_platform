package com.shopplatform.domain.mp;

public record MpWechatSettings(
        String componentAppId,
        String componentSecret,
        String token,
        String encodingAesKey,
        String callbackUrl
) {
    public boolean configured() {
        return notBlank(componentAppId) && notBlank(componentSecret)
                && notBlank(token) && encodingAesKey != null && encodingAesKey.length() == 43
                && notBlank(callbackUrl);
    }

    public String authCallbackUrl() {
        String base = callbackUrl == null ? "" : callbackUrl.trim();
        if (base.endsWith("/notify/wechat/component")) {
            return base.substring(0, base.length() - "/notify/wechat/component".length())
                    + "/notify/wechat/auth/callback";
        }
        if (base.endsWith("/")) {
            return base + "notify/wechat/auth/callback";
        }
        return base + "/notify/wechat/auth/callback";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
