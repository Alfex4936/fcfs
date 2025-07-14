package csw.fcfs.post.repository;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import csw.fcfs.post.Post;
import csw.fcfs.post.PostState;
import csw.fcfs.user.UserAccount;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // N+1 문제 해결: owner와 함께 조회 (공개 게시물만)
    @Query("SELECT p FROM Post p JOIN FETCH p.owner WHERE p.visibility = 'PUBLIC' ORDER BY p.id DESC")
    List<Post> findAllPublicWithOwner();

    // N+1 문제 해결: owner와 함께 조회 (공개 + 사용자 본인 게시물)
    @Query("SELECT p FROM Post p JOIN FETCH p.owner WHERE p.visibility = 'PUBLIC' OR p.owner = :user ORDER BY p.id DESC")
    List<Post> findAllVisibleToUserWithOwner(@Param("user") UserAccount user);

    // 페이지네이션용 N+1 문제 해결 (공개 게시물만)
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.owner WHERE p.visibility = 'PUBLIC'",
            countQuery = "SELECT count(p) FROM Post p WHERE p.visibility = 'PUBLIC'")
    Page<Post> findAllPublicWithOwner(Pageable pageable);

    // 페이지네이션용 N+1 문제 해결 (공개 + 사용자 본인 게시물)
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.owner WHERE p.visibility = 'PUBLIC' OR p.owner = :user",
            countQuery = "SELECT count(p) FROM Post p WHERE p.visibility = 'PUBLIC' OR p.owner = :user")
    Page<Post> findAllVisibleToUserWithOwner(@Param("user") UserAccount user, Pageable pageable);

    // 공유 코드로 게시물 조회
    @Query("SELECT p FROM Post p JOIN FETCH p.owner WHERE p.shareCode = :shareCode")
    Optional<Post> findByShareCodeWithOwner(@Param("shareCode") UUID shareCode);

    // 기존 메서드들 (하위 호환성 유지, 하지만 사용 권장하지 않음)
    @Query("SELECT p FROM Post p JOIN FETCH p.owner ORDER BY p.id DESC")
    List<Post> findAllWithOwner();

    // 페이지네이션용 N+1 문제 해결
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.owner",
            countQuery = "SELECT count(p) FROM Post p")
    Page<Post> findAllWithOwner(Pageable pageable);

    // Admin용: claims와 claims.user까지 함께 조회
    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.owner " +
            "LEFT JOIN FETCH p.claims c " +
            "LEFT JOIN FETCH c.user")
    List<Post> findAllWithOwnerAndClaims();

    // 단일 게시물 조회도 N+1 문제 해결
    @Query("SELECT p FROM Post p JOIN FETCH p.owner WHERE p.id = :id")
    Optional<Post> findByIdWithOwner(@Param("id") Long id);

    @Lock(PESSIMISTIC_WRITE)
    @Query("select p from Post p join fetch p.owner where p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") Long id);

    // 기존 메서드들
    int countByOwnerAndStateIn(UserAccount owner, List<PostState> states);

    // 커서 기반 페이지네이션 - 생성시간 기준 (최신순)
    @Query("SELECT p FROM Post p JOIN FETCH p.owner WHERE p.visibility = 'PUBLIC' AND p.createdAt < :cursor ORDER BY p.createdAt DESC, p.id DESC")
    List<Post> findPublicPostsAfterCursor(@Param("cursor") java.time.Instant cursor, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.owner WHERE p.visibility = 'PUBLIC' AND p.createdAt > :cursor ORDER BY p.createdAt ASC, p.id ASC")
    List<Post> findPublicPostsBeforeCursor(@Param("cursor") java.time.Instant cursor, Pageable pageable);

    // 커서 없이 시작 (첫 페이지)
    @Query("SELECT p FROM Post p JOIN FETCH p.owner WHERE p.visibility = 'PUBLIC' ORDER BY p.createdAt DESC, p.id DESC")
    List<Post> findFirstPublicPosts(Pageable pageable);

    // 사용자별 가시성 고려한 커서 기반 페이지네이션
    @Query("SELECT p FROM Post p JOIN FETCH p.owner WHERE (p.visibility = 'PUBLIC' OR p.owner = :user) AND p.createdAt < :cursor ORDER BY p.createdAt DESC, p.id DESC")
    List<Post> findVisiblePostsAfterCursor(@Param("user") UserAccount user, @Param("cursor") java.time.Instant cursor, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.owner WHERE (p.visibility = 'PUBLIC' OR p.owner = :user) AND p.createdAt > :cursor ORDER BY p.createdAt ASC, p.id ASC")
    List<Post> findVisiblePostsBeforeCursor(@Param("user") UserAccount user, @Param("cursor") java.time.Instant cursor, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.owner WHERE p.visibility = 'PUBLIC' OR p.owner = :user ORDER BY p.createdAt DESC, p.id DESC")
    List<Post> findFirstVisiblePosts(@Param("user") UserAccount user, Pageable pageable);
}
