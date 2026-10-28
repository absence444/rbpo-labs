package ru.rbpo.museum;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import ru.rbpo.museum.security.AdminBootstrap;

/** Общие помощники: запросы от имени пользователя (HTTP Basic + CSRF) и очистка БД перед тестом. */
abstract class ApiTestSupport {

    static final Creds ADMIN = new Creds("admin", "Str0ng-Muse#Key");
    static final String STRONG_PASSWORD = "Gallery#Night-2026";

    private static final String[] TABLES = {"exhibit_placements", "exhibitions", "exhibits", "exhibition_halls", "users"};

    record Creds(String username, String password) {
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminBootstrap adminBootstrap;

    @BeforeEach
    void cleanDatabase() {
        for (String table : TABLES) {
            jdbcTemplate.execute("DELETE FROM " + table);
            jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH 1");
        }
        adminBootstrap.ensureAdminExists();
    }

    ResultActions getAs(Creds user, String url) throws Exception {
        return mockMvc.perform(get(url).with(httpBasic(user.username(), user.password())));
    }

    ResultActions postAs(Creds user, String url, String json) throws Exception {
        return mockMvc.perform(withBody(post(url), json).with(httpBasic(user.username(), user.password())).with(csrf()));
    }

    ResultActions patchAs(Creds user, String url, String json) throws Exception {
        return mockMvc.perform(withBody(patch(url), json).with(httpBasic(user.username(), user.password())).with(csrf()));
    }

    ResultActions deleteAs(Creds user, String url) throws Exception {
        return mockMvc.perform(delete(url).with(httpBasic(user.username(), user.password())).with(csrf()));
    }

    long createAs(Creds user, String url, String json) throws Exception {
        String body = postAs(user, url, json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** Создаёт учётную запись с заданной ролью от имени администратора. */
    Creds createUser(String username, String role) throws Exception {
        createAs(ADMIN, "/api/users", """
                {"username": "%s", "fullName": "Тестовый %s", "email": "%s@museum.test",
                 "password": "%s", "role": "%s"}
                """.formatted(username, username, username, STRONG_PASSWORD, role));
        return new Creds(username, STRONG_PASSWORD);
    }

    long idOf(Creds user) throws Exception {
        String body = getAs(user, "/api/auth/me").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /**
     * CSRF как у настоящего клиента (double submit cookie): одинаковое значение в cookie XSRF-TOKEN и
     * заголовке X-XSRF-TOKEN. Стандартный csrf() из spring-security-test не используется, потому что он
     * подменяет репозиторий токенов в общем CsrfFilter.
     */
    static RequestPostProcessor csrf() {
        return request -> {
            String token = UUID.randomUUID().toString();
            request.setCookies(new Cookie("XSRF-TOKEN", token));
            request.addHeader("X-XSRF-TOKEN", token);
            return request;
        };
    }

    private static MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder request, String json) {
        return json.isEmpty() ? request : request.contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
