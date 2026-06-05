package com.ssafy.manager.auth.application;

public record OAuthUserInfo(String provider, String oauthId, String email) {
}
