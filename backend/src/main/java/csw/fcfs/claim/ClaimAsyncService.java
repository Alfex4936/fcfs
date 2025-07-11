package csw.fcfs.claim;

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
            // No sleep needed! No database lookups needed!
            // The passed entities are already valid - Hibernate will handle attachment
            
            Claim claim = Claim.builder()
                    .post(post)  // Use directly - Hibernate attaches to current session
                    .user(user)  // Use directly - Hibernate attaches to current session
                    .build();
            claimRepository.save(claim);

            String subject = "Congratulations! You've successfully claimed the post: " + post.getTitle();
            String text = "Dear " + user.getEmail() + ",\n\nCongratulations! You have successfully claimed the post titled '" + post.getTitle() + "'.\n\nThank you for using our platform!\n\nBest regards,\nThe FCFS Team";
            emailService.sendEmail(user.getEmail(), subject, text);
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
