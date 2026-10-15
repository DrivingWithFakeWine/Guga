package pl.piomin.services.guga;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import pl.piomin.services.guga.support.IntegrationTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentApiTest extends IntegrationTestBase {

    private static final String CONTENT = "这是一段足够长的正文内容，用来通过最小长度校验，不含特殊引号。";

    /** 建一篇评论者 B 不可修改的文章，返回文章作者的 token。 */
    private record Setup(String authorToken, String commenterToken, String slug) {
    }

    private Setup setup() throws Exception {
        String authorToken = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String commenterToken = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String slug = createArticle(authorToken, "评论功能测试文章标题", CONTENT, "[]");
        return new Setup(authorToken, commenterToken, slug);
    }

    @Test
    void addCommentUpdatesListAndCounter() throws Exception {
        Setup s = setup();
        mockMvc.perform(post("/api/articles/{slug}/comments", s.slug())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + s.commenterToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"写得不错，学习了\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.content").value("写得不错，学习了"))
                .andExpect(jsonPath("$.authorName").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        mockMvc.perform(get("/api/articles/{slug}/comments", s.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("写得不错，学习了"));
        mockMvc.perform(get("/api/articles/{slug}", s.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(1));
    }

    @Test
    void anonymousAddRejected() throws Exception {
        Setup s = setup();
        mockMvc.perform(post("/api/articles/{slug}/comments", s.slug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"匿名评论\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyContentRejected() throws Exception {
        Setup s = setup();
        mockMvc.perform(post("/api/articles/{slug}/comments", s.slug())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + s.commenterToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void commentOnUnknownSlugRejected() throws Exception {
        String token = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        mockMvc.perform(post("/api/articles/{slug}/comments", "doesnotexist0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"找不到这篇文章\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyCommentAuthorCanDelete() throws Exception {
        Setup s = setup();
        String id = mockMvc.perform(post("/api/articles/{slug}/comments", s.slug())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + s.commenterToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"即将被删除的评论\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        long commentId = Long.parseLong(objectMapper.readTree(id).get("id").asText());

        // 文章作者也不是评论作者，不能删
        mockMvc.perform(delete("/api/comments/{id}", commentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + s.authorToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只能删除自己的评论"));
        // 评论作者本人可以删，计数回退
        mockMvc.perform(delete("/api/comments/{id}", commentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + s.commenterToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/articles/{slug}", s.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(0));
        mockMvc.perform(get("/api/articles/{slug}/comments", s.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
