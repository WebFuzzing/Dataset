package com.example.arimaabackend.playwright.mongo;

import com.example.arimaabackend.dto.UserCreateRequest;
import com.example.arimaabackend.dto.UserResponse;
import com.example.arimaabackend.dto.UserUpdateRequest;
import com.microsoft.playwright.options.RequestOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.playwright.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserMongoTest {

    private Playwright playwright;
    private Dotenv dotenv;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void AsUser_FailToCreateUser() {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        UserCreateRequest createUser = new UserCreateRequest(
                "usernametest_mongo_" + uniqueSuffix,
                "usernametest_mongo_" + uniqueSuffix + "@unknown.invalid",
                "usernametest1_userCantCreateuser");
        APIResponse response = userRequest.post("/api/mongo/users", RequestOptions.create().setData(createUser));
        assertFalse(response.ok(), "Expected 4xx status. Got: " + response.status());
        assertEquals(403, response.status());
    }

    @Test
    void AsUser_FailToDeleteUser() {
        // First find user by username to get the ID
        APIResponse responseByUsername = adminRequest.get("/api/mongo/users/by-username/bot_ShallowBlue");
        assertTrue(responseByUsername.ok());
        long targetId;
        try {
            targetId = objectMapper.readTree(responseByUsername.text()).get("id").asLong();
        } catch (Exception e) {
            fail(e.getMessage());
            return;
        }

        APIResponse getFirstUserResponse = adminRequest.get("/api/mongo/users/" + targetId);
        assertTrue(getFirstUserResponse.ok(), "Expected 2xx status. Got: " + getFirstUserResponse.status());
        assertEquals(200, getFirstUserResponse.status());

        APIResponse responseDelete = userRequest.delete("/api/mongo/users/" + targetId);
        assertFalse(responseDelete.ok(), "Expected 4xx status. Got: " + responseDelete.status());
        assertEquals(403, responseDelete.status());
    }

    @Test
    void AsAdmin_CreateThenDeleteUser() {
        // Create user (to delete later)
        String username = "AsAdmin_CreateThenDeleteUser";
        UserCreateRequest createUser = new UserCreateRequest(
                username,
                "usernametest_" + username + "@unknown.invalid",
                "username_123_" + username);
        APIResponse response = adminRequest.post("/api/mongo/users", RequestOptions.create().setData(createUser));
        assertTrue(response.ok(), "Expected 2xx status. Got: " + response.status());
        assertEquals(201, response.status());
        JsonNode json;
        try {
            json = objectMapper.readTree(response.text());
        } catch (JsonProcessingException e) {
            fail("Invalid JSON response: " + e.getMessage());
            return;
        }
        Integer newUserId = json.path("id").asInt();
        String newUserName = json.path("username").asText();
        assertEquals(username, newUserName);

        // get user by newUserId
        APIResponse getNewUserResponse = adminRequest.get("/api/mongo/users/" + newUserId);
        assertTrue(getNewUserResponse.ok(), "Expected 2xx status. Got: " + getNewUserResponse.status());
        assertEquals(200, getNewUserResponse.status());

        // delete the user
        APIResponse responseDelete = adminRequest.delete("/api/mongo/users/" + newUserId);
        assertTrue(responseDelete.ok(), "Expected 2xx status. Got: " + responseDelete.status());
        assertEquals(204, responseDelete.status());

        // try to find the deleted user again
        APIResponse responsegetdeleted = adminRequest.get("/api/mongo/users/" + newUserId);
        assertEquals(404, responsegetdeleted.status(),
                "Expected 404 Not Found after user deletion, but got: " + responsegetdeleted.status());
    }


    @Test
    void AsAdmin_GetUserById() {
        Long userId = 1L;

        APIResponse response = adminRequest.get("/api/mongo/users/" + userId);
        assertTrue(response.ok(), "Expected 2xx status. Got: " + response.status());
        assertEquals(200, response.status());
        String jsonString = response.text();

        JsonNode json;
        try {
            json = objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            fail("Invalid JSON response: " + e.getMessage());
            return;
        }
        assertEquals(userId, json.get("id").asLong());
        assertEquals("bot_ShallowBlue", json.get("username").asText());
        assertEquals("bot_ShallowBlue@unknown.invalid", json.get("email").asText());
        assertEquals("PLAYER", json.get("role").asText());

        assertValidDate(json.get("createdAt").asText());
        assertValidDate(json.get("updatedAt").asText());
    }

    @Test
    void AsAdmin_FailToGetMissingUserById() {
        APIResponse response = adminRequest.get("/api/mongo/users/43543637");
        assertFalse(response.ok(), "Expected 4xx status. Got: " + response.status());
        assertEquals(404, response.status());
    }

    @Test
    void AsAdmin_UpdateUser() throws JsonProcessingException {
        // Create user (to update later)
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        UserCreateRequest createUser = new UserCreateRequest(
                "update_me_" + uniqueSuffix,
                "update_me_" + uniqueSuffix + "@example.com",
                "password123");
        APIResponse createResponse = adminRequest.post("/api/mongo/users", RequestOptions.create().setData(createUser));
        assertEquals(201, createResponse.status());
        JsonNode json = objectMapper.readTree(createResponse.text());
        Integer userId = json.get("id").asInt();

        // Update user
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "updated_" + uniqueSuffix,
                "updated_" + uniqueSuffix + "@example.com",
                "new_password123");

        APIResponse updateResponse = adminRequest.put("/api/mongo/users/" + userId, RequestOptions.create().setData(updateRequest));
        assertEquals(200, updateResponse.status());
        JsonNode updatedJson = objectMapper.readTree(updateResponse.text());
        Integer newUserID = updatedJson.get("id").asInt();
        assertEquals("updated_" + uniqueSuffix, updatedJson.get("username").asText());
        assertEquals("updated_" + uniqueSuffix + "@example.com", updatedJson.get("email").asText());

        // get user by newUserId
        APIResponse getNewUserResponse = adminRequest.get("/api/mongo/users/" + newUserID);
        assertTrue(getNewUserResponse.ok(), "Expected 2xx status. Got: " + getNewUserResponse.status());
        assertEquals(200, getNewUserResponse.status());

        // delete the user
        APIResponse responseDelete = adminRequest.delete("/api/mongo/users/" + newUserID);
        assertTrue(responseDelete.ok(), "Expected 2xx status. Got: " + responseDelete.status());
        assertEquals(204, responseDelete.status());

        // try to find the deleted user again
        APIResponse responsegetdeleted = adminRequest.get("/api/mongo/users/" + newUserID);
        assertEquals(404, responsegetdeleted.status(),
                "Expected 404 Not Found after user deletion, but got: " + responsegetdeleted.status());
    }

    @Test
    void AsUser_FailToUpdateUser() {
        // Try to update username, email and password of user 1
        // Using bot_ShallowBlue ID instead of hardcoded 1
        APIResponse responseByUsername = adminRequest.get("/api/mongo/users/by-username/bot_ShallowBlue");
        assertTrue(responseByUsername.ok());
        long targetId;
        try {
            targetId = objectMapper.readTree(responseByUsername.text()).get("id").asLong();
        } catch (Exception e) {
            fail(e.getMessage());
            return;
        }

        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "hacker",
                "hacker@example.com",
                "hacked123");

        APIResponse updateResponse = userRequest.put("/api/mongo/users/" + targetId, RequestOptions.create().setData(updateRequest));
        // The users attempt to update the user should fail with a 403 Forbidden
        assertEquals(403, updateResponse.status());
    }

    private void assertValidDate(String dateString) {
        assertDoesNotThrow(() -> Instant.parse(dateString),
                "Expected a valid date (e.g. 2026-03-04T09:04:54Z), but got: " + dateString);
    }
}
