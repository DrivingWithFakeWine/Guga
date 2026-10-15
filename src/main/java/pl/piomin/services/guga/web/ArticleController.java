package pl.piomin.services.guga.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.piomin.services.guga.dto.ArticleDtos.ArticleDetailResponse;
import pl.piomin.services.guga.dto.ArticleDtos.ArticleSummaryResponse;
import pl.piomin.services.guga.dto.ArticleDtos.ArticleWriteRequest;
import pl.piomin.services.guga.dto.ArticleDtos.PageResponse;
import pl.piomin.services.guga.dto.CommentDtos.TagResponse;
import pl.piomin.services.guga.service.ArticleService;

import java.util.List;

@RestController
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/api/articles")
    public PageResponse<ArticleSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "latest") String sort) {
        return articleService.list(page, size, tag, q, sort);
    }

    @GetMapping("/api/articles/{slug}")
    public ArticleDetailResponse detail(@PathVariable String slug, Authentication authentication) {
        return articleService.getBySlug(slug, authentication);
    }

    @PostMapping("/api/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleDetailResponse create(@Valid @RequestBody ArticleWriteRequest request,
                                        Authentication authentication) {
        return articleService.create(request, authentication);
    }

    @PutMapping("/api/articles/{slug}")
    public ArticleDetailResponse update(@PathVariable String slug,
                                        @Valid @RequestBody ArticleWriteRequest request,
                                        Authentication authentication) {
        return articleService.update(slug, request, authentication);
    }

    @DeleteMapping("/api/articles/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String slug, Authentication authentication) {
        articleService.delete(slug, authentication);
    }

    @GetMapping("/api/tags")
    public List<TagResponse> tags() {
        return articleService.tags();
    }
}
