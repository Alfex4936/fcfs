package csw.fcfs.post.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import csw.fcfs.post.Post;
import csw.fcfs.post.PostState;
import csw.fcfs.user.UserAccount;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    int countByOwnerAndStateIn(UserAccount owner, List<PostState> states);
}
