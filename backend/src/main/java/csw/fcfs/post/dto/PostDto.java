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
        int currentClaims,   // 현재 클레임 수
        String authorName,   // 작성자 이름 (이메일)
        Long authorId        // 작성자 ID (선택적, 필요시 사용)
) {
    // 기존 생성자와의 호환성을 위한 팩토리 메소드
    public static PostDto of(Long id, String title, String description, short quota,
                             Instant openAt, Instant closeAt, List<String> tags, List<String> images) {
        return new PostDto(id, title, description, quota, openAt, closeAt, tags, images, 0, null, null);
    }

    // 작성자 정보 포함한 팩토리 메소드
    public static PostDto withAuthor(Long id, String title, String description, short quota,
                                   Instant openAt, Instant closeAt, List<String> tags, List<String> images,
                                   int currentClaims, String authorName, Long authorId) {
        return new PostDto(id, title, description, quota, openAt, closeAt, tags, images, currentClaims, authorName, authorId);
    }
}
