package csw.fcfs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import csw.fcfs.post.PostVisibility;
import csw.fcfs.post.dto.PostDto;
import csw.fcfs.post.repository.PostRepository;
import csw.fcfs.service.RedisService;
import csw.fcfs.user.OAuth2Provider;
import csw.fcfs.user.Role;
import csw.fcfs.user.UserAccount;
import csw.fcfs.user.repository.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class FullFlowTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RedisService redisService;

    // Track test-specific data for cleanup
    private List<UserAccount> testUsers = new ArrayList<>();
    private List<Long> testPostIds = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        // Clear test data trackers
        testUsers.clear();
        testPostIds.clear();

        // Create only the test users needed for this test
        UserAccount testUser = UserAccount.builder()
                .email("testuser@test.com")
                .oauth2Provider(OAuth2Provider.GOOGLE)
                .role(Role.USER)
                .build();
        testUsers.add(userAccountRepository.save(testUser));

        UserAccount claimer1 = UserAccount.builder()
                .email("claimer1@test.com")
                .oauth2Provider(OAuth2Provider.GOOGLE)
                .role(Role.USER)
                .build();
        testUsers.add(userAccountRepository.save(claimer1));
    }

    @AfterEach
    public void tearDown() {
        // Clean up test-specific Redis data
        for (Long postId : testPostIds) {
            try {
                redisService.deleteKeys(
                    "post:{" + postId + "}:claimants",
                    "post:{" + postId + "}:claims_count"
                );
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        // Clean up test posts (this will cascade delete claims)
        for (Long postId : testPostIds) {
            try {
                postRepository.deleteById(postId);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        // Clean up test users
        for (UserAccount user : testUsers) {
            try {
                userAccountRepository.deleteById(user.getId());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    @WithMockUser(username = "testuser@test.com")
    public void fullFlowTest() throws Exception {
        // 1. Create a post
        PostDto postDto = PostDto.withAuthor(null, "Test Title", "Test Description", (short) 2, 
                Instant.now(), Instant.now().plusSeconds(3600), 
                Collections.singletonList("test"), Collections.emptyList(), 
                0, "testuser@test.com", null);
        MockMultipartFile postFile = new MockMultipartFile("post", "", "application/json", objectMapper.writeValueAsBytes(postDto));
        MockMultipartFile imageFile = new MockMultipartFile("images", "test.jpg", "image/jpeg", "test image content".getBytes());

        MvcResult createResult = mvc.perform(multipart("/api/posts")
                        .file(postFile)
                        .file(imageFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andReturn();

        PostDto createdPost = objectMapper.readValue(createResult.getResponse().getContentAsString(), PostDto.class);
        Long postId = createdPost.id();
        testPostIds.add(postId); // Track this post for cleanup

        // 2. Update the post
        PostDto updatedDto = PostDto.withAllFields(postId, "Updated Title", "Updated Description", (short) 2, 
                postDto.openAt(), postDto.closeAt(), postDto.tags(), postDto.images(), 
                0, "testuser@test.com", null, PostVisibility.PUBLIC, postDto.shareCode());
        MockMultipartFile updatedPostFile = new MockMultipartFile("post", "", "application/json", objectMapper.writeValueAsBytes(updatedDto));

        mvc.perform(multipart("/api/posts/" + postId)
                        .file(updatedPostFile)
                        .file(imageFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));

        // 3. Another user claims the post
        mvc.perform(post("/api/claims/" + postId)
                        .with(user("claimer1@test.com")))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo("SUCCESS"));

        // Try to update the post again (should fail)
        mvc.perform(multipart("/api/posts/" + postId)
                        .file(updatedPostFile)
                        .file(imageFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isConflict());

        // 4. Claim until full - create additional user on demand
        UserAccount claimer2 = UserAccount.builder()
                .email("claimer2@test.com")
                .oauth2Provider(OAuth2Provider.GOOGLE)
                .role(Role.USER)
                .build();
        testUsers.add(userAccountRepository.save(claimer2));

        mvc.perform(post("/api/claims/" + postId)
                        .with(user("claimer2@test.com")))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo("SUCCESS"));

        // Next claim should fail - create another user
        UserAccount claimer3 = UserAccount.builder()
                .email("claimer3@test.com")
                .oauth2Provider(OAuth2Provider.GOOGLE)
                .role(Role.USER)
                .build();
        testUsers.add(userAccountRepository.save(claimer3));

        mvc.perform(post("/api/claims/" + postId)
                        .with(user("claimer3@test.com")))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo("QUOTA_EXCEEDED"));
    }
}
