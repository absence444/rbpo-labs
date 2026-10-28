package ru.rbpo.museum;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

/** Бизнес-операции и CRUD (выполняются администратором и куратором). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MuseumApiTests extends ApiTestSupport {

    private static final LocalDate TODAY = LocalDate.now();

    @Test
    void exhibitCrudLifecycle() throws Exception {
        long id = createAs(ADMIN, "/api/exhibits", """
                {"name": "Ваза эпохи Мин", "condition": "GOOD", "requiredArea": 2.5}
                """);

        getAs(ADMIN, "/api/exhibits").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        getAs(ADMIN, "/api/exhibits/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ваза эпохи Мин"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
        patchAs(ADMIN, "/api/exhibits/" + id, "{\"condition\": \"SATISFACTORY\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("SATISFACTORY"))
                .andExpect(jsonPath("$.requiredArea").value(2.5));
        deleteAs(ADMIN, "/api/exhibits/" + id).andExpect(status().isNoContent());
        getAs(ADMIN, "/api/exhibits/" + id).andExpect(status().isNotFound());
    }

    @Test
    void serverSetsIdentityStatusAndTimestamps() throws Exception {
        postAs(ADMIN, "/api/exhibitions", """
                {"title": "Выставка", "startDate": "%s", "endDate": "%s", "status": "COMPLETED",
                 "createdAt": "2000-01-01T00:00:00Z", "id": 999}
                """.formatted(TODAY, TODAY.plusDays(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.createdAt").value(not("2000-01-01T00:00:00Z")));
    }

    @Test
    void validationAndIntegrityErrors() throws Exception {
        postAs(ADMIN, "/api/exhibits", "{\"name\": \"\", \"condition\": \"GOOD\", \"requiredArea\": -1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.requiredArea", notNullValue()));
        postAs(ADMIN, "/api/exhibitions", """
                {"title": "X", "startDate": "%s", "endDate": "%s"}
                """.formatted(TODAY.plusDays(10), TODAY))
                .andExpect(status().isBadRequest());
        // Куратором может быть только пользователь с ролью CURATOR.
        Creds visitor = createUser("visitor", "VISITOR");
        postAs(ADMIN, "/api/exhibitions", exhibitionJson("X", TODAY, TODAY.plusDays(5), idOf(visitor)))
                .andExpect(status().isConflict());
    }

    @Test
    void fullExhibitionLifecycle() throws Exception {
        Scenario s = scenario();
        Creds curator = s.curator;

        postAs(curator, "/api/exhibitions/" + s.exhibitionId + "/open", "").andExpect(status().isConflict());
        postAs(curator, "/api/exhibit-placements/" + s.placementId + "/mount", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mountingStatus").value("MOUNTED"))
                .andExpect(jsonPath("$.mountedAt", notNullValue()));
        postAs(curator, "/api/exhibitions/" + s.exhibitionId + "/open", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.openedAt", notNullValue()));

        postAs(curator, "/api/exhibit-placements/" + s.placementId + "/move", "{\"hallId\": " + s.otherHallId + "}")
                .andExpect(status().isConflict());
        postAs(curator, "/api/exhibits/" + s.exhibitId + "/send-to-restoration", "").andExpect(status().isConflict());
        deleteAs(ADMIN, "/api/users/" + s.curatorId).andExpect(status().isConflict());
        patchAs(ADMIN, "/api/users/" + s.curatorId, "{\"status\": \"BLOCKED\"}").andExpect(status().isConflict());
        deleteAs(ADMIN, "/api/exhibition-halls/" + s.hallId).andExpect(status().isConflict());
        postAs(curator, "/api/exhibitions/" + s.exhibitionId + "/open", "").andExpect(status().isConflict());

        postAs(curator, "/api/exhibitions/" + s.exhibitionId + "/close", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.endDate").value(TODAY.toString()));
        getAs(curator, "/api/exhibit-placements/" + s.placementId)
                .andExpect(jsonPath("$.mountingStatus").value("DISMANTLED"))
                .andExpect(jsonPath("$.mountedAt", nullValue()));
        postAs(curator, "/api/exhibitions/" + s.exhibitionId + "/cancel", "").andExpect(status().isConflict());
    }

    @Test
    void openRequiresCurator() throws Exception {
        Scenario s = scenario();
        postAs(ADMIN, "/api/exhibit-placements/" + s.placementId + "/mount", "").andExpect(status().isOk());
        deleteAs(ADMIN, "/api/users/" + s.curatorId).andExpect(status().isNoContent());
        getAs(ADMIN, "/api/exhibitions/" + s.exhibitionId).andExpect(jsonPath("$.curatorId", nullValue()));
        postAs(ADMIN, "/api/exhibitions/" + s.exhibitionId + "/open", "").andExpect(status().isConflict());
    }

    @Test
    void restorationBlocksMountingAndFreesHall() throws Exception {
        Scenario s = scenario();
        postAs(s.curator, "/api/exhibits/" + s.exhibitId + "/send-to-restoration", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("NEEDS_RESTORATION"));
        getAs(s.curator, "/api/exhibit-placements/" + s.placementId)
                .andExpect(jsonPath("$.mountingStatus").value("DISMANTLED"));
        postAs(s.curator, "/api/exhibit-placements/" + s.placementId + "/mount", "").andExpect(status().isConflict());
        postAs(s.curator, "/api/exhibits/" + s.exhibitId + "/send-to-restoration", "").andExpect(status().isConflict());

        long newPlacement = createAs(s.curator, "/api/exhibit-placements",
                placementJson(s.exhibitId, s.otherExhibitionId, s.hallId));
        postAs(s.curator, "/api/exhibit-placements/" + newPlacement + "/mount", "").andExpect(status().isConflict());
    }

    @Test
    void moveChecksTargetHall() throws Exception {
        Scenario s = scenario();
        long closedHall = createAs(ADMIN, "/api/exhibition-halls", "{\"name\": \"Закрытый\", \"area\": 100, \"available\": false}");
        long tinyHall = createAs(ADMIN, "/api/exhibition-halls", "{\"name\": \"Крошечный\", \"area\": 1}");
        String url = "/api/exhibit-placements/" + s.placementId + "/move";

        postAs(s.curator, url, "{\"hallId\": " + closedHall + "}").andExpect(status().isConflict());
        postAs(s.curator, url, "{\"hallId\": " + tinyHall + "}").andExpect(status().isConflict());
        postAs(s.curator, url, "{\"hallId\": " + s.hallId + "}").andExpect(status().isBadRequest());
        postAs(s.curator, "/api/exhibit-placements/" + s.placementId + "/mount", "").andExpect(status().isOk());
        postAs(s.curator, url, "{\"hallId\": " + s.otherHallId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hallId").value(s.otherHallId))
                .andExpect(jsonPath("$.mountingStatus").value("PLANNED"));
    }

    @Test
    void hallCapacityAccountsForOverlappingExhibitions() throws Exception {
        long hall = createAs(ADMIN, "/api/exhibition-halls", "{\"name\": \"Малый\", \"area\": 10}");
        long big = createAs(ADMIN, "/api/exhibits", "{\"name\": \"Скульптура\", \"condition\": \"GOOD\", \"requiredArea\": 8}");
        long small = createAs(ADMIN, "/api/exhibits", "{\"name\": \"Монета\", \"condition\": \"GOOD\", \"requiredArea\": 3}");
        long first = createAs(ADMIN, "/api/exhibitions", exhibitionJson("Первая", TODAY, TODAY.plusDays(30), null));
        long overlapping = createAs(ADMIN, "/api/exhibitions", exhibitionJson("Вторая", TODAY.plusDays(10), TODAY.plusDays(40), null));
        long later = createAs(ADMIN, "/api/exhibitions", exhibitionJson("Третья", TODAY.plusDays(31), TODAY.plusDays(60), null));

        createAs(ADMIN, "/api/exhibit-placements", placementJson(big, first, hall));
        postAs(ADMIN, "/api/exhibit-placements", placementJson(small, overlapping, hall)).andExpect(status().isConflict());
        createAs(ADMIN, "/api/exhibit-placements", placementJson(small, later, hall));
        patchAs(ADMIN, "/api/exhibitions/" + later, "{\"startDate\": \"" + TODAY.plusDays(20) + "\"}")
                .andExpect(status().isConflict());
        patchAs(ADMIN, "/api/exhibition-halls/" + hall, "{\"area\": 5}").andExpect(status().isConflict());
    }

    @Test
    void deletingExhibitionRemovesPlacements() throws Exception {
        Scenario s = scenario();
        deleteAs(ADMIN, "/api/exhibitions/" + s.exhibitionId).andExpect(status().isNoContent());
        getAs(ADMIN, "/api/exhibit-placements/" + s.placementId).andExpect(status().isNotFound());
        getAs(ADMIN, "/api/exhibits/" + s.exhibitId).andExpect(status().isOk());
    }

    private record Scenario(Creds curator, long curatorId, long exhibitId, long hallId, long otherHallId,
                            long exhibitionId, long otherExhibitionId, long placementId) {
    }

    /** Куратор создаёт экспонат, две свои выставки и размещение; залы создаёт администратор. */
    private Scenario scenario() throws Exception {
        Creds curator = createUser("curator", "CURATOR");
        long curatorId = idOf(curator);
        long hall = createAs(ADMIN, "/api/exhibition-halls", "{\"name\": \"Зал А\", \"area\": 50}");
        long otherHall = createAs(ADMIN, "/api/exhibition-halls", "{\"name\": \"Зал Б\", \"area\": 50}");
        long exhibit = createAs(curator, "/api/exhibits", "{\"name\": \"Картина\", \"condition\": \"GOOD\", \"requiredArea\": 3}");
        long exhibition = createAs(curator, "/api/exhibitions", exhibitionJson("Живопись", TODAY, TODAY.plusDays(30), null));
        long otherExhibition = createAs(curator, "/api/exhibitions",
                exhibitionJson("Графика", TODAY.plusDays(60), TODAY.plusDays(90), null));
        long placement = createAs(curator, "/api/exhibit-placements", placementJson(exhibit, exhibition, hall));
        return new Scenario(curator, curatorId, exhibit, hall, otherHall, exhibition, otherExhibition, placement);
    }

    static String exhibitionJson(String title, LocalDate start, LocalDate end, Long curatorId) {
        return "{\"title\": \"%s\", \"startDate\": \"%s\", \"endDate\": \"%s\", \"curatorId\": %s}"
                .formatted(title, start, end, curatorId);
    }

    static String placementJson(long exhibitId, long exhibitionId, long hallId) {
        return "{\"exhibitId\": %d, \"exhibitionId\": %d, \"hallId\": %d}".formatted(exhibitId, exhibitionId, hallId);
    }
}
