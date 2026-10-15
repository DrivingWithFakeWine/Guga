package pl.piomin.services.guga.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CommentDtos {

    public record CommentCreateRequest(
            @NotBlank @Size(max = 1000, message = "评论长度须为 1-1000 字") String content) {
    }

    public record CommentResponse(Long id, String content, String authorName, Instant createdAt) {
    }

    public record LikeResponse(boolean liked, long likeCount) {
    }

    public record TagResponse(String name, long articleCount) {
    }

    private CommentDtos() {
    }
}
