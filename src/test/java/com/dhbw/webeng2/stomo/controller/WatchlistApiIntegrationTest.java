package com.dhbw.webeng2.stomo.controller;

import com.dhbw.webeng2.stomo.service.PriceHistoryService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end (API) tests driving the full stack — controller, security filter, service and
 * H2 — over MockMvc. The live price source is mocked so nothing reaches the network. Each
 * test runs as a freshly-registered user authenticated with a real bearer token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WatchlistApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PriceHistoryService priceHistory;

    private String token;

    @BeforeEach
    void registerUser() throws Exception {
        when(priceHistory.getLatestPrice(anyString())).thenReturn(100.0);
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String body = """
                {"firstname":"Jane","lastname":"Doe","email":"%s","password":"password123"}
                """.formatted(email);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(response, "$.token");
    }

    @Test
    void rejectsUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/watchlists"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        String body = """
                {"firstname":"A","lastname":"B","email":"not-an-email","password":"password123"}
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void listReturnsAutoCreatedDefaultWatchlist() throws Exception {
        mockMvc.perform(get("/api/watchlists").header(AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("My Watchlist"));
    }

    @Test
    void createsAndAccumulatesWatchlists() throws Exception {
        // First GET auto-creates the default list, then we add a second.
        mockMvc.perform(get("/api/watchlists").header(AUTHORIZATION, bearer()))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(post("/api/watchlists").header(AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Tech\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tech"));

        mockMvc.perform(get("/api/watchlists").header(AUTHORIZATION, bearer()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("My Watchlist", "Tech")));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/watchlists").header(AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renamesWatchlist() throws Exception {
        long id = createWatchlist("Old");

        mockMvc.perform(put("/api/watchlists/" + id).header(AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void deletesWatchlist() throws Exception {
        long id = createWatchlist("Temp");

        mockMvc.perform(delete("/api/watchlists/" + id).header(AUTHORIZATION, bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/watchlists/" + id).header(AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void addsAndRemovesItem() throws Exception {
        long id = createWatchlist("Tech");

        String item = mockMvc.perform(post("/api/watchlists/" + id + "/items").header(AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"symbol\":\"aapl\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.startPrice").value(100.0))
                .andReturn().getResponse().getContentAsString();
        long itemId = ((Number) JsonPath.read(item, "$.id")).longValue();

        mockMvc.perform(get("/api/watchlists/" + id).header(AUTHORIZATION, bearer()))
                .andExpect(jsonPath("$.items.length()").value(1));

        mockMvc.perform(delete("/api/watchlists/" + id + "/items/" + itemId).header(AUTHORIZATION, bearer()))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsDuplicateSymbol() throws Exception {
        long id = createWatchlist("Dup");
        String addAapl = "{\"symbol\":\"aapl\"}";

        mockMvc.perform(post("/api/watchlists/" + id + "/items").header(AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(addAapl))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/watchlists/" + id + "/items").header(AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(addAapl))
                .andExpect(status().isConflict());
    }

    private long createWatchlist(String name) throws Exception {
        String response = mockMvc.perform(post("/api/watchlists").header(AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String bearer() {
        return "Bearer " + token;
    }
}
