package csw.fcfs.post.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import csw.fcfs.post.PostVisibility;

/**
 * Cursor-based pagination을 위한 Post DTO
 * 커서로 사용할 수 있는 정렬 가능한 필드들을 포함
 */
public record PostCursorDto(
        Long id,
        String title,
        String description,
        Short quota,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant openAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant closeAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant createdAt,  // 커서용 필드 추가
        List<String> tags,
        List<String> images,
        int currentClaims,
        String ownerEmail,
        Long ownerId,
        PostVisibility visibility,
        UUID shareCode
) {
}
