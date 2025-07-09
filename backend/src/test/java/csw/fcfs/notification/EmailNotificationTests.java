package csw.fcfs.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import csw.fcfs.service.RedisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import csw.fcfs.claim.ClaimService;
import csw.fcfs.post.Post;
import csw.fcfs.post.repository.PostRepository;
import csw.fcfs.user.OAuth2Provider;
import csw.fcfs.user.Role;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.repository.UserAccountRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class EmailNotificationTests {

    @Autowired
    private ClaimService claimService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RedisService redisService;

    @MockitoBean
    private JavaMailSender mailSender;

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
                .email("winner@test.com")
                .oauth2Provider(OAuth2Provider.GOOGLE)
                .role(Role.USER)
                .build();
        testUsers.add(userAccountRepository.save(user));

        // Create test post
        Post post = Post.builder()
                .title("Test Post for Email")
                .description("Test Description")
                .quota((short) 1)
                .owner(user)
                .openAt(Instant.now())
                .state(csw.fcfs.post.PostState.OPEN)
                .build();
        Post savedPost = postRepository.save(post);
        testPostIds.add(savedPost.getId());

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user.getEmail(), null, null));
    }

    @AfterEach
    public void tearDown() {
        // Clear security context
        SecurityContextHolder.clearContext();

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
    public void testEmailSentOnSuccessfulClaim() {
        // Get the test user and post
        UserAccount user = testUsers.get(0);
        Long postId = testPostIds.get(0);

        Principal principal = () -> user.getEmail();
        claimService.claimPost(postId, principal);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(1000).times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).contains(user.getEmail());
        assertThat(sentMessage.getSubject()).contains("Test Post for Email");
        assertThat(sentMessage.getText()).contains("Congratulations");
    }
}
