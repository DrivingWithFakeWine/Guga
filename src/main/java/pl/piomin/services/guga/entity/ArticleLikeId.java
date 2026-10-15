package pl.piomin.services.guga.entity;

import java.io.Serializable;
import java.util.Objects;

public class ArticleLikeId implements Serializable {

    private Long articleId;
    private Long userId;

    public ArticleLikeId() {
    }

    public ArticleLikeId(Long articleId, Long userId) {
        this.articleId = articleId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArticleLikeId that)) {
            return false;
        }
        return Objects.equals(articleId, that.articleId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId, userId);
    }
}
