package csw.fcfs.post.dto;

import java.util.List;

/**
 * 커서 기반 페이지네이션 응답 DTO
 */
public record CursorPageResponse<T>(
        List<T> content,           // 현재 페이지 데이터
        String nextCursor,         // 다음 페이지를 위한 커서 (null이면 마지막 페이지)
        String prevCursor,         // 이전 페이지를 위한 커서 (null이면 첫 페이지)
        boolean hasNext,           // 다음 페이지 존재 여부
        boolean hasPrev,           // 이전 페이지 존재 여부
        int size,                  // 요청한 페이지 크기
        int numberOfElements,      // 현재 페이지의 실제 요소 수
        boolean first,             // 첫 번째 페이지 여부
        boolean last               // 마지막 페이지 여부
) {
    
    public static <T> CursorPageResponse<T> of(
            List<T> content,
            String nextCursor,
            String prevCursor,
            int requestedSize
    ) {
        boolean hasNext = nextCursor != null;
        boolean hasPrev = prevCursor != null;
        int actualSize = content.size();
        
        return new CursorPageResponse<>(
                content,
                nextCursor,
                prevCursor,
                hasNext,
                hasPrev,
                requestedSize,
                actualSize,
                !hasPrev,  // first
                !hasNext   // last
        );
    }
}
