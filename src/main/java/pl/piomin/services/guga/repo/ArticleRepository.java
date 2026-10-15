package pl.piomin.services.guga.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.piomin.services.guga.entity.Article;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = "author")
    @Query(value = """
            select distinct a from Article a
            left join a.tags t
            where (:tag is null or t.name = :tag)
              and (:q is null or lower(a.title) like :q
                                or lower(a.content) like :q)
            """,
            countQuery = """
            select count(distinct a.id) from Article a
            left join a.tags t
            where (:tag is null or t.name = :tag)
              and (:q is null or lower(a.title) like :q
                                or lower(a.content) like :q)
            """)
    Page<Article> findByFilters(@Param("tag") String tag, @Param("q") String q, Pageable pageable);

    /** 按 id 批量取回并连带标签，避免分页列表的 N+1 查询。 */
    @Query("select a from Article a join fetch a.tags where a.id in :ids")
    List<Article> findWithTags(@Param("ids") List<Long> ids);

    @Query("select t.name, count(a.id) from Article a join a.tags t group by t.name order by count(a.id) desc")
    List<Object[]> countArticlesPerTag();

    @Modifying
    @Query("update Article a set a.likeCount = a.likeCount + 1 where a.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("update Article a set a.likeCount = a.likeCount - 1 where a.id = :id and a.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("update Article a set a.commentCount = a.commentCount + 1 where a.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("update Article a set a.commentCount = a.commentCount - 1 where a.id = :id and a.commentCount > 0")
    void decrementCommentCount(@Param("id") Long id);

    @Query("select a.likeCount from Article a where a.id = :id")
    long likeCountOf(@Param("id") Long id);
}
