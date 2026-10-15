package pl.piomin.services.guga.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.piomin.services.guga.entity.ArticleLike;
import pl.piomin.services.guga.entity.ArticleLikeId;

public interface ArticleLikeRepository extends JpaRepository<ArticleLike, ArticleLikeId> {

    /** 原生 upsert：返回 1 表示本次新建（即点赞成功），0 表示原本已存在（未点赞）。复合主键保证并发安全。 */
    @Modifying
    @Query(value = "insert into article_likes (article_id, user_id) values (:articleId, :userId) on conflict do nothing",
            nativeQuery = true)
    int insertIgnore(@Param("articleId") Long articleId, @Param("userId") Long userId);

    @Modifying
    @Query(value = "delete from article_likes where article_id = :articleId and user_id = :userId",
            nativeQuery = true)
    int deleteByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    boolean existsByArticleIdAndUserId(Long articleId, Long userId);
}
