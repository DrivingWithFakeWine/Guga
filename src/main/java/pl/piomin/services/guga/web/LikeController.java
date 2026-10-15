package pl.piomin.services.guga.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.piomin.services.guga.dto.CommentDtos.LikeResponse;
import pl.piomin.services.guga.service.LikeService;

@RestController
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/api/articles/{slug}/like")
    public LikeResponse toggle(@PathVariable String slug, Authentication authentication) {
        return likeService.toggle(slug, authentication);
    }
}
