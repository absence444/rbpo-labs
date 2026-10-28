package ru.rbpo.museum;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTests extends ApiTestSupport {

    private static final LocalDate TODAY = LocalDate.now();

    private static final String REGISTER_JSON = """
            {"username": "visitor1", "fullName": "Посетитель", "email": "visitor1@museum.test",
             "password": "%s", "role": "ADMIN"}
            """.formatted(STRONG_PASSWORD);

    @Test
    void csrfTokenFlowForRegistration() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        String token = JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");
        Cookie cookie = csrf.getResponse().getCookie("XSRF-TOKEN");

        // Без токена и с поддельным токеном — 403 с кодом CSRF.
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        mockMvc.perform(post("/api/auth/register").cookie(cookie).header("X-XSRF-TOKEN", "forged")
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
                .andExpect(status().isForbidden());

        // С токеном — регистрация, причём роль из запроса игнорируется.
        mockMvc.perform(post("/api/auth/register").cookie(cookie).header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VISITOR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(content().string(not(containsString("password"))));

        getAs(new Creds("visitor1", STRONG_PASSWORD), "/api/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("visitor1"));
    }

    @Test
    void csrfTokenStaysValidAcrossAuthenticatedRequests() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        String token = JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");
        Cookie cookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        // При HTTP Basic аутентификация выполняется в каждом запросе; она не должна сбрасывать CSRF-токен.
        mockMvc.perform(get("/api/auth/me").cookie(cookie).with(httpBasic(ADMIN.username(), ADMIN.password())))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("XSRF-TOKEN"));
        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(post("/api/exhibition-halls").cookie(cookie).header("X-XSRF-TOKEN", token)
                            .with(httpBasic(ADMIN.username(), ADMIN.password()))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Зал " + i + "\", \"area\": 10}"))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void registrationRejectsWeakPasswordsAndDuplicates() throws Exception {
        String weak = """
                {"username": "weakling", "fullName": "W", "email": "weak@museum.test", "password": "%s"}
                """;
        for (String password : new String[]{"short1!A", "alllowercase123!", "NoDigitsHere!!", "NoSpecials12345",
                "Password123!xyz", "Weakling#2026XY"}) {
            postAs(ADMIN, "/api/auth/register", weak.formatted(password))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.password", notNullValue()));
        }
        postAs(ADMIN, "/api/auth/register", REGISTER_JSON).andExpect(status().isCreated());
        postAs(ADMIN, "/api/auth/register", REGISTER_JSON.replace("visitor1@", "other@"))
                .andExpect(status().isConflict());
        postAs(ADMIN, "/api/auth/register", REGISTER_JSON.replace("\"visitor1\"", "\"Visitor2\""))
                .andExpect(status().isConflict());
    }

    @Test
    void authenticationErrorsAreGeneric() throws Exception {
        mockMvc.perform(get("/api/exhibitions"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Basic")))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        String wrongPassword = mockMvc.perform(get("/api/exhibitions").with(httpBasic("admin", "wrong")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownUser = mockMvc.perform(get("/api/exhibitions").with(httpBasic("ghost", "wrong")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        // По ответу нельзя понять, существует ли пользователь.
        org.junit.jupiter.api.Assertions.assertEquals(wrongPassword, unknownUser);
    }

    @Test
    void visitorCanOnlyRead() throws Exception {
        Creds visitor = createUser("visitor", "VISITOR");
        getAs(visitor, "/api/exhibitions").andExpect(status().isOk());
        getAs(visitor, "/api/exhibits").andExpect(status().isOk());
        getAs(visitor, "/api/users").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        postAs(visitor, "/api/exhibition-halls", "{\"name\": \"Зал\", \"area\": 10}").andExpect(status().isForbidden());
        postAs(visitor, "/api/exhibits", "{\"name\": \"Э\", \"condition\": \"GOOD\", \"requiredArea\": 1}")
                .andExpect(status().isForbidden());
        postAs(visitor, "/api/exhibitions", MuseumApiTests.exhibitionJson("В", TODAY, TODAY.plusDays(1), null))
                .andExpect(status().isForbidden());
    }

    @Test
    void curatorManagesOnlyOwnExhibitions() throws Exception {
        Creds owner = createUser("owner", "CURATOR");
        Creds stranger = createUser("stranger", "CURATOR");
        long hall = createAs(ADMIN, "/api/exhibition-halls", "{\"name\": \"Зал\", \"area\": 50}");
        long exhibit = createAs(owner, "/api/exhibits", "{\"name\": \"Э\", \"condition\": \"GOOD\", \"requiredArea\": 1}");

        // Куратор не может назначить куратором другого пользователя; без curatorId назначается он сам.
        postAs(owner, "/api/exhibitions", MuseumApiTests.exhibitionJson("Чужая", TODAY, TODAY.plusDays(5), idOf(stranger)))
                .andExpect(status().isForbidden());
        long exhibition = createAs(owner, "/api/exhibitions", MuseumApiTests.exhibitionJson("Своя", TODAY, TODAY.plusDays(5), null));
        getAs(owner, "/api/exhibitions/" + exhibition).andExpect(jsonPath("$.curatorId").value(idOf(owner)));

        postAs(stranger, "/api/exhibit-placements", MuseumApiTests.placementJson(exhibit, exhibition, hall))
                .andExpect(status().isForbidden());
        long placement = createAs(owner, "/api/exhibit-placements", MuseumApiTests.placementJson(exhibit, exhibition, hall));
        postAs(stranger, "/api/exhibit-placements/" + placement + "/mount", "").andExpect(status().isForbidden());
        postAs(owner, "/api/exhibit-placements/" + placement + "/mount", "").andExpect(status().isOk());
        postAs(stranger, "/api/exhibitions/" + exhibition + "/open", "").andExpect(status().isForbidden());
        patchAs(stranger, "/api/exhibitions/" + exhibition, "{\"title\": \"Захват\"}").andExpect(status().isForbidden());
        postAs(owner, "/api/exhibitions/" + exhibition + "/open", "").andExpect(status().isOk());

        // Куратор не удаляет экспонаты, выставки и залы — это права администратора.
        deleteAs(owner, "/api/exhibits/" + exhibit).andExpect(status().isForbidden());
        deleteAs(owner, "/api/exhibitions/" + exhibition).andExpect(status().isForbidden());
        patchAs(owner, "/api/exhibition-halls/" + hall, "{\"area\": 60}").andExpect(status().isForbidden());

        // Права не обходят бизнес-ограничения: даже администратор не удалит открытую выставку.
        deleteAs(ADMIN, "/api/exhibitions/" + exhibition).andExpect(status().isConflict());
        // Понижение роли куратора с незавершёнными выставками запрещено.
        patchAs(ADMIN, "/api/users/" + idOf(owner), "{\"role\": \"VISITOR\"}").andExpect(status().isConflict());
        patchAs(ADMIN, "/api/users/" + idOf(stranger), "{\"role\": \"VISITOR\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("VISITOR"));
        postAs(stranger, "/api/exhibits", "{\"name\": \"Э2\", \"condition\": \"GOOD\", \"requiredArea\": 1}")
                .andExpect(status().isForbidden());
    }

    @Test
    void blockedUserCannotAuthenticate() throws Exception {
        Creds visitor = createUser("visitor", "VISITOR");
        long id = idOf(visitor);
        patchAs(ADMIN, "/api/users/" + id, "{\"status\": \"BLOCKED\"}").andExpect(status().isOk());
        getAs(visitor, "/api/exhibitions").andExpect(status().isUnauthorized());
        patchAs(ADMIN, "/api/users/" + id, "{\"status\": \"ACTIVE\"}").andExpect(status().isOk());
        getAs(visitor, "/api/exhibitions").andExpect(status().isOk());
    }

    @Test
    void repeatedWrongPasswordsLockAccountTemporarily() throws Exception {
        Creds visitor = createUser("visitor", "VISITOR");
        Creds wrong = new Creds("visitor", "Wrong-Password#1");
        for (int i = 0; i < 5; i++) {
            getAs(wrong, "/api/exhibitions").andExpect(status().isUnauthorized());
        }
        // Даже верный пароль не принимается до окончания блокировки.
        getAs(visitor, "/api/exhibitions").andExpect(status().isUnauthorized());
        getAs(ADMIN, "/api/users/" + 2).andExpect(jsonPath("$.lockedUntil", notNullValue()));
        // Администратор может снять блокировку досрочно.
        patchAs(ADMIN, "/api/users/2", "{\"status\": \"ACTIVE\"}").andExpect(status().isOk());
        getAs(visitor, "/api/exhibitions").andExpect(status().isOk());
    }

    @Test
    void adminCannotLockThemselvesOut() throws Exception {
        long adminId = idOf(ADMIN);
        patchAs(ADMIN, "/api/users/" + adminId, "{\"role\": \"VISITOR\"}").andExpect(status().isConflict());
        patchAs(ADMIN, "/api/users/" + adminId, "{\"status\": \"BLOCKED\"}").andExpect(status().isConflict());
        deleteAs(ADMIN, "/api/users/" + adminId).andExpect(status().isConflict());
    }

    @Test
    void adminCreatesUsersWithRolesAndPasswordIsHashed() throws Exception {
        createUser("curator", "CURATOR");
        getAs(ADMIN, "/api/users")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].role", hasItem("CURATOR")))
                .andExpect(content().string(not(containsString(STRONG_PASSWORD))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void unexpectedRoutesAndMethodsDoNotLeakDetails() throws Exception {
        getAs(ADMIN, "/api/unknown").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/exhibits/abc").with(httpBasic(ADMIN.username(), ADMIN.password())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Exception"))));
    }
}
