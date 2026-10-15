package pl.piomin.services.guga.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.piomin.services.guga.dto.CommentDtos.CommentCreateRequest;
import pl.piomin.services.guga.dto.CommentDtos.CommentResponse;
import pl.piomin.services.guga.service.CommentService;

import java.util.List;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/api/articles/{slug}/comments")
    public List<CommentResponse> list(@PathVariable String slug) {
        return commentService.list(slug);
    }

    @PostMapping("/api/articles/{slug}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable String slug,
                               @Valid @RequestBody CommentCreateRequest request,
                               Authentication authentication) {
        return commentService.add(slug, request, authentication);
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, Authentication authentication) {
        commentService.delete(id, authentication);
    }
}
