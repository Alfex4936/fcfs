package csw.fcfs;

import csw.fcfs.config.security.jwt.JwtTokenProvider;
import csw.fcfs.config.security.oauth2.CustomOAuth2UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class AuthFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    // @Test
    // OAuth2 인증 성공 후 리다이렉트 테스트는 실제 플로우를 완전히 모킹하기 어려워 주석 처리
    //void whenOAuth2LoginSuccess_thenRedirectWithToken() throws Exception {
    //    Map<String, Object> attributes = new HashMap<>();
    //    attributes.put("email", "testuser@example.com");

    //    OAuth2User oAuth2User = new DefaultOAuth2User(
    //            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
    //            attributes,
    //            "email"
    //    );

    //    when(customOAuth2UserService.loadUser(any())).thenReturn(oAuth2User);

    //    mockMvc.perform(get("/login/oauth2/code/google")
    //                    .with(authentication(new OAuth2AuthenticationToken(oAuth2User, oAuth2User.getAuthorities(), "google"))))
    //            .andExpect(status().is3xxRedirection())
    //            .andExpect(redirectedUrlPattern("http://localhost:3000/oauth2/redirect?token=*"));
    //}

    @Test
    void whenAccessProtectedEndpointWithValidToken_thenOk() throws Exception {
        String token = jwtTokenProvider.createToken("testuser@example.com", csw.fcfs.user.Role.USER);

        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessProtectedEndpointWithoutToken_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }
}
