package com.ssafy.manager.auth.infrastructure;

import com.ssafy.manager.auth.application.MemberOAuthService;
import com.ssafy.manager.auth.application.OAuthUserInfo;
import com.ssafy.manager.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberOAuthService memberOAuthService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        OAuthUserInfo userInfo = OAuthAttributes.of(provider, oAuth2User.getAttributes()).toUserInfo();
        Member member = memberOAuthService.getOrRegister(userInfo);

        Map<String, Object> attrs = new HashMap<>(oAuth2User.getAttributes());
        attrs.put("memberId", member.getId());
        attrs.put("needsOnboarding", !member.isOnboardingCompleted());

        return new DefaultOAuth2User(List.of(), attrs, "id");
    }
}
