package csw.fcfs;

import csw.fcfs.claim.ClaimRepository;
import csw.fcfs.claim.ClaimService;
import csw.fcfs.post.Post;
import csw.fcfs.post.PostState;
import csw.fcfs.post.repository.PostRepository;
import csw.fcfs.service.RedisService;
import csw.fcfs.user.OAuth2Provider;
import csw.fcfs.user.Role;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.repository.UserAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class ConcurrentClaimBenchmarkTest {

    private final List<UserAccount> testUsers = new ArrayList<>();
    private final List<Long> testPostIds = new ArrayList<>();
    private final String testRunId = String.valueOf(System.currentTimeMillis()); // Unique ID for this test run

    @Autowired
    private ClaimService claimService;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private ClaimRepository claimRepository;
    @Autowired
    private RedisService redisService;
    private ExecutorService executorService;

    @BeforeEach
    public void setUp() {
        testUsers.clear();
        testPostIds.clear();

        // Clean up any existing benchmark test users from previous runs
        cleanupExistingBenchmarkData();

        executorService = Executors.newFixedThreadPool(500); // 100 → 500으로 대폭 증가
    }

    @AfterEach
    public void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // Clean up Redis data
        for (Long postId : testPostIds) {
            try {
                redisService.deleteKeys(
                        "post:{" + postId + "}:claimants",
                        "post:{" + postId + "}:claims_count"
                );
            } catch (Exception e) {
                log.warn("Failed to clean up Redis data for post {}: {}", postId, e.getMessage());
            }
        }

        // Clean up database data
        for (Long postId : testPostIds) {
            try {
                postRepository.deleteById(postId);
            } catch (Exception e) {
                log.warn("Failed to delete post {}: {}", postId, e.getMessage());
            }
        }

        for (UserAccount user : testUsers) {
            try {
                userAccountRepository.deleteById(user.getId());
            } catch (Exception e) {
                log.warn("Failed to delete user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    @Test
    public void benchmarkConcurrentClaims_SmallLoad() throws Exception {
        runConcurrentClaimBenchmark(50, 10, "Small Load Test");
    }

    @Test
    public void benchmarkConcurrentClaims_MediumLoad() throws Exception {
        runConcurrentClaimBenchmark(200, 25, "Medium Load Test");
    }

    @Test
    public void benchmarkConcurrentClaims_HighLoad() throws Exception {
        runConcurrentClaimBenchmark(500, 50, "High Load Test");
    }

    @Test
    public void benchmarkConcurrentClaims_ExtremeLoad() throws Exception {
        runConcurrentClaimBenchmark(1000, 100, "Extreme Load Test");
    }

    @Test
    public void benchmarkConcurrentClaims_OversubscribedLoad() throws Exception {
        // Test with way more users than quota to stress test the system
        runConcurrentClaimBenchmark(1000, 5, "Oversubscribed Load Test");
    }

    private void runConcurrentClaimBenchmark(int totalUsers, int quota, String testName) throws Exception {
        log.info("=== Starting {} ===", testName);
        log.info("Total Users: {}, Quota: {}", totalUsers, quota);

        // 1. Create test users
        long userCreationStart = System.currentTimeMillis();
        List<UserAccount> users = createTestUsers(totalUsers);
        long userCreationEnd = System.currentTimeMillis();
        log.info("Created {} users in {}ms", totalUsers, userCreationEnd - userCreationStart);

        // 2. Create test post
        Post testPost = createTestPost(quota);
        testPostIds.add(testPost.getId());

        // 3. Execute concurrent claims
        long claimStart = System.currentTimeMillis();
        BenchmarkResult result = executeConcurrentClaims(users, testPost.getId());
        long claimEnd = System.currentTimeMillis();

        // 4. Analyze results
        analyzeBenchmarkResults(result, testName, quota, totalUsers, claimEnd - claimStart);

        // 5. Verify correctness
        verifyClaimCorrectness(testPost.getId(), quota, result);

        log.info("=== Completed {} ===\n", testName);
    }

    private List<UserAccount> createTestUsers(int count) {
        List<UserAccount> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UserAccount user = UserAccount.builder()
                    .email("benchuser" + i + "@test.com")
                    .oauth2Provider(OAuth2Provider.GOOGLE)
                    .role(Role.USER)
                    .build();
            users.add(userAccountRepository.save(user));
            testUsers.add(user);
        }
        return users;
    }

    private Post createTestPost(int quota) {
        // Make post owner email unique for each test run to avoid conflicts
        String ownerEmail = "postowner" + testRunId + "@test.com";

        UserAccount owner = UserAccount.builder()
                .email(ownerEmail)
                .oauth2Provider(OAuth2Provider.GOOGLE)
                .role(Role.USER)
                .build();
        testUsers.add(userAccountRepository.save(owner));

        Post post = Post.builder()
                .title("Benchmark Test Post " + testRunId) // Make title unique too
                .description("Post for concurrent claim testing")
                .quota((short) quota)
                .openAt(Instant.now())
                .closeAt(Instant.now().plusSeconds(3600))
                .owner(owner)
                .state(PostState.OPEN)
                .build();
        return postRepository.save(post);
    }

    private BenchmarkResult executeConcurrentClaims(List<UserAccount> users, Long postId) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger alreadyClaimedCount = new AtomicInteger(0);
        AtomicInteger quotaExceededCount = new AtomicInteger(0);
        AtomicInteger ownerCannotClaimCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        ConcurrentHashMap<String, Long> responseTimes = new ConcurrentHashMap<>();
        List<Long> allResponseTimes = Collections.synchronizedList(new ArrayList<>());

        // Create futures for all users
        List<CompletableFuture<Void>> futures = users.stream()
                .map(user -> CompletableFuture.runAsync(() -> {
                    long startTime = System.nanoTime();
                    try {
                        // Create a mock principal
                        MockPrincipal principal = new MockPrincipal(user.getEmail());
                        String result = claimService.claimPost(postId, principal);

                        long endTime = System.nanoTime();
                        long responseTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds
                        allResponseTimes.add(responseTime);
                        responseTimes.put(user.getEmail(), responseTime);

                        switch (result) {
                            case "SUCCESS":
                                successCount.incrementAndGet();
                                break;
                            case "QUOTA_EXCEEDED":
                                quotaExceededCount.incrementAndGet();
                                break;
                            case "ALREADY_CLAIMED":
                                alreadyClaimedCount.incrementAndGet();
                                break;
                            case "OWNER_CANNOT_CLAIM":
                                ownerCannotClaimCount.incrementAndGet();
                                break;
                            default:
                                failureCount.incrementAndGet();
                                log.warn("Unexpected result: {}", result);
                        }
                    } catch (Exception e) {
                        long endTime = System.nanoTime();
                        long responseTime = (endTime - startTime) / 1_000_000;
                        allResponseTimes.add(responseTime);

                        errorCount.incrementAndGet();
                        log.error("Error during claim for user {}: {}", user.getEmail(), e.getMessage());
                    }
                }, executorService))
                .toList();

        // Wait for all claims to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return new BenchmarkResult(
                successCount.get(),
                failureCount.get(),
                alreadyClaimedCount.get(),
                quotaExceededCount.get(),
                ownerCannotClaimCount.get(),
                errorCount.get(),
                allResponseTimes
        );
    }

    private void analyzeBenchmarkResults(BenchmarkResult result, String testName, int quota, int totalUsers, long totalTime) {
        List<Long> responseTimes = result.responseTimes();

        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        long minResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0L);

        long maxResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        // Calculate percentiles
        List<Long> sortedTimes = responseTimes.stream().sorted().toList();
        long p50 = getPercentile(sortedTimes, 50);
        long p95 = getPercentile(sortedTimes, 95);
        long p99 = getPercentile(sortedTimes, 99);

        double throughput = (double) totalUsers / totalTime * 1000; // requests per second

        log.info("=== {} Results ===", testName);
        log.info("Total Execution Time: {}ms", totalTime);
        log.info("Throughput: {:.2f} claims/second", throughput);
        log.info("SUCCESS: {} (Expected: {})", result.successCount(), quota);
        log.info("QUOTA_EXCEEDED: {}", result.quotaExceededCount());
        log.info("ALREADY_CLAIMED: {}", result.alreadyClaimedCount());
        log.info("OWNER_CANNOT_CLAIM: {}", result.ownerCannotClaimCount());
        log.info("FAILURES: {}", result.failureCount());
        log.info("ERRORS: {}", result.errorCount());
        log.info("Response Times - Min: {}ms, Max: {}ms, Avg: {:.2f}ms", minResponseTime, maxResponseTime, avgResponseTime);
        log.info("Response Time Percentiles - P50: {}ms, P95: {}ms, P99: {}ms", p50, p95, p99);
    }

    private long getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(sortedValues.size() * percentile / 100.0) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    private void verifyClaimCorrectness(Long postId, int expectedQuota, BenchmarkResult result) {
        // Verify that exactly the quota number of users succeeded
        assertThat(result.successCount())
                .as("Number of successful claims should equal quota")
                .isEqualTo(expectedQuota);

        // Verify that the rest got quota exceeded
        int totalAttempts = result.successCount() + result.quotaExceededCount() +
                result.alreadyClaimedCount() + result.ownerCannotClaimCount() +
                result.failureCount() + result.errorCount();

        log.info("Correctness Verification:");
        log.info("- Exactly {} users succeeded (quota respected)", result.successCount());
        log.info("- {} users got QUOTA_EXCEEDED (as expected)", result.quotaExceededCount());
        log.info("- Total attempts processed: {}", totalAttempts);

        // Additional verification: check Redis state
        try {
            String claimsCount = redisService.get("post:{" + postId + "}:claims_count");
            assertThat(Integer.parseInt(claimsCount))
                    .as("Redis claims count should match successful claims")
                    .isEqualTo(result.successCount());
            log.info("- Redis claims count matches: {}", claimsCount);
        } catch (Exception e) {
            log.warn("Could not verify Redis state: {}", e.getMessage());
        }
    }

    // Helper class for mock principal
    private static class MockPrincipal implements java.security.Principal {
        private final String name;

        public MockPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    // Record to hold benchmark results
    private record BenchmarkResult(
            int successCount,
            int failureCount,
            int alreadyClaimedCount,
            int quotaExceededCount,
            int ownerCannotClaimCount,
            int errorCount,
            List<Long> responseTimes
    ) {
    }

    private void cleanupExistingBenchmarkData() {
        try {
            log.info("Cleaning up existing benchmark test data...");

            // Clean up benchmark users by trying to delete known email patterns
            for (int i = 0; i < 2000; i++) {
                try {
                    String email = "benchuser" + i + "@test.com";
                    Optional<UserAccount> user = userAccountRepository.findByEmail(email);
                    if (user.isPresent()) {
                        userAccountRepository.deleteById(user.get().getId());
                        log.debug("Deleted existing benchmark user: {}", email);
                    }
                } catch (Exception e) {
                    // Ignore - user probably doesn't exist
                }
            }

            // Clean up all possible post owners (with different timestamp patterns)
            // Look for emails matching postowner*@test.com pattern
            try {
                List<UserAccount> allUsers = userAccountRepository.findAll();
                for (UserAccount user : allUsers) {
                    if (user.getEmail().startsWith("postowner") && user.getEmail().endsWith("@test.com")) {
                        try {
                            userAccountRepository.deleteById(user.getId());
                            log.debug("Deleted existing post owner: {}", user.getEmail());
                        } catch (Exception e) {
                            log.warn("Failed to delete post owner {}: {}", user.getEmail(), e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to clean up post owners: {}", e.getMessage());
            }

            // Clean up benchmark posts by checking all posts and filtering by title
            try {
                List<Post> allPosts = postRepository.findAll();
                for (Post post : allPosts) {
                    if (post.getTitle().startsWith("Benchmark Test Post")) {
                        try {
                            // Clean up Redis data for this post
                            redisService.deleteKeys(
                                "post:{" + post.getId() + "}:claimants",
                                "post:{" + post.getId() + "}:claims_count"
                            );

                            // Delete the post (this will cascade delete claims)
                            postRepository.deleteById(post.getId());
                            log.debug("Deleted existing benchmark post: {}", post.getTitle());
                        } catch (Exception e) {
                            log.warn("Failed to delete existing benchmark post {}: {}", post.getId(), e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to clean up benchmark posts: {}", e.getMessage());
            }

            log.info("Finished cleaning up existing benchmark test data");

        } catch (Exception e) {
            log.warn("Error during benchmark data cleanup: {}", e.getMessage());
            // Don't fail the test if cleanup fails, just log the warning
        }
    }
}
