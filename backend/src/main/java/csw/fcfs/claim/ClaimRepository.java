package csw.fcfs.claim;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import csw.fcfs.post.Post;
import csw.fcfs.user.UserAccount;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByPostAndUser(Post post, UserAccount user);

    @Query("SELECT c FROM Claim c " +
           "JOIN FETCH c.post p " +
           "JOIN FETCH c.user u " +
           "WHERE c.post = :post AND c.user = :user")
    Optional<Claim> findByPostAndUserWithDetails(@Param("post") Post post, @Param("user") UserAccount user);

    @Query("SELECT c FROM Claim c " +
           "JOIN FETCH c.user u " +
           "WHERE c.post = :post " +
           "ORDER BY c.createdAt ASC")
    List<Claim> findByPostWithUser(@Param("post") Post post);

    @Query("SELECT c FROM Claim c " +
           "JOIN FETCH c.post p " +
           "WHERE c.user = :user " +
           "ORDER BY c.createdAt DESC")
    List<Claim> findByUserWithPost(@Param("user") UserAccount user);

    @Query("SELECT c FROM Claim c " +
           "JOIN FETCH c.post p " +
           "JOIN FETCH c.user u " +
           "WHERE c.post = :post " +
           "ORDER BY c.createdAt ASC")
    List<Claim> findByPostWithFullDetails(@Param("post") Post post);

    @Query("SELECT COUNT(c) FROM Claim c WHERE c.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM Claim c WHERE c.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);
}
