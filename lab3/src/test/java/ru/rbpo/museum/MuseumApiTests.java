package ru.rbpo.museum;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MuseumApiTests {

    private static final LocalDate TODAY = LocalDate.now();

    private static final String[] TABLES = {"exhibit_placements", "exhibitions", "exhibits", "exhibition_halls", "users"};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        for (String table : TABLES) {
            jdbcTemplate.execute("DELETE FROM " + table);
            jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH 1");
        }
    }

    @Test
    void exhibitCrudLifecycle() throws Exception {
        long id = create("/api/exhibits", """
                {"name": "Ваза эпохи Мин", "condition": "GOOD", "requiredArea": 2.5}
                """);

        mockMvc.perform(get("/api/exhibits")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/exhibits/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ваза эпохи Мин"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
        patchJson("/api/exhibits/" + id, "{\"condition\": \"SATISFACTORY\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("SATISFACTORY"))
                .andExpect(jsonPath("$.requiredArea").value(2.5));
        mockMvc.perform(delete("/api/exhibits/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/exhibits/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void serverSetsIdentityStatusAndTimestamps() throws Exception {
        postJson("/api/exhibitions", """
                {"title": "Выставка", "startDate": "%s", "endDate": "%s", "status": "COMPLETED",
                 "createdAt": "2000-01-01T00:00:00Z", "id": 999}
                """.formatted(TODAY, TODAY.plusDays(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.not("2000-01-01T00:00:00Z")));
    }

    @Test
    void validationAndIntegrityErrors() throws Exception {
        postJson("/api/exhibits", "{\"name\": \"\", \"condition\": \"GOOD\", \"requiredArea\": -1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.requiredArea", notNullValue()));
        postJson("/api/exhibitions", """
                {"title": "X", "startDate": "%s", "endDate": "%s"}
                """.formatted(TODAY.plusDays(10), TODAY))
                .andExpect(status().isBadRequest());
        create("/api/users", "{\"username\": \"Curator\", \"fullName\": \"К\", \"email\": \"c@m.ru\"}");
        postJson("/api/users", "{\"username\": \"curator\", \"fullName\": \"К2\", \"email\": \"c2@m.ru\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void fullExhibitionLifecycle() throws Exception {
        Scenario s = scenario();

        // Нельзя открыть: экспонат не смонтирован.
        postJson("/api/exhibitions/" + s.exhibitionId + "/open", "").andExpect(status().isConflict());

        postJson("/api/exhibit-placements/" + s.placementId + "/mount", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mountingStatus").value("MOUNTED"))
                .andExpect(jsonPath("$.mountedAt", notNullValue()));
        postJson("/api/exhibitions/" + s.exhibitionId + "/open", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.openedAt", notNullValue()));

        // Во время работы выставки: нельзя перенести экспонат, отправить на реставрацию, удалить куратора и зал.
        postJson("/api/exhibit-placements/" + s.placementId + "/move", "{\"hallId\": " + s.otherHallId + "}")
                .andExpect(status().isConflict());
        postJson("/api/exhibits/" + s.exhibitId + "/send-to-restoration", "").andExpect(status().isConflict());
        mockMvc.perform(delete("/api/users/{id}", s.curatorId)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/exhibition-halls/{id}", s.hallId)).andExpect(status().isConflict());
        postJson("/api/exhibitions/" + s.exhibitionId + "/open", "").andExpect(status().isConflict());

        postJson("/api/exhibitions/" + s.exhibitionId + "/close", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.endDate").value(TODAY.toString()));
        mockMvc.perform(get("/api/exhibit-placements/{id}", s.placementId))
                .andExpect(jsonPath("$.mountingStatus").value("DISMANTLED"))
                .andExpect(jsonPath("$.mountedAt", nullValue()));
        postJson("/api/exhibitions/" + s.exhibitionId + "/cancel", "").andExpect(status().isConflict());
    }

    @Test
    void openRequiresCurator() throws Exception {
        Scenario s = scenario();
        postJson("/api/exhibit-placements/" + s.placementId + "/mount", "").andExpect(status().isOk());
        mockMvc.perform(delete("/api/users/{id}", s.curatorId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/exhibitions/{id}", s.exhibitionId)).andExpect(jsonPath("$.curatorId", nullValue()));
        postJson("/api/exhibitions/" + s.exhibitionId + "/open", "").andExpect(status().isConflict());
    }

    @Test
    void restorationBlocksMountingAndFreesHall() throws Exception {
        Scenario s = scenario();
        postJson("/api/exhibits/" + s.exhibitId + "/send-to-restoration", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("NEEDS_RESTORATION"));
        mockMvc.perform(get("/api/exhibit-placements/{id}", s.placementId))
                .andExpect(jsonPath("$.mountingStatus").value("DISMANTLED"));
        postJson("/api/exhibit-placements/" + s.placementId + "/mount", "").andExpect(status().isConflict());
        postJson("/api/exhibits/" + s.exhibitId + "/send-to-restoration", "").andExpect(status().isConflict());

        long newPlacement = create("/api/exhibit-placements", placementJson(s.exhibitId, s.otherExhibitionId, s.hallId));
        postJson("/api/exhibit-placements/" + newPlacement + "/mount", "").andExpect(status().isConflict());
    }

    @Test
    void moveChecksTargetHall() throws Exception {
        Scenario s = scenario();
        long closedHall = create("/api/exhibition-halls", "{\"name\": \"Закрытый\", \"area\": 100, \"available\": false}");
        long tinyHall = create("/api/exhibition-halls", "{\"name\": \"Крошечный\", \"area\": 1}");

        postJson("/api/exhibit-placements/" + s.placementId + "/move", "{\"hallId\": " + closedHall + "}")
                .andExpect(status().isConflict());
        postJson("/api/exhibit-placements/" + s.placementId + "/move", "{\"hallId\": " + tinyHall + "}")
                .andExpect(status().isConflict());
        postJson("/api/exhibit-placements/" + s.placementId + "/move", "{\"hallId\": " + s.hallId + "}")
                .andExpect(status().isBadRequest());
        postJson("/api/exhibit-placements/" + s.placementId + "/mount", "").andExpect(status().isOk());
        postJson("/api/exhibit-placements/" + s.placementId + "/move", "{\"hallId\": " + s.otherHallId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hallId").value(s.otherHallId))
                .andExpect(jsonPath("$.mountingStatus").value("PLANNED"));
    }

    @Test
    void hallCapacityAccountsForOverlappingExhibitions() throws Exception {
        long hall = create("/api/exhibition-halls", "{\"name\": \"Малый\", \"area\": 10}");
        long big = create("/api/exhibits", "{\"name\": \"Скульптура\", \"condition\": \"GOOD\", \"requiredArea\": 8}");
        long small = create("/api/exhibits", "{\"name\": \"Монета\", \"condition\": \"GOOD\", \"requiredArea\": 3}");
        long first = create("/api/exhibitions", exhibitionJson("Первая", TODAY, TODAY.plusDays(30), null));
        long overlapping = create("/api/exhibitions", exhibitionJson("Вторая", TODAY.plusDays(10), TODAY.plusDays(40), null));
        long later = create("/api/exhibitions", exhibitionJson("Третья", TODAY.plusDays(31), TODAY.plusDays(60), null));

        create("/api/exhibit-placements", placementJson(big, first, hall));
        postJson("/api/exhibit-placements", placementJson(small, overlapping, hall)).andExpect(status().isConflict());
        create("/api/exhibit-placements", placementJson(small, later, hall));
        // Сдвиг дат третьей выставки на пересечение с первой переполнил бы зал.
        patchJson("/api/exhibitions/" + later, "{\"startDate\": \"" + TODAY.plusDays(20) + "\"}")
                .andExpect(status().isConflict());
        patchJson("/api/exhibition-halls/" + hall, "{\"area\": 5}").andExpect(status().isConflict());
    }

    @Test
    void deletingExhibitionRemovesPlacements() throws Exception {
        Scenario s = scenario();
        mockMvc.perform(delete("/api/exhibitions/{id}", s.exhibitionId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/exhibit-placements/{id}", s.placementId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/exhibits/{id}", s.exhibitId)).andExpect(status().isOk());
    }

    private record Scenario(long curatorId, long exhibitId, long hallId, long otherHallId, long exhibitionId,
                            long otherExhibitionId, long placementId) {
    }

    private Scenario scenario() throws Exception {
        long curator = create("/api/users", "{\"username\": \"curator\", \"fullName\": \"Анна\", \"email\": \"a@m.ru\"}");
        long exhibit = create("/api/exhibits", "{\"name\": \"Картина\", \"condition\": \"GOOD\", \"requiredArea\": 3}");
        long hall = create("/api/exhibition-halls", "{\"name\": \"Зал А\", \"area\": 50}");
        long otherHall = create("/api/exhibition-halls", "{\"name\": \"Зал Б\", \"area\": 50}");
        long exhibition = create("/api/exhibitions", exhibitionJson("Живопись", TODAY, TODAY.plusDays(30), curator));
        long otherExhibition = create("/api/exhibitions",
                exhibitionJson("Графика", TODAY.plusDays(60), TODAY.plusDays(90), curator));
        long placement = create("/api/exhibit-placements", placementJson(exhibit, exhibition, hall));
        return new Scenario(curator, exhibit, hall, otherHall, exhibition, otherExhibition, placement);
    }

    private static String exhibitionJson(String title, LocalDate start, LocalDate end, Long curatorId) {
        return "{\"title\": \"%s\", \"startDate\": \"%s\", \"endDate\": \"%s\", \"curatorId\": %s}"
                .formatted(title, start, end, curatorId);
    }

    private static String placementJson(long exhibitId, long exhibitionId, long hallId) {
        return "{\"exhibitId\": %d, \"exhibitionId\": %d, \"hallId\": %d}".formatted(exhibitId, exhibitionId, hallId);
    }

    private ResultActions postJson(String url, String json) throws Exception {
        var request = post(url);
        if (!json.isEmpty()) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(request);
    }

    private ResultActions patchJson(String url, String json) throws Exception {
        return mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long create(String url, String json) throws Exception {
        String body = postJson(url, json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
