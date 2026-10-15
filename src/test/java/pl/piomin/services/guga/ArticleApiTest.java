package pl.piomin.services.guga;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import pl.piomin.services.guga.support.IntegrationTestBase;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArticleApiTest extends IntegrationTestBase {

    private static final String CONTENT = "这是一段足够长的正文内容，用来通过最小长度校验，不含特殊引号。";
    private static final String UPDATED_CONTENT = "这是更新之后的一段全新正文内容，同样满足最小长度要求。";

    @Test
    void createReturnsSlugAndExcerpt() throws Exception {
        String token = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        mockMvc.perform(post("/api/articles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Spring 安全实战笔记\",\"content\":\"%s\",\"tags\":[\"Java\",\"Spring\"]}"
                                .formatted(CONTENT)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").isNotEmpty())
                .andExpect(jsonPath("$.slug").value(Matchers.matchesRegex("[0-9a-z]{12}")))
                .andExpect(jsonPath("$.content").value(CONTENT))
                .andExpect(jsonPath("$.author.username").isNotEmpty())
                .andExpect(jsonPath("$.likedByMe").value(false))
                .andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test
    void anonymousCreateRejected() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"匿名发帖测试内容\",\"content\":\"%s\"}".formatted(CONTENT)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tagFilterPaginatesAndCounts() throws Exception {
        String token = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String tag = uniqueSuffix();
        for (int i = 0; i < 3; i++) {
            createArticle(token, "标签分页测试文章第" + i + "篇", CONTENT, "[\"%s\"]".formatted(tag));
        }
        mockMvc.perform(get("/api/articles").param("tag", tag).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content[0].tags[0]").value(tag))
                .andExpect(jsonPath("$.content[0].authorName").isNotEmpty());
        mockMvc.perform(get("/api/articles").param("tag", uniqueSuffix()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void searchByTitleKeyword() throws Exception {
        String token = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String keyword = uniqueSuffix();
        createArticle(token, "标题包含关键词" + keyword, CONTENT, "[]");
        createArticle(token, "无关文章不匹配任何词", CONTENT, "[]");
        mockMvc.perform(get("/api/articles").param("q", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("标题包含关键词" + keyword));
    }

    @Test
    void hottestSortOrdersByLikes() throws Exception {
        String tokenA = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String tokenB = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String hotSlug = createArticle(tokenA, "热门文章点赞排第一", CONTENT, "[]");
        createArticle(tokenA, "冷门文章没有获赞", CONTENT, "[]");
        mockMvc.perform(post("/api/articles/{slug}/like", hotSlug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));
        mockMvc.perform(get("/api/articles").param("sort", "hottest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value(hotSlug));
    }

    @Test
    void authorCanUpdateArticle() throws Exception {
        String token = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String slug = createArticle(token, "原始标题内容不少于五字", CONTENT, "[]");
        mockMvc.perform(put("/api/articles/{slug}", slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"更新之后的全新标题\",\"content\":\"%s\",\"tags\":[\"updated\"]}"
                                .formatted(UPDATED_CONTENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("更新之后的全新标题"))
                .andExpect(jsonPath("$.tags[0]").value("updated"));
    }

    @Test
    void nonAuthorCannotModifyOrDelete() throws Exception {
        String tokenA = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String tokenB = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String slug = createArticle(tokenA, "只允许作者修改的文章", CONTENT, "[]");
        String body = "{\"title\":\"越权修改的假标题\",\"content\":\"%s\"}".formatted(UPDATED_CONTENT);
        mockMvc.perform(put("/api/articles/{slug}", slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只能操作自己的文章"));
        mockMvc.perform(delete("/api/articles/{slug}", slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteByAuthorThenNotFound() throws Exception {
        String token = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String slug = createArticle(token, "将被作者删除的文章", CONTENT, "[]");
        mockMvc.perform(delete("/api/articles/{slug}", slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/articles/{slug}", slug))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("文章不存在"));
    }

    @Test
    void unknownSlugNotFound() throws Exception {
        mockMvc.perform(get("/api/articles/{slug}", "doesnotexist0"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidCreateRejected() throws Exception {
        String token = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        mockMvc.perform(post("/api/articles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"短\",\"content\":\"短\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(post("/api/articles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"标签超限的合法标题\",\"content\":\"%s\",\"tags\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"]}"
                                .formatted(CONTENT)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tagsEndpointListsTagWithCount() throws Exception {
        String token = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String tag = uniqueSuffix();
        createArticle(token, "标签云统计测试文章", CONTENT, "[\"%s\"]".formatted(tag));
        String body = mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"name\":\"" + tag + "\""), "标签应出现在标签云中: " + body);
    }
}
