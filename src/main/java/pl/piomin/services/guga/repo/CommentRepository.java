package pl.piomin.services.guga.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.piomin.services.guga.entity.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("select c from Comment c join fetch c.author where c.article.slug = :slug order by c.createdAt asc")
    List<Comment> findByArticleSlug(@Param("slug") String slug);
}
