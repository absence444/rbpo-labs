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

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MuseumApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exhibitCrudLifecycle() throws Exception {
        long id = create("/api/exhibits", """
                {"name": "Ваза эпохи Мин", "condition": "GOOD", "requiredArea": 2.5}
                """);

        mockMvc.perform(get("/api/exhibits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/exhibits/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ваза эпохи Мин"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
        mockMvc.perform(patch("/api/exhibits/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"condition\": \"NEEDS_RESTORATION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("NEEDS_RESTORATION"))
                .andExpect(jsonPath("$.requiredArea").value(2.5));
        mockMvc.perform(delete("/api/exhibits/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/exhibits/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void serverSetsTimestampsAndIgnoresClientValues() throws Exception {
        mockMvc.perform(post("/api/exhibition-halls").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Зал 1", "area": 100, "createdAt": "2000-01-01T00:00:00Z", "id": 999}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.not("2000-01-01T00:00:00Z")));
    }

    @Test
    void validationErrorsReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/exhibits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"condition\": \"GOOD\", \"requiredArea\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.requiredArea", notNullValue()));
        mockMvc.perform(post("/api/exhibitions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"X\", \"startDate\": \"2026-12-10\", \"endDate\": \"2026-12-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingUserClearsCuratorReference() throws Exception {
        long userId = create("/api/users", """
                {"username": "curator", "fullName": "Иван Иванов", "email": "ivan@museum.ru"}
                """);
        long exhibitionId = create("/api/exhibitions", """
                {"title": "Импрессионисты", "startDate": "2026-11-01", "endDate": "2027-01-31", "curatorId": %d}
                """.formatted(userId));

        mockMvc.perform(delete("/api/users/{id}", userId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/exhibitions/{id}", exhibitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curatorId", nullValue()));
    }

    @Test
    void deletingRelatedEntityRemovesPlacements() throws Exception {
        long exhibitId = create("/api/exhibits", "{\"name\": \"Картина\", \"condition\": \"GOOD\", \"requiredArea\": 3}");
        long exhibitionId = create("/api/exhibitions",
                "{\"title\": \"Живопись\", \"startDate\": \"2026-11-01\", \"endDate\": \"2026-12-01\"}");
        long hallId = create("/api/exhibition-halls", "{\"name\": \"Зал А\", \"area\": 50}");
        long placementId = create("/api/exhibit-placements", """
                {"exhibitId": %d, "exhibitionId": %d, "hallId": %d}
                """.formatted(exhibitId, exhibitionId, hallId));

        mockMvc.perform(get("/api/exhibit-placements/{id}", placementId))
                .andExpect(jsonPath("$.mountingStatus").value("PLANNED"));
        mockMvc.perform(patch("/api/exhibit-placements/{id}", placementId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mountingStatus\": \"MOUNTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mountingStatus").value("MOUNTED"));

        mockMvc.perform(delete("/api/exhibition-halls/{id}", hallId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/exhibit-placements/{id}", placementId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/exhibit-placements")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void placementRespectsHallCapacityAndAvailability() throws Exception {
        long bigExhibit = create("/api/exhibits", "{\"name\": \"Скульптура\", \"condition\": \"GOOD\", \"requiredArea\": 8}");
        long smallExhibit = create("/api/exhibits", "{\"name\": \"Монета\", \"condition\": \"GOOD\", \"requiredArea\": 1}");
        long exhibitionId = create("/api/exhibitions",
                "{\"title\": \"Античность\", \"startDate\": \"2026-11-01\", \"endDate\": \"2026-12-01\"}");
        long hallId = create("/api/exhibition-halls", "{\"name\": \"Малый зал\", \"area\": 8.5}");
        long closedHallId = create("/api/exhibition-halls", "{\"name\": \"Закрытый зал\", \"area\": 100, \"available\": false}");

        create("/api/exhibit-placements", "{\"exhibitId\": %d, \"exhibitionId\": %d, \"hallId\": %d}"
                .formatted(bigExhibit, exhibitionId, hallId));

        mockMvc.perform(post("/api/exhibit-placements").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exhibitId\": %d, \"exhibitionId\": %d, \"hallId\": %d}"
                                .formatted(smallExhibit, exhibitionId, hallId)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/exhibit-placements").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exhibitId\": %d, \"exhibitionId\": %d, \"hallId\": %d}"
                                .formatted(smallExhibit, exhibitionId, closedHallId)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/exhibit-placements").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exhibitId\": %d, \"exhibitionId\": %d, \"hallId\": 777}"
                                .formatted(smallExhibit, exhibitionId)))
                .andExpect(status().isNotFound());
    }

    private long create(String url, String json) throws Exception {
        String body = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
