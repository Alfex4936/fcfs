package csw.fcfs.post;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import csw.fcfs.config.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import csw.fcfs.post.dto.PostDto;
import csw.fcfs.storage.StorageService;

@WebMvcTest(PostController.class)
public class PostControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "testuser")
    public void shouldCreatePost() throws Exception {
        PostDto postDto = new PostDto(null, "Test Title", "Test Description", (short) 10, Instant.now(), Instant.now().plusSeconds(3600), Collections.singletonList("test"), Collections.emptyList(), 0);
        MockMultipartFile postFile = new MockMultipartFile("post", "", "application/json", objectMapper.writeValueAsBytes(postDto));
        MockMultipartFile imageFile = new MockMultipartFile("images", "test.jpg", "image/jpeg", "test image content".getBytes());

        given(postService.createPost(any(), any(), any())).willReturn(new PostDto(1L, "Test Title", "Test Description", (short) 10, postDto.openAt(), postDto.closeAt(), postDto.tags(), List.of("test.jpg"), 0));

        mvc.perform(multipart("/api/posts")
                        .file(postFile)
                        .file(imageFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Title"));
    }

    @Test
    public void shouldGetPost() throws Exception {
        PostDto postDto = new PostDto(1L, "Test Title", "Test Description", (short) 10, Instant.now(), Instant.now().plusSeconds(3600), Collections.singletonList("test"), Collections.emptyList(), 0);
        given(postService.getPost(1L)).willReturn(postDto);

        mvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Title"));
    }

    @Test
    public void shouldGetAllPosts() throws Exception {
        PostDto postDto = new PostDto(1L, "Test Title", "Test Description", (short) 10, Instant.now(), Instant.now().plusSeconds(3600), Collections.singletonList("test"), Collections.emptyList(), 0);
        given(postService.getAllPosts()).willReturn(Collections.singletonList(postDto));

        mvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Title"));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void shouldUpdatePost() throws Exception {
        PostDto postDto = new PostDto(1L, "Updated Title", "Updated Description", (short) 20, Instant.now(), Instant.now().plusSeconds(7200), Collections.singletonList("updated"), Collections.emptyList(), 0);
        MockMultipartFile postFile = new MockMultipartFile("post", "", "application/json", objectMapper.writeValueAsBytes(postDto));
        MockMultipartFile imageFile = new MockMultipartFile("images", "updated.jpg", "image/jpeg", "updated image content".getBytes());

        given(postService.updatePost(any(), any(), any(), any())).willReturn(new PostDto(1L, "Updated Title", "Updated Description", (short) 20, postDto.openAt(), postDto.closeAt(), postDto.tags(), List.of("updated.jpg"), 0));

        mvc.perform(multipart("/api/posts/1")
                        .file(postFile)
                        .file(imageFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void shouldDeletePost() throws Exception {
        mvc.perform(delete("/api/posts/1"))
                .andExpect(status().isNoContent());
    }
}
