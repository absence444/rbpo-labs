package ru.rbpo.lab1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RbpoLab1ApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void helloReturnsText() throws Exception {
        mockMvc.perform(get("/api/hello").param("name", "RBPO"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("Hello, RBPO!")));
    }

    @Test
    void squaresReturnsNumbers() throws Exception {
        mockMvc.perform(get("/api/squares").param("count", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1))
                .andExpect(jsonPath("$[1]").value(4))
                .andExpect(jsonPath("$[2]").value(9));
    }
}
