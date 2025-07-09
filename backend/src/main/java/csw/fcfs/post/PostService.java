package csw.fcfs.post;

import java.security.Principal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
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
import jakarta.transaction.Transactional;
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

    public PostDto getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        return toDto(post);
    }

    public List<PostDto> getAllPosts() {
        return postRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<PostAdminDto> getAllPostsForAdmin() {
        return postRepository.findAll().stream()
                .map(this::toAdminDto)
                .collect(Collectors.toList());
    }

    private PostDto toDto(Post post) {
        String claimantsKey = "post:{" + post.getId() + "}:claimants";
        Long claimantsCount = redisService.getSetSize(claimantsKey);
        int currentClaims = claimantsCount != null ? claimantsCount.intValue() : 0;

        return new PostDto(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                post.getQuota(),
                post.getOpenAt(),
                post.getCloseAt(),
                post.getTags() != null ? Arrays.asList(post.getTags()) : List.of(),
                post.getImages() != null ? Arrays.asList(post.getImages()) : List.of(),
                currentClaims
        );
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
                claims
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
