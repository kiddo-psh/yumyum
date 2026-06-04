package com.ssafy.manager.auth.infrastructure;

import com.ssafy.manager.auth.application.MemberOAuthService;
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
        return processKakaoUser(oAuth2User.getAttributes());
    }

    private OAuth2User processKakaoUser(Map<String, Object> attributes) {
        String oauthId = String.valueOf(attributes.get("id"));

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : null;

        Member member = memberOAuthService.getOrRegister("kakao", oauthId, email, nickname);

        Map<String, Object> attrs = new HashMap<>(attributes);
        attrs.put("memberId", member.getId());

        return new DefaultOAuth2User(List.of(), attrs, "id");
    }
}
