package pl.piomin.services.guga.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "article_likes")
@IdClass(ArticleLikeId.class)
public class ArticleLike {

    @Id
    private Long articleId;

    @Id
    private Long userId;

    public Long getArticleId() {
        return articleId;
    }

    public Long getUserId() {
        return userId;
    }
}
