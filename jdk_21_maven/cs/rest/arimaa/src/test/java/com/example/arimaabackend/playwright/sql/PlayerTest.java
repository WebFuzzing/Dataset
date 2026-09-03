package com.example.arimaabackend.playwright.sql;

import com.example.arimaabackend.dto.PlayerCreateRequest;
import com.example.arimaabackend.dto.PlayerUpdateRequest;
import com.example.arimaabackend.dto.UserCreateRequest;
import com.example.arimaabackend.dto.UserUpdateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;
import java.util.Base64;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlayerTest {

    private static final Integer BOT_SHALLOWBLUE_ID = 371;

    private Playwright playwright;
    private Dotenv dotenv;
    private ObjectMapper objectMapper = new ObjectMapper();

    private APIRequestContext adminRequest;
    private APIRequestContext userRequest;

    @BeforeAll
    void setup() {
        // use the .env file in the root directory
        dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        playwright = Playwright.create();

        String adminAuth = Base64.getEncoder().encodeToString(
                (dotenv.get("DEFAULT_ADMIN_USERNAME", "admin1") + ":" +
                 dotenv.get("DEFAULT_ADMIN_PASSWORD", "password")).getBytes());

        String userAuth = Base64.getEncoder().encodeToString(
                (dotenv.get("DEFAULT_USER_USERNAME", "user1") + ":" +
                 dotenv.get("DEFAULT_USER_PASSWORD", "password")).getBytes());

        adminRequest = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL("http://localhost:8080")
                        .setExtraHTTPHeaders(Map.of(
                                "Accept", "application/json",
                                "Content-Type", "application/json",
                                "Authorization", "Basic " + adminAuth))
        );

        userRequest = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL("http://localhost:8080")
                        .setExtraHTTPHeaders(Map.of(
                                "Accept", "application/json",
                                "Content-Type", "application/json",
                                "Authorization", "Basic " + userAuth))
        );

    }

    @AfterAll
    void teardown() {
        if (adminRequest != null) adminRequest.dispose();
        if (userRequest != null) userRequest.dispose();
        if (playwright != null) playwright.close();
    }

    @Test
    void ASAdmin_CreateThenDeletePlayer() throws JsonProcessingException {
        // Create user first (add player later)
        String testUsername = "player_test_" + System.currentTimeMillis();
        UserCreateRequest userRequestData = new UserCreateRequest(
                testUsername,
                testUsername + "@example.com",
                "password123"
        );
        APIResponse userCreateResponse = adminRequest.post("/api/users", RequestOptions.create().setData(userRequestData));
        assertEquals(201, userCreateResponse.status());
        JsonNode userJson = objectMapper.readTree(userCreateResponse.text());
        long userId = userJson.get("id").asLong();

        try {
            // Create a Player
            PlayerCreateRequest playerRequestData = new PlayerCreateRequest(userId, 1500, 100, 5, 1, null);
            APIResponse playerCreateResponse = adminRequest.post("/api/players", RequestOptions.create().setData(playerRequestData));
            assertEquals(201, playerCreateResponse.status(), "Failed to create player: " + playerCreateResponse.text());
            
            JsonNode playerJson = objectMapper.readTree(playerCreateResponse.text());
            int playerId = playerJson.get("id").asInt();
            assertEquals(testUsername, playerJson.get("username").asText());

            // Get the Player
            APIResponse getResponse = adminRequest.get("/api/players/" + playerId);
            assertEquals(200, getResponse.status());
            JsonNode getJson = objectMapper.readTree(getResponse.text());
            assertEquals(playerId, getJson.get("id").asInt());

            // Get the user
            APIResponse getUserResponse = adminRequest.get("/api/users/" + userId);
            assertEquals(200, getUserResponse.status());
            JsonNode getUserJson = objectMapper.readTree(getUserResponse.text());
            assertEquals(userId, getUserJson.get("id").asInt());

            // Delete the Player
            APIResponse deleteResponse = adminRequest.delete("/api/players/" + playerId);
            System.out.println("[DEBUG_LOG] Delete response status: " + deleteResponse.status());
            assertEquals(204, deleteResponse.status());

            // Verify Player is gone
            APIResponse getAfterDeleteResponse = adminRequest.get("/api/players/" + playerId);
            assertEquals(404, getAfterDeleteResponse.status());

            //verify user is gone
            APIResponse getAfterDeleteUserResponse = adminRequest.get("/api/users/" + userId);
            assertEquals(404, getAfterDeleteResponse.status());

        } finally {
            // Cleanup: delete user (which might also cascade delete player if we didn't do it)
            adminRequest.delete("/api/users/" + userId);
        }
    }

    @Test
    void AsAdmin_FailToCreatePlayerWithoutUserObj() {
        PlayerCreateRequest playerRequestData = new PlayerCreateRequest(null, 1500, 100, 5, 1, null);
        APIResponse playerCreateResponse = adminRequest.post("/api/players", RequestOptions.create().setData(playerRequestData));
        assertEquals(400, playerCreateResponse.status(), "Expected status 400 but got: " + playerCreateResponse.text());
    }


    @Test
    void AsUser_FailToCreatePlayerWithoutUser() {
        PlayerCreateRequest playerRequestData = new PlayerCreateRequest(null, 1500, 100, 5, 1, null);
        APIResponse playerCreateResponse = userRequest.post("/api/players", RequestOptions.create().setData(playerRequestData));
        assertEquals(400, playerCreateResponse.status(), "Expected status 400 but got: " + playerCreateResponse.text());
    }


    @Test
    void AsAdmin_FailToDeletePlayerWithMatches() throws JsonProcessingException {
        APIResponse getResponse = adminRequest.get("/api/players/" + BOT_SHALLOWBLUE_ID);
        assertEquals(200, getResponse.status());
        JsonNode getJson = objectMapper.readTree(getResponse.text());
        assertEquals(BOT_SHALLOWBLUE_ID, getJson.get("id").asInt());

        APIResponse deleteResponse = adminRequest.delete("/api/players/" + BOT_SHALLOWBLUE_ID);
        System.out.println("[DEBUG_LOG] Delete response status: " + deleteResponse.status());
        assertEquals(400, deleteResponse.status());
    }

    @Test
    void AsUser_FailToDeletePlayerWithMatches() throws JsonProcessingException {
        APIResponse getResponse = userRequest.get("/api/players/" + BOT_SHALLOWBLUE_ID);
        assertEquals(200, getResponse.status());
        JsonNode getJson = objectMapper.readTree(getResponse.text());
        assertEquals(BOT_SHALLOWBLUE_ID, getJson.get("id").asInt());

        APIResponse deleteResponse = userRequest.delete("/api/players/" + BOT_SHALLOWBLUE_ID);
        System.out.println("[DEBUG_LOG] Delete response status: " + deleteResponse.status());
        assertEquals(403, deleteResponse.status());
    }

    @Test
    void deleteById_returns404_whenNotFound() {
        APIResponse response = adminRequest.delete("/api/players/999999");
        assertEquals(404, response.status());
    }

    @Test
    void AsAdmin_UpdatePlayer() throws JsonProcessingException {
        // Create user first (add player later)
        String testUsername = "player_test_" + System.currentTimeMillis();
        UserCreateRequest userRequestData = new UserCreateRequest(
                testUsername,
                testUsername + "@example.com",
                "password123"
        );
        APIResponse userCreateResponse = adminRequest.post("/api/users", RequestOptions.create().setData(userRequestData));
        assertEquals(201, userCreateResponse.status());
        JsonNode userJson = objectMapper.readTree(userCreateResponse.text());
        long userId = userJson.get("id").asLong();

        try {
            // Create a Player
            PlayerCreateRequest playerRequestData = new PlayerCreateRequest(userId, 1500, 100, 5, 1, null);
            APIResponse playerCreateResponse = adminRequest.post("/api/players", RequestOptions.create().setData(playerRequestData));
            assertEquals(201, playerCreateResponse.status(), "Failed to create player: " + playerCreateResponse.text());

            JsonNode playerJson = objectMapper.readTree(playerCreateResponse.text());
            int playerId = playerJson.get("id").asInt();
            assertEquals(testUsername, playerJson.get("username").asText());

            // Get the Player
            APIResponse getResponse = adminRequest.get("/api/players/" + playerId);
            assertEquals(200, getResponse.status());
            JsonNode getJson = objectMapper.readTree(getResponse.text());
            assertEquals(playerId, getJson.get("id").asInt());

            // Get the user
            APIResponse getUserResponse = adminRequest.get("/api/users/" + userId);
            assertEquals(200, getUserResponse.status());
            JsonNode getUserJson = objectMapper.readTree(getUserResponse.text());
            assertEquals(userId, getUserJson.get("id").asInt());

            // Update player and their user information
            UserUpdateRequest userUpdate = new UserUpdateRequest(
                    "updated_player",
                    "updated_player" + "@example.com",
                    "new_password123");
            PlayerUpdateRequest updateRequest = new PlayerUpdateRequest(1500, 10, 5, 2, userUpdate); // Update rating to 1500, country to 2

            APIResponse updateResponse = adminRequest.put("/api/players/" + playerId, RequestOptions.create().setData(updateRequest));
            assertEquals(200, updateResponse.status());
            JsonNode updatedJson = objectMapper.readTree(updateResponse.text());

            assertEquals(1500, updatedJson.get("rating").asInt());
            assertEquals(10, updatedJson.get("ru").asInt());
            assertEquals(5, updatedJson.get("gamesPlayed").asInt());
            assertEquals(2, updatedJson.get("countryId").asInt());
            assertEquals("updated_player", updatedJson.get("username").asText());
            assertEquals("updated_player" + "@example.com", updatedJson.get("email").asText());

            // Verify user was updated too via GET /api/users/{id}
            APIResponse updatedUserResponse = adminRequest.get("/api/users/" + userId);
            assertEquals(200, updatedUserResponse.status());
            JsonNode updatedUserJson = objectMapper.readTree(updatedUserResponse.text());
            assertEquals("updated_player", updatedUserJson.get("username").asText());
            assertEquals("updated_player" + "@example.com", updatedUserJson.get("email").asText());

            // Delete the Player
            APIResponse deleteResponse = adminRequest.delete("/api/players/" + playerId);
            System.out.println("[DEBUG_LOG] Delete response status: " + deleteResponse.status());
            assertEquals(204, deleteResponse.status());

            // Verify Player is gone
            APIResponse getAfterDeleteResponse = adminRequest.get("/api/players/" + playerId);
            assertEquals(404, getAfterDeleteResponse.status());

            //verify user is gone
            APIResponse getAfterDeleteUserResponse = adminRequest.get("/api/users/" + userId);
            assertEquals(404, getAfterDeleteResponse.status());

        } finally {
            // Cleanup: delete user (which might also cascade delete player if we didn't do it)
            adminRequest.delete("/api/users/" + userId);
        }
    }
    @Test
    void AsUser_FailToUpdatePlayer() {
        PlayerUpdateRequest updateRequest = new PlayerUpdateRequest(2000, 100, 50, 1, null);
        APIResponse response = userRequest.put("/api/players/1", RequestOptions.create().setData(updateRequest));
        assertEquals(403, response.status());
    }
}
