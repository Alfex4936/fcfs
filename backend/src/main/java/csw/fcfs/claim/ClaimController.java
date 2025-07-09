package csw.fcfs.claim;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping("/{postId}")
    public ResponseEntity<String> claimPost(@PathVariable Long postId, Principal principal) {
        return ResponseEntity.ok(claimService.claimPost(postId, principal));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<String> declaimPost(@PathVariable Long postId, Principal principal) {
        return ResponseEntity.ok(claimService.declaimPost(postId, principal));
    }
}
