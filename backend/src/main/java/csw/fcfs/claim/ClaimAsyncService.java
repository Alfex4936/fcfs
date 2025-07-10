package csw.fcfs.claim;

import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import csw.fcfs.notification.EmailService;
import csw.fcfs.post.Post;
import csw.fcfs.post.repository.PostRepository;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimAsyncService {

    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final ClaimRepository claimRepository;
    private final EmailService emailService;

    @Async("claimExecutor")
    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        isolation = Isolation.READ_UNCOMMITTED,  // Fast reads, can see uncommitted data
        readOnly = false
    )
    public void saveClaimAndNotify(Post post, UserAccount user) {
        try {
            // Minimal delay since READ_UNCOMMITTED should see data immediately
            Thread.sleep(10);
            
            // Try direct lookup first - should work with READ_UNCOMMITTED
            Optional<UserAccount> userOpt = userAccountRepository.findById(user.getId());
            if (userOpt.isEmpty()) {
                // Single retry if needed
                Thread.sleep(25);
                userOpt = userAccountRepository.findById(user.getId());
                if (userOpt.isEmpty()) {
                    log.error("User {} not found after retry, skipping claim save", user.getId());
                    return;
                }
            }
            
            UserAccount freshUser = userOpt.get();
            
            Post freshPost = postRepository.findById(post.getId())
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + post.getId()));
            
            Claim claim = Claim.builder()
                    .post(freshPost)
                    .user(freshUser)
                    .build();
            claimRepository.save(claim);

            String subject = "Congratulations! You've successfully claimed the post: " + freshPost.getTitle();
            String text = "Dear " + freshUser.getEmail() + ",\n\nCongratulations! You have successfully claimed the post titled '" + freshPost.getTitle() + "'.\n\nThank you for using our platform!\n\nBest regards,\nThe FCFS Team";
            emailService.sendEmail(freshUser.getEmail(), subject, text);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while saving claim for user {} and post {}", user.getId(), post.getId());
        } catch (Exception e) {
            log.error("Failed to save claim for user {} and post {}: {}", user.getId(), post.getId(), e.getMessage());
        }
    }

    @Async("claimExecutor")
    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        isolation = Isolation.READ_UNCOMMITTED  // Fast reads for delete operations
    )
    public void deleteClaim(Post post, UserAccount user) {
        try {
            claimRepository.findByPostAndUserWithDetails(post, user)
                    .ifPresent(claimRepository::delete);
        } catch (Exception e) {
            log.error("Failed to delete claim for user {} and post {}: {}", user.getId(), post.getId(), e.getMessage());
        }
    }
}
