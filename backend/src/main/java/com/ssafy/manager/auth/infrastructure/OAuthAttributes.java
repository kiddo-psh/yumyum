package com.ssafy.manager.auth.infrastructure;

import com.ssafy.manager.auth.application.OAuthUserInfo;

import java.util.Map;

abstract class OAuthAttributes {

    protected final Map<String, Object> attributes;

    protected OAuthAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    abstract String provider();
    abstract String oauthId();
    abstract String email();

    final OAuthUserInfo toUserInfo() {
        return new OAuthUserInfo(provider(), oauthId(), email());
    }

    static OAuthAttributes of(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "kakao" -> new KakaoAttributes(attributes);
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        };
    }
}
