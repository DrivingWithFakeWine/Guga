package pl.piomin.services.guga.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import pl.piomin.services.guga.dto.AuthDtos.UserResponse;

import java.time.Instant;
import java.util.List;

public final class ArticleDtos {

    public record ArticleWriteRequest(
            @NotBlank @Size(min = 5, max = 120, message = "标题长度须为 5-120 字") String title,
            @NotBlank @Size(min = 10, max = 50000, message = "正文长度须为 10-50000 字") String content,
            @Size(max = 5, message = "标签最多 5 个")
            List<@Size(max = 30, message = "标签长度须为 1-30 字") String> tags) {
    }

    public record ArticleSummaryResponse(Long id, String slug, String title, String excerpt,
                                         String authorName, List<String> tags,
                                         long likeCount, int commentCount, Instant createdAt) {
    }

    public record ArticleDetailResponse(Long id, String slug, String title, String content,
                                        UserResponse author, List<String> tags,
                                        long likeCount, int commentCount, Boolean likedByMe,
                                        Instant createdAt, Instant updatedAt) {
    }

    public record PageResponse<T>(List<T> content, int page, int size,
                                  long totalElements, int totalPages) {

        public static <T> PageResponse<T> of(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }

    private ArticleDtos() {
    }
}
