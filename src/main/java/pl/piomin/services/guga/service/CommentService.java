package pl.piomin.services.guga.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.guga.common.ForbiddenOperationException;
import pl.piomin.services.guga.common.ResourceNotFoundException;
import pl.piomin.services.guga.dto.CommentDtos.CommentCreateRequest;
import pl.piomin.services.guga.dto.CommentDtos.CommentResponse;
import pl.piomin.services.guga.entity.Article;
import pl.piomin.services.guga.entity.Comment;
import pl.piomin.services.guga.entity.User;
import pl.piomin.services.guga.repo.ArticleRepository;
import pl.piomin.services.guga.repo.CommentRepository;
import pl.piomin.services.guga.repo.UserRepository;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, ArticleRepository articleRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentResponse add(String slug, CommentCreateRequest request, Authentication authentication) {
        User author = currentUser(authentication);
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setAuthor(author);
        comment.setContent(request.content().trim());
        commentRepository.save(comment);
        articleRepository.incrementCommentCount(article.getId());
        return new CommentResponse(comment.getId(), comment.getContent(), author.getUsername(), comment.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(String slug) {
        return commentRepository.findByArticleSlug(slug).stream()
                .map(c -> new CommentResponse(c.getId(), c.getContent(),
                        c.getAuthor().getUsername(), c.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void delete(long id, Authentication authentication) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));
        if (!comment.getAuthor().getUsername().equals(authentication.getName())) {
            throw new ForbiddenOperationException("只能删除自己的评论");
        }
        commentRepository.delete(comment);
        articleRepository.decrementCommentCount(comment.getArticle().getId());
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("未登录或登录已过期"));
    }
}
