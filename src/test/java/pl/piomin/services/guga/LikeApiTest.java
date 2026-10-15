package pl.piomin.services.guga;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import pl.piomin.services.guga.support.IntegrationTestBase;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LikeApiTest extends IntegrationTestBase {

    private static final String CONTENT = "这是一段足够长的正文内容，用来通过最小长度校验，不含特殊引号。";

    private String like(String token, String slug) throws Exception {
        return mockMvc.perform(post("/api/articles/{slug}/like", slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void toggleAndCountersAcrossUsers() throws Exception {
        String tokenA = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String tokenB = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String slug = createArticle(tokenA, "点赞切换测试文章标题", CONTENT, "[]");

        org.junit.jupiter.api.Assertions.assertEquals("{\"liked\":true,\"likeCount\":1}", like(tokenA, slug));
        org.junit.jupiter.api.Assertions.assertEquals("{\"liked\":false,\"likeCount\":0}", like(tokenA, slug));
        org.junit.jupiter.api.Assertions.assertEquals("{\"liked\":true,\"likeCount\":1}", like(tokenB, slug));

        mockMvc.perform(get("/api/articles/{slug}", slug).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.likedByMe").value(false));
        mockMvc.perform(get("/api/articles/{slug}", slug).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByMe").value(true));
        mockMvc.perform(get("/api/articles/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByMe", nullValue()));
    }

    @Test
    void anonymousLikeRejected() throws Exception {
        String token = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        String slug = createArticle(token, "匿名点赞拦截测试文章", CONTENT, "[]");
        mockMvc.perform(post("/api/articles/{slug}/like", slug))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void likeUnknownSlugRejected() throws Exception {
        String token = registerAndGetToken("user" + uniqueSuffix(), uniqueSuffix() + "@test.com");
        mockMvc.perform(post("/api/articles/{slug}/like", "doesnotexist0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
