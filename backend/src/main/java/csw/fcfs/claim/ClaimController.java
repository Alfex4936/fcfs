package csw.fcfs.claim;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import csw.fcfs.post.Post;
import csw.fcfs.post.repository.PostRepository;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;

    @PostMapping("/{postId}")
    public ResponseEntity<String> claimPost(@PathVariable Long postId, Principal principal) {
        // Load entities upfront for the optimized claimPost method
        Post post = postRepository.findByIdWithOwner(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        UserAccount user = userAccountRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return ResponseEntity.ok(claimService.claimPost(post, user));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<String> declaimPost(@PathVariable Long postId, Principal principal) {
        return ResponseEntity.ok(claimService.declaimPost(postId, principal));
    }
}
