package com.ssafy.manager.auth.infrastructure;

import java.util.Map;

class KakaoAttributes extends OAuthAttributes {

    KakaoAttributes(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    String provider() {
        return "kakao";
    }

    @Override
    String oauthId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    String email() {
        Map<String, Object> account = kakaoAccount();
        return account == null ? null : (String) account.get("email");
    }

    String nickname() {
        Map<String, Object> account = kakaoAccount();
        if (account == null) return null;
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        return profile == null ? null : (String) profile.get("nickname");
    }

    private Map<String, Object> kakaoAccount() {
        return (Map<String, Object>) attributes.get("kakao_account");
    }
}
