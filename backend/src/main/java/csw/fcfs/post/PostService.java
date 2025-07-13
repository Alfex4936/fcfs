package csw.fcfs.post;

import java.security.Principal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import csw.fcfs.claim.Claim;
import csw.fcfs.claim.dto.ClaimDto;
import csw.fcfs.post.dto.PostAdminDto;
import csw.fcfs.post.dto.PostDto;
import csw.fcfs.post.repository.PostRepository;
import csw.fcfs.service.RedisService;
import csw.fcfs.storage.StorageService;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.dto.UserDto;
import csw.fcfs.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final StorageService storageService;
    private final RedisService redisService;

    @Transactional
    public PostDto createPost(PostDto postDto, List<MultipartFile> images, Principal principal) {
        UserAccount user = userAccountRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        checkPostQuota(user);

        String[] imagePaths = {};
        if (images != null && !images.isEmpty()) {
            imagePaths = images.stream()
                    .filter(file -> file != null && !file.isEmpty())
                    .map(storageService::store)
                    .toArray(String[]::new);
        }

        Post post = Post.builder()
                .title(postDto.title())
                .description(postDto.description())
                .quota(postDto.quota())
                .openAt(postDto.openAt())
                .closeAt(postDto.closeAt())
                .tags(postDto.tags() != null ? postDto.tags().toArray(new String[0]) : null)
                .images(imagePaths)
                .owner(user)
                .state(PostState.SCHEDULED)
                .visibility(postDto.visibility() != null ? postDto.visibility() : PostVisibility.PUBLIC)
                .shareCode(UUID.randomUUID())
                .build();

        Post savedPost = postRepository.save(post);

        user.setMonthlyPostCount(user.getMonthlyPostCount() + 1);
        user.setLastPostDate(Instant.now());
        userAccountRepository.save(user);

        return toDto(savedPost);
    }

    private void checkPostQuota(UserAccount user) {
        if (user.getLastPostDate() != null) {
            // 항상 UTC 기준으로 일관되게 처리
            YearMonth lastPostMonth = YearMonth.from(
                    user.getLastPostDate().atZone(java.time.ZoneId.of("UTC")).toLocalDate()
            );
            YearMonth currentMonth = YearMonth.now(java.time.ZoneId.of("UTC"));

            if (!lastPostMonth.equals(currentMonth)) {
                user.setMonthlyPostCount(0);
            }
        }

        int concurrentPostLimit = user.isPremium() ? 10 : 3;
        int monthlyPostLimit = user.isPremium() ? Integer.MAX_VALUE : 50;

        int activePosts = postRepository.countByOwnerAndStateIn(user,
                Arrays.asList(PostState.SCHEDULED, PostState.OPEN));

        if (activePosts >= concurrentPostLimit) {
            throw new IllegalStateException("You have reached the maximum number of concurrent posts.");
        }

        if (user.getMonthlyPostCount() >= monthlyPostLimit) {
            throw new IllegalStateException("You have reached your monthly post limit.");
        }
    }

    @Deprecated // Use getAllVisiblePosts instead
    @Transactional(readOnly = true)
    public List<PostDto> getAllPosts() {
        return postRepository.findAllWithOwner().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Deprecated // Use getAllVisiblePosts instead
    @Transactional(readOnly = true)
    public Page<PostDto> getAllPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAllWithOwner(pageable);
        return posts.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<PostAdminDto> getAllPostsForAdmin() {
        return postRepository.findAllWithOwnerAndClaims().stream()  // N+1 문제 해결
                .map(this::toAdminDto)
                .collect(Collectors.toList());
    }

    // Privacy-aware post retrieval methods
    @Transactional(readOnly = true)
    public List<PostDto> getAllPublicPosts() {
        return postRepository.findAllPublicWithOwner().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PostDto> getAllPublicPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAllPublicWithOwner(pageable);
        return posts.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<PostDto> getAllVisiblePosts(Principal principal) {
        if (principal == null) {
            return getAllPublicPosts();
        }
        
        UserAccount user = userAccountRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return postRepository.findAllVisibleToUserWithOwner(user).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PostDto> getAllVisiblePosts(Principal principal, Pageable pageable) {
        if (principal == null) {
            return getAllPublicPosts(pageable);
        }
        
        UserAccount user = userAccountRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Page<Post> posts = postRepository.findAllVisibleToUserWithOwner(user, pageable);
        return posts.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public PostDto getPostByShareCode(UUID shareCode) {
        Post post = postRepository.findByShareCodeWithOwner(shareCode)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        return toDto(post);
    }

    // Enhanced getPost with privacy check
    @Transactional(readOnly = true)
    public PostDto getPost(Long id, Principal principal) {
        Post post = postRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        // Check if user can access this post
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            if (principal == null) {
                throw new IllegalArgumentException("This post is private");
            }
            
            UserAccount user = userAccountRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            if (!post.getOwner().equals(user)) {
                throw new IllegalArgumentException("You don't have permission to view this post");
            }
        }
        
        return toDto(post);
    }

    // Backward-compatible method for public posts only
    @Deprecated
    @Transactional(readOnly = true)
    public PostDto getPost(Long id) {
        return getPost(id, null);
    }

    private PostDto toDto(Post post) {
        String claimantsKey = "post:{" + post.getId() + "}:claimants";
        Long claimantsCount = redisService.getSetSize(claimantsKey);
        
        // If Redis data is missing (expired), rebuild from PostgreSQL
        int currentClaims;
        if (claimantsCount == null) {
            // Fallback: count from database and rebuild cache
            currentClaims = post.getClaims() != null ? post.getClaims().size() : 0;
            rebuildClaimCache(post);
        } else {
            currentClaims = claimantsCount.intValue();
        }

        return new PostDto(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                post.getQuota(),
                post.getOpenAt(),
                post.getCloseAt(),
                post.getTags() != null ? Arrays.asList(post.getTags()) : List.of(),
                post.getImages() != null ? Arrays.asList(post.getImages()) : List.of(),
                currentClaims,
                post.getOwner().getEmail(),
                post.getOwner().getId(),
                post.getVisibility(),
                post.getShareCode()
        );
    }
    
    /**
     * Rebuild Redis cache from PostgreSQL data when cache is missing
     */
    private void rebuildClaimCache(Post post) {
        if (post.getClaims() == null || post.getClaims().isEmpty()) {
            return; // No claims to rebuild
        }
        
        String setKey = "post:{" + post.getId() + "}:claimants";
        String cntKey = "post:{" + post.getId() + "}:claims_count";
        
        // Clear existing data first
        redisService.deleteKeys(setKey, cntKey);
        
        // Rebuild by calling claim.lua for each existing claim
        for (Claim claim : post.getClaims()) {
            redisService.executeScript(
                    redisService.loadScript("claim.lua"),
                    List.of(setKey, cntKey),
                    String.valueOf(claim.getUser().getId()),
                    String.valueOf(post.getQuota())
            );
        }
    }

    private PostAdminDto toAdminDto(Post post) {
        List<ClaimDto> claims = post.getClaims() != null ? post.getClaims().stream()
                .map(this::toDto)
                .collect(Collectors.toList()) : List.of();
        return new PostAdminDto(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                post.getQuota(),
                post.getOpenAt(),
                post.getCloseAt(),
                post.getTags() != null ? Arrays.asList(post.getTags()) : List.of(),
                post.getImages() != null ? Arrays.asList(post.getImages()) : List.of(),
                claims,
                post.getVisibility(),
                post.getShareCode()
        );
    }

    private ClaimDto toDto(Claim claim) {
        UserDto userDto = new UserDto(claim.getUser().getId(), claim.getUser().getEmail());
        return new ClaimDto(claim.getId(), userDto);
    }

    @Transactional
    public PostDto updatePost(Long id, PostDto postDto, List<MultipartFile> images, Principal principal) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        UserAccount user = userAccountRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!post.getOwner().equals(user)) {
            throw new SecurityException("User is not the owner of the post");
        }

        String claimantsKey = "post:{" + id + "}:claimants";
        Long claimantsCount = redisService.getSetSize(claimantsKey);

        if (claimantsCount != null && claimantsCount > 0) {
            throw new IllegalStateException("Cannot update post with existing claims.");
        }

        // For simplicity, we'll just replace the images. A more sophisticated implementation might handle adding/removing specific images.
        if (post.getImages() != null) {
            Arrays.stream(post.getImages()).forEach(storageService::delete);
        }
        String[] imagePaths = {};
        if (images != null && !images.isEmpty()) {
            imagePaths = images.stream()
                    .filter(file -> file != null && !file.isEmpty())
                    .map(storageService::store)
                    .toArray(String[]::new);
        }

        post.setTitle(postDto.title());
        post.setDescription(postDto.description());
        post.setQuota(postDto.quota());
        post.setOpenAt(postDto.openAt());
        post.setCloseAt(postDto.closeAt());
        post.setTags(postDto.tags() != null ? postDto.tags().toArray(new String[0]) : null);
        post.setImages(imagePaths);
        post.setVisibility(postDto.visibility() != null ? postDto.visibility() : post.getVisibility());
        // Note: shareCode is not updated - it remains the same for the lifetime of the post

        Post updatedPost = postRepository.save(post);
        return toDto(updatedPost);
    }

    @Transactional
    public void deletePost(Long id, Principal principal) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        UserAccount user = userAccountRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!post.getOwner().equals(user)) {
            throw new SecurityException("User is not the owner of the post");
        }

        // Delete images from storage
        if (post.getImages() != null) {
            Arrays.stream(post.getImages()).forEach(storageService::delete);
        }

        postRepository.delete(post);
    }
}
