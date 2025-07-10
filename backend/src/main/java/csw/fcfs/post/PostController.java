package csw.fcfs.post;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<PostDto> getPost(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(postService.getPost(id, principal));
    }

    @GetMapping
    public ResponseEntity<Page<PostDto>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {

        // 정렬 방향 설정
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
            Sort.Direction.DESC : Sort.Direction.ASC;

        // Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(postService.getAllVisiblePosts(principal, pageable));
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

    @GetMapping("/share/{shareCode}")
    public ResponseEntity<PostDto> getPostByShareCode(@PathVariable UUID shareCode) {
        return ResponseEntity.ok(postService.getPostByShareCode(shareCode));
    }

    @GetMapping("/public")
    public ResponseEntity<Page<PostDto>> getPublicPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // 정렬 방향 설정
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
            Sort.Direction.DESC : Sort.Direction.ASC;

        // Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(postService.getAllPublicPosts(pageable));
    }
}
