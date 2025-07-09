package csw.fcfs.config.security.oauth2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import csw.fcfs.user.OAuth2Provider;
import csw.fcfs.user.Role;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserAccountRepository userAccountRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Provider provider = OAuth2Provider.valueOf(registrationId.toUpperCase());

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = getEmail(provider, attributes);

        Optional<UserAccount> userOptional = userAccountRepository.findByEmail(email);
        UserAccount user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            user = UserAccount.builder()
                    .email(email)
                    .oauth2Provider(provider)
                    .role(Role.USER)
                    .build();
            userAccountRepository.save(user);
        }

        Map<String, Object> customAttributes = new HashMap<>(attributes);
        customAttributes.put("email", email);
        customAttributes.put("userRole", user.getRole().name());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                customAttributes,
                "email"
        );
    }

    @SuppressWarnings("unchecked")
    private String getEmail(OAuth2Provider provider, Map<String, Object> attributes) {
        switch (provider) {
            case GOOGLE:
                return (String) attributes.get("email");
            case NAVER:
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("email");
            case KAKAO:
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                return (String) kakaoAccount.get("email");
            default:
                throw new IllegalArgumentException("Invalid provider: " + provider);
        }
    }
}
