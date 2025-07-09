package csw.fcfs.user.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import csw.fcfs.claim.ClaimService;
import csw.fcfs.post.PostService;
import csw.fcfs.post.dto.PostAdminDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PostService postService;
    private final ClaimService claimService;

    @GetMapping("/posts")
    public ResponseEntity<List<PostAdminDto>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPostsForAdmin());
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, Principal principal) {
        postService.deletePost(postId, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/claims/{postId}/{userId}")
    public ResponseEntity<Void> removeUserFromClaim(@PathVariable Long postId, @PathVariable Long userId) {
        claimService.removeClaim(postId, userId);
        return ResponseEntity.noContent().build();
    }
}
