package csw.fcfs.post;

import csw.fcfs.user.UserAccount;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp; // hibernate v6.6.15
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private short quota;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostState state;

    @Column(name = "open_at", nullable = false)
    private Instant openAt;

    @Column(name = "close_at")
    private Instant closeAt;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    // @Type(StringArrayType.class)
    @JdbcTypeCode(SqlTypes.ARRAY) // hibernate v6+
    @Column(name = "images", columnDefinition = "text[]")
    private String[] images;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<csw.fcfs.claim.Claim> claims;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
