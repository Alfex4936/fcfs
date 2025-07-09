package csw.fcfs.config.ratelimit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import csw.fcfs.service.RedisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import csw.fcfs.post.Post;
import csw.fcfs.post.repository.PostRepository;
import csw.fcfs.user.OAuth2Provider;
import csw.fcfs.user.Role;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.repository.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimitTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RedisService redisService;

    // Track test-specific data for cleanup
    private List<UserAccount> testUsers = new ArrayList<>();
    private List<Long> testPostIds = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        // Clear test data trackers
        testUsers.clear();
        testPostIds.clear();

        // Create test user
        UserAccount user = UserAccount.builder()
                .email("testuser@test.com")
                .oauth2Provider(OAuth2Provider.GOOGLE)
                .role(Role.USER)
                .build();
        testUsers.add(userAccountRepository.save(user));

        // Create 11 test posts for rate limiting test
        for (int i = 0; i < 11; i++) {
            Post post = Post.builder()
                    .title("Test Post " + i)
                    .description("Test Description")
                    .quota((short) 100)
                    .owner(user)
                    .openAt(Instant.now())
                    .state(csw.fcfs.post.PostState.OPEN)
                    .build();
            Post savedPost = postRepository.save(post);
            testPostIds.add(savedPost.getId());
        }
    }

    @AfterEach
    public void tearDown() {
        // Clean up test-specific Redis data
        for (Long postId : testPostIds) {
            try {
                redisService.deleteKeys(
                    "post:{" + postId + "}:claimants",
                    "post:{" + postId + "}:claims_count"
                );
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        // Clean up test posts (this will cascade delete claims)
        for (Long postId : testPostIds) {
            try {
                postRepository.deleteById(postId);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        // Clean up test users
        for (UserAccount user : testUsers) {
            try {
                userAccountRepository.deleteById(user.getId());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    @WithMockUser(username = "testuser@test.com")
    public void testRateLimitingOnClaimEndpoint() throws Exception {
        // First 10 requests should succeed
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/claims/" + testPostIds.get(i)).with(csrf()))
                    .andExpect(status().isOk());
        }

        // 11th request should be rate limited
        mvc.perform(post("/api/claims/" + testPostIds.get(10)).with(csrf()))
                .andExpect(status().isTooManyRequests());
    }
}
