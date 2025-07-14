package csw.fcfs.util;

import java.time.Instant;
import java.util.Base64;

/**
 * 커서 기반 페이지네이션을 위한 유틸리티 클래스
 * Base64 인코딩된 타임스탬프를 커서로 사용
 */
public class CursorUtil {

    private CursorUtil() {
        // 유틸리티 클래스는 인스턴스화 방지
    }

    /**
     * Instant를 Base64 인코딩된 커서 문자열로 변환
     * 
     * @param timestamp 변환할 타임스탬프
     * @return Base64 인코딩된 커서 문자열
     */
    public static String encodeCursor(Instant timestamp) {
        if (timestamp == null) {
            return null;
        }
        String isoString = timestamp.toString();
        return Base64.getEncoder().encodeToString(isoString.getBytes());
    }

    /**
     * Base64 인코딩된 커서 문자열을 Instant로 디코딩
     * 
     * @param cursor 디코딩할 커서 문자열
     * @return 파싱된 Instant 객체
     * @throws IllegalArgumentException 커서 형식이 잘못된 경우
     */
    public static Instant decodeCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        
        try {
            String decodedString = new String(Base64.getDecoder().decode(cursor));
            return Instant.parse(decodedString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor format: " + cursor, e);
        }
    }

    /**
     * 커서의 유효성 검증
     * 
     * @param cursor 검증할 커서 문자열
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public static boolean isValidCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return true; // null/empty는 첫 페이지로 간주하여 유효함
        }
        
        try {
            decodeCursor(cursor);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 현재 시점의 커서 생성 (테스트 및 기본값 용도)
     * 
     * @return 현재 시각을 기반으로 한 커서
     */
    public static String createCurrentCursor() {
        return encodeCursor(Instant.now());
    }

    /**
     * 복합 커서 생성 (타임스탬프 + ID 조합)
     * 동일한 타임스탬프를 가진 레코드들의 순서를 보장하기 위해 사용
     * 
     * @param timestamp 기본 정렬 기준이 되는 타임스탬프
     * @param id 보조 정렬 기준이 되는 ID
     * @return 복합 커서 문자열
     */
    public static String encodeCompositeCursor(Instant timestamp, Long id) {
        if (timestamp == null || id == null) {
            return null;
        }
        String compositeString = timestamp.toString() + ":" + id;
        return Base64.getEncoder().encodeToString(compositeString.getBytes());
    }

    /**
     * 복합 커서 디코딩
     * 
     * @param cursor 디코딩할 복합 커서
     * @return [Instant, Long] 배열 (timestamp, id 순서)
     * @throws IllegalArgumentException 커서 형식이 잘못된 경우
     */
    public static Object[] decodeCompositeCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        
        try {
            String decodedString = new String(Base64.getDecoder().decode(cursor));
            String[] parts = decodedString.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid composite cursor format");
            }
            
            Instant timestamp = Instant.parse(parts[0]);
            Long id = Long.parseLong(parts[1]);
            return new Object[]{timestamp, id};
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid composite cursor format: " + cursor, e);
        }
    }

    /**
     * Long ID 기반 단순 커서 (숫자 ID 정렬용)
     * 
     * @param id 인코딩할 ID
     * @return 인코딩된 커서
     */
    public static String encodeIdCursor(Long id) {
        if (id == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(id.toString().getBytes());
    }

    /**
     * Long ID 커서 디코딩
     * 
     * @param cursor 디코딩할 커서
     * @return 파싱된 ID
     * @throws IllegalArgumentException 커서 형식이 잘못된 경우
     */
    public static Long decodeIdCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        
        try {
            String decodedString = new String(Base64.getDecoder().decode(cursor));
            return Long.parseLong(decodedString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ID cursor format: " + cursor, e);
        }
    }
}
