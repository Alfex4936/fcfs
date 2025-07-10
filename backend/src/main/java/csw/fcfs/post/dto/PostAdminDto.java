package csw.fcfs.post.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import csw.fcfs.claim.dto.ClaimDto;
import csw.fcfs.post.PostVisibility;

public record PostAdminDto(
        Long id,
        String title,
        String description,
        int quota,
        Instant openAt,
        Instant closeAt,
        List<String> tags,
        List<String> images,
        List<ClaimDto> claims,
        PostVisibility visibility,
        UUID shareCode
) {
}
