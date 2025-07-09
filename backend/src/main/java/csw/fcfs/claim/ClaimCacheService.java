package csw.fcfs.claim;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import csw.fcfs.post.Post;
import csw.fcfs.post.repository.PostRepository;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaimCacheService {

    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;

    @Cacheable(value = "posts", key = "#postId")  // 1시간 TTL
    public Post getPostFromCache(Long postId) {
        return postRepository.findByIdWithOwner(postId)  // N+1 문제 해결
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }

    @Cacheable(value = "users", key = "#email")   // 5분 TTL (자주 변경될 수 있음)
    public UserAccount getUserFromCache(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
