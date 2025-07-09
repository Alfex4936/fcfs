package csw.fcfs.post;

import java.security.Principal;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import csw.fcfs.claim.ClaimService;
import csw.fcfs.post.dto.PostDto;
import csw.fcfs.storage.StorageService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ClaimService claimService;
    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<PostDto> createPost(@RequestPart("post") PostDto postDto,
                                              @RequestPart(value = "images", required = false) List<MultipartFile> images,
                                              Principal principal) {
        return ResponseEntity.ok(postService.createPost(postDto, images, principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @GetMapping
    public ResponseEntity<List<PostDto>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostDto> updatePost(@PathVariable Long id,
                                              @RequestPart("post") PostDto postDto,
                                              @RequestPart("images") List<MultipartFile> images,
                                              Principal principal) {
        return ResponseEntity.ok(postService.updatePost(id, postDto, images, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, Principal principal) {
        postService.deletePost(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<String> claimPost(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(claimService.claimPost(id, principal));
    }

    @GetMapping("/images/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }
}
