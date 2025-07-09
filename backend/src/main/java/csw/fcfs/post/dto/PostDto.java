package csw.fcfs.post.dto;

import java.time.Instant;
import java.util.List;

public record PostDto(
        Long id,
        String title,
        String description,
        short quota,
        Instant openAt,
        Instant closeAt,
        List<String> tags,
        List<String> images,
        int currentClaims   // 현재 클레임 수
) {
    // 기존 생성자와의 호환성을 위한 팩토리 메소드
    public static PostDto of(Long id, String title, String description, short quota,
                             Instant openAt, Instant closeAt, List<String> tags, List<String> images) {
        return new PostDto(id, title, description, quota, openAt, closeAt, tags, images, 0);
    }
}
