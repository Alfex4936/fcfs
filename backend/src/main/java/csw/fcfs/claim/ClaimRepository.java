package csw.fcfs.claim;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import csw.fcfs.post.Post;
import csw.fcfs.user.UserAccount;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByPostAndUser(Post post, UserAccount user);
}
