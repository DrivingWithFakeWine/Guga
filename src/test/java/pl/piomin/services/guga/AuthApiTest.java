package pl.piomin.services.guga;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import pl.piomin.services.guga.support.IntegrationTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends IntegrationTestBase {

    private String registerBody(String username, String email, String password) {
        return "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(username, email, password);
    }

    @Test
    void registerCreatesUserAndReturnsToken() throws Exception {
        String suffix = uniqueSuffix();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("user" + suffix, suffix + "@test.com", "password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.username").value("user" + suffix));
    }

    @Test
    void duplicateUsernameRejected() throws Exception {
        String username = "user" + uniqueSuffix();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, uniqueSuffix() + "@test.com", "password123")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, uniqueSuffix() + "@test.com", "password123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("用户名已被占用"));
    }

    @Test
    void duplicateEmailRejected() throws Exception {
        String email = uniqueSuffix() + "@test.com";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("user" + uniqueSuffix(), email, "password123")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("user" + uniqueSuffix(), email, "password123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("邮箱已被注册"));
    }

    @Test
    void invalidRegistrationRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("x", "not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void loginReturnsToken() throws Exception {
        String username = "user" + uniqueSuffix();
        String email = uniqueSuffix() + "@test.com";
        registerAndGetToken(username, email);
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value(username));
    }

    @Test
    void wrongPasswordRejected() throws Exception {
        String email = uniqueSuffix() + "@test.com";
        registerAndGetToken("user" + uniqueSuffix(), email);
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("邮箱或密码错误"));
    }

    @Test
    void meReturnsCurrentUserWithValidToken() throws Exception {
        String username = "user" + uniqueSuffix();
        String token = registerAndGetToken(username, uniqueSuffix() + "@test.com");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").isNotEmpty());
    }

    @Test
    void meWithoutTokenRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/auth/me"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void meWithGarbageTokenRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized());
    }
}
