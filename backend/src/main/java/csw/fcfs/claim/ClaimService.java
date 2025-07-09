package csw.fcfs.claim;

import java.security.Principal;
import java.util.Collections;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import csw.fcfs.notification.EmailService;
import csw.fcfs.post.Post;
import csw.fcfs.post.repository.PostRepository;
import csw.fcfs.service.RedisService;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final RedisService redisService;
    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final ClaimRepository claimRepository;
    private final EmailService emailService;

    public String claimPost(Long postId, Principal principal) {
        // Redis 캐싱으로 DB 조회 최소화
        Post post = getPostFromCache(postId);
        UserAccount user = getUserFromCache(principal.getName());

        // 게시물 소유자가 클레임을 시도하는 경우 차단
        if (post.getOwner().getId().equals(user.getId())) {
            return "OWNER_CANNOT_CLAIM";
        }

        String result = redisService.executeScript(
                loadScript("claim.lua"),
                Collections.singletonList(String.valueOf(postId)),
                String.valueOf(user.getId()),
                String.valueOf(post.getQuota()));

        if ("SUCCESS".equals(result)) {
            // 비동기 처리는 응답에 영향 없음
            saveClaimAndNotify(post, user);
        }

        return result;
    }

    @Cacheable(value = "posts", key = "#postId")
    public Post getPostFromCache(Long postId) {  // private -> public으로 변경
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }

    @Cacheable(value = "users", key = "#email")
    public UserAccount getUserFromCache(String email) {  // private -> public으로 변경
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Async("claimExecutor")  // 전용 스레드 풀 사용
    public void saveClaimAndNotify(Post post, UserAccount user) {
        Claim claim = Claim.builder()
                .post(post)
                .user(user)
                .build();
        claimRepository.save(claim);

        String subject = "Congratulations! You've successfully claimed the post: " + post.getTitle();
        String text = "Dear " + user.getEmail() + ",\n\nCongratulations! You have successfully claimed the post titled '" + post.getTitle() + "'.\n\nThank you for using our platform!\n\nBest regards,\nThe FCFS Team";
        emailService.sendEmail(user.getEmail(), subject, text);
    }

    public String declaimPost(Long postId, Principal principal) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        UserAccount user = userAccountRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String result = redisService.executeScript(
                loadScript("declaim.lua"),
                Collections.singletonList(String.valueOf(postId)),
                String.valueOf(user.getId()));

        if ("SUCCESS".equals(result)) {
            deleteClaim(post, user);
        }

        return result;
    }

    @Async
    public void deleteClaim(Post post, UserAccount user) {
        claimRepository.findByPostAndUser(post, user).ifPresent(claimRepository::delete);
    }

    public void removeClaim(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String result = redisService.executeScript(
                loadScript("declaim.lua"),
                Collections.singletonList(String.valueOf(postId)),
                String.valueOf(user.getId()));

        if ("SUCCESS".equals(result)) {
            deleteClaim(post, user);
        }
    }

    private String loadScript(String filename) {
        try (var is = getClass().getClassLoader().getResourceAsStream("scripts/" + filename)) {
            if (is == null) {
                throw new IllegalStateException("Script not found: " + filename);
            }
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Could not load script: " + filename, e);
        }
    }
}
