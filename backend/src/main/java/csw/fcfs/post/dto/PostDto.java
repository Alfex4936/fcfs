package csw.fcfs.post.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import csw.fcfs.post.PostVisibility;

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
        Long authorId,       // 작성자 ID (선택적, 필요시 사용)
        PostVisibility visibility, // 공개/비공개 설정
        UUID shareCode       // 공유 코드
) {
    // 기존 생성자와의 호환성을 위한 팩토리 메소드
    public static PostDto of(Long id, String title, String description, short quota,
                             Instant openAt, Instant closeAt, List<String> tags, List<String> images) {
        return new PostDto(id, title, description, quota, openAt, closeAt, tags, images, 0, null, null, 
                          PostVisibility.PUBLIC, UUID.randomUUID());
    }

    // 작성자 정보 포함한 팩토리 메소드
    public static PostDto withAuthor(Long id, String title, String description, short quota,
                                   Instant openAt, Instant closeAt, List<String> tags, List<String> images,
                                   int currentClaims, String authorName, Long authorId) {
        return new PostDto(id, title, description, quota, openAt, closeAt, tags, images, currentClaims, 
                          authorName, authorId, PostVisibility.PUBLIC, UUID.randomUUID());
    }

    // 완전한 팩토리 메소드
    public static PostDto withAllFields(Long id, String title, String description, short quota,
                                       Instant openAt, Instant closeAt, List<String> tags, List<String> images,
                                       int currentClaims, String authorName, Long authorId, 
                                       PostVisibility visibility, UUID shareCode) {
        return new PostDto(id, title, description, quota, openAt, closeAt, tags, images, currentClaims, 
                          authorName, authorId, visibility, shareCode);
    }
}
