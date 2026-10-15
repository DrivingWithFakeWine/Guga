package pl.piomin.services.guga.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.guga.common.ForbiddenOperationException;
import pl.piomin.services.guga.common.ResourceNotFoundException;
import pl.piomin.services.guga.dto.ArticleDtos.ArticleDetailResponse;
import pl.piomin.services.guga.dto.ArticleDtos.ArticleSummaryResponse;
import pl.piomin.services.guga.dto.ArticleDtos.ArticleWriteRequest;
import pl.piomin.services.guga.dto.ArticleDtos.PageResponse;
import pl.piomin.services.guga.dto.AuthDtos.UserResponse;
import pl.piomin.services.guga.dto.CommentDtos.TagResponse;
import pl.piomin.services.guga.entity.Article;
import pl.piomin.services.guga.entity.Tag;
import pl.piomin.services.guga.entity.User;
import pl.piomin.services.guga.repo.ArticleLikeRepository;
import pl.piomin.services.guga.repo.ArticleRepository;
import pl.piomin.services.guga.repo.TagRepository;
import pl.piomin.services.guga.repo.UserRepository;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private static final int SLUG_LENGTH = 12;
    private static final int EXCERPT_LENGTH = 200;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String SLUG_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final UserRepository userRepository;

    public ArticleService(ArticleRepository articleRepository, TagRepository tagRepository,
                          ArticleLikeRepository articleLikeRepository, UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
        this.articleLikeRepository = articleLikeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ArticleDetailResponse create(ArticleWriteRequest request, Authentication authentication) {
        Article article = new Article();
        apply(article, request);
        article.setSlug(generateUniqueSlug());
        article.setAuthor(currentUser(authentication));
        articleRepository.save(article);
        return toDetail(article, false);
    }

    @Transactional
    public ArticleDetailResponse update(String slug, ArticleWriteRequest request, Authentication authentication) {
        Article article = getBySlug(slug);
        requireOwner(article, authentication);
        apply(article, request);
        return toDetail(article, likedByMe(article, authentication));
    }

    @Transactional
    public void delete(String slug, Authentication authentication) {
        Article article = getBySlug(slug);
        requireOwner(article, authentication);
        articleRepository.delete(article);
    }

    @Transactional(readOnly = true)
    public ArticleDetailResponse getBySlug(String slug, Authentication authentication) {
        Article article = getBySlug(slug);
        return toDetail(article, likedByMe(article, authentication));
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleSummaryResponse> list(int page, int size, String tag, String q, String sort) {
        String normalizedTag = blankToNull(tag);
        normalizedTag = normalizedTag == null ? null : normalizedTag.toLowerCase();
        String qLike = blankToNull(q);
        qLike = qLike == null ? null : "%" + qLike.toLowerCase() + "%";
        Sort springSort = "hottest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.DESC, "likeCount", "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        Page<Article> articles = articleRepository.findByFilters(
                normalizedTag, qLike,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), springSort));

        // 一次查询补齐标签，避免逐篇懒加载
        Map<Long, Article> tagged = articles.getContent().isEmpty() ? Map.of()
                : articleRepository.findWithTags(articles.map(Article::getId).getContent()).stream()
                        .collect(Collectors.toMap(Article::getId, Function.identity()));
        List<ArticleSummaryResponse> content = articles.getContent().stream()
                .map(a -> toSummary(a, tagged.getOrDefault(a.getId(), a).getTags()))
                .toList();
        return new PageResponse<>(content, articles.getNumber(), articles.getSize(),
                articles.getTotalElements(), articles.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<TagResponse> tags() {
        return articleRepository.countArticlesPerTag().stream()
                .map(row -> new TagResponse((String) row[0], (Long) row[1]))
                .toList();
    }

    private void apply(Article article, ArticleWriteRequest request) {
        article.setTitle(request.title().trim());
        article.setContent(request.content());
        article.setExcerpt(buildExcerpt(request.content()));
        article.getTags().clear();
        request.tags().stream()
                .filter(t -> t != null && !t.isBlank())
                .map(t -> t.trim().toLowerCase())
                .distinct()
                .forEach(name -> article.getTags().add(tagRepository.findByName(name).orElseGet(() -> {
                    Tag tag = new Tag();
                    tag.setName(name);
                    return tagRepository.save(tag);
                })));
    }

    private String generateUniqueSlug() {
        for (int attempt = 0; attempt < 3; attempt++) {
            String candidate = randomSlug();
            if (!articleRepository.existsBySlug(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("无法生成唯一 slug");
    }

    private String randomSlug() {
        StringBuilder sb = new StringBuilder(SLUG_LENGTH);
        for (int i = 0; i < SLUG_LENGTH; i++) {
            sb.append(SLUG_ALPHABET.charAt(RANDOM.nextInt(SLUG_ALPHABET.length())));
        }
        return sb.toString();
    }

    /** 摘要：去掉 markdown 标记后取前 200 字符。 */
    private String buildExcerpt(String content) {
        String plain = content
                .replaceAll("```[\\s\\S]*?```", " ")
                .replaceAll("[#>*`\\[\\]()!_-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return plain.length() <= EXCERPT_LENGTH ? plain : plain.substring(0, EXCERPT_LENGTH);
    }

    private Article getBySlug(String slug) {
        return articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
    }

    private void requireOwner(Article article, Authentication authentication) {
        if (!article.getAuthor().getUsername().equals(authentication.getName())) {
            throw new ForbiddenOperationException("只能操作自己的文章");
        }
    }

    private Boolean likedByMe(Article article, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName())
                .map(user -> articleLikeRepository.existsByArticleIdAndUserId(article.getId(), user.getId()))
                .orElse(null);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("未登录或登录已过期"));
    }

    private ArticleSummaryResponse toSummary(Article article, java.util.Set<Tag> tags) {
        return new ArticleSummaryResponse(article.getId(), article.getSlug(), article.getTitle(),
                article.getExcerpt(), article.getAuthor().getUsername(),
                tags.stream().map(Tag::getName).sorted().toList(),
                article.getLikeCount(), article.getCommentCount(), article.getCreatedAt());
    }

    private ArticleDetailResponse toDetail(Article article, Boolean likedByMe) {
        return new ArticleDetailResponse(article.getId(), article.getSlug(), article.getTitle(),
                article.getContent(),
                new UserResponse(article.getAuthor().getId(), article.getAuthor().getUsername(),
                        article.getAuthor().getEmail()),
                article.getTags().stream().map(Tag::getName).sorted().toList(),
                article.getLikeCount(), article.getCommentCount(), likedByMe,
                article.getCreatedAt(), article.getUpdatedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
