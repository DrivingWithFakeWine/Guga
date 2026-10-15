package pl.piomin.services.guga.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.guga.common.ResourceNotFoundException;
import pl.piomin.services.guga.dto.CommentDtos.LikeResponse;
import pl.piomin.services.guga.repo.ArticleLikeRepository;
import pl.piomin.services.guga.repo.ArticleRepository;
import pl.piomin.services.guga.repo.UserRepository;

@Service
public class LikeService {

    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final UserRepository userRepository;

    public LikeService(ArticleRepository articleRepository, ArticleLikeRepository articleLikeRepository,
                       UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.articleLikeRepository = articleLikeRepository;
        this.userRepository = userRepository;
    }

    /**
     * 切换点赞。以原生 insert on conflict 的返回行数判定当前状态，
     * 复合主键兜底并发：全部写操作在同一事务内完成。
     */
    @Transactional
    public LikeResponse toggle(String slug, Authentication authentication) {
        Long articleId = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在")).getId();
        Long userId = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("未登录或登录已过期")).getId();

        boolean liked;
        if (articleLikeRepository.insertIgnore(articleId, userId) > 0) {
            articleRepository.incrementLikeCount(articleId);
            liked = true;
        } else if (articleLikeRepository.deleteByArticleAndUser(articleId, userId) > 0) {
            articleRepository.decrementLikeCount(articleId);
            liked = false;
        } else {
            // 并发下另一个请求已取消点赞，本次保持未赞
            liked = false;
        }
        return new LikeResponse(liked, articleRepository.likeCountOf(articleId));
    }
}
