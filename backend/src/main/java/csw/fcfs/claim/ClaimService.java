package csw.fcfs.claim;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import csw.fcfs.post.Post;
import csw.fcfs.post.PostVisibility;
import csw.fcfs.post.repository.PostRepository;
import csw.fcfs.service.RedisService;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final RedisService redisService;
    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final ClaimCacheService claimCacheService;
    private final ClaimAsyncService claimAsyncService;

//    @Transactional(
//        propagation = Propagation.REQUIRES_NEW, // never inherit a read-only TX
//        readOnly = false                        // explicitly writable
//    )
    @Deprecated
    public String claimPost(Long postId, Principal principal) {
        // Redis 캐싱으로 DB 조회 최소화
        Post post = claimCacheService.getPostFromCache(postId);
        UserAccount user = claimCacheService.getUserFromCache(principal.getName());

        // 게시물 소유자가 클레임을 시도하는 경우 차단
        if (post.getOwner().getId().equals(user.getId())) {
            return "OWNER_CANNOT_CLAIM";
        }

        // 🔒 Privacy check: 비공개 게시물인 경우 소유자가 아니면 클레임 불가
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            return "POST_NOT_ACCESSIBLE";
        }

        String result = redisService.executeScript(
                redisService.loadScript("claim.lua"),
                Collections.singletonList(String.valueOf(postId)),
                String.valueOf(user.getId()),
                String.valueOf(post.getQuota()));

        if ("SUCCESS".equals(result)) {
            // 비동기 처리는 응답에 영향 없음
            claimAsyncService.saveClaimAndNotify(post, user);
        }

        return result;
    }

    // Optimized version that bypasses cache for better performance
    public String claimPost(Post post, UserAccount user) {
        // Input validation at application level (better than Lua script validation)
        if (post == null) {
            throw new IllegalArgumentException("Post cannot be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (post.getId() == null) {
            throw new IllegalArgumentException("Post ID cannot be null");
        }
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (post.getQuota() <= 0) {
            return "INVALID_QUOTA";
        }
        
        // 게시물 소유자가 클레임을 시도하는 경우 차단
        if (post.getOwner().getId().equals(user.getId())) {
            return "OWNER_CANNOT_CLAIM";
        }

        // 🔒 Privacy check: 비공개 게시물인 경우 소유자가 아니면 클레임 불가
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            return "POST_NOT_ACCESSIBLE";
        }

        // Build keys in Java to ensure Redis Cluster compatibility
        String setKey = "post:{" + post.getId() + "}:claimants";
        String cntKey = "post:{" + post.getId() + "}:claims_count";
        
        String result = redisService.executeScript(
                redisService.loadScript("claim.lua"),
                List.of(setKey, cntKey),  // Pass both keys as KEYS[1], KEYS[2]
                String.valueOf(user.getId()),
                String.valueOf(post.getQuota()));

        if ("SUCCESS".equals(result)) {
            // 비동기 처리는 응답에 영향 없음
            claimAsyncService.saveClaimAndNotify(post, user);
        }

        return result;
    }

    public String declaimPost(Long postId, Principal principal) {
        // N+1 문제 해결: owner와 함께 조회
        Post post = postRepository.findByIdWithOwner(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        UserAccount user = userAccountRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String result = redisService.executeScript(
                redisService.loadScript("declaim.lua"),
                Collections.singletonList(String.valueOf(postId)),
                String.valueOf(user.getId()));

        if ("SUCCESS".equals(result)) {
            claimAsyncService.deleteClaim(post, user);
        }

        return result;
    }

    public void removeClaim(Long postId, Long userId) {
        // N+1 문제 해결: owner와 함께 조회
        Post post = postRepository.findByIdWithOwner(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String result = redisService.executeScript(
                redisService.loadScript("declaim.lua"),
                Collections.singletonList(String.valueOf(postId)),
                String.valueOf(user.getId()));

        if ("SUCCESS".equals(result)) {
            claimAsyncService.deleteClaim(post, user);
        }
    }
}
