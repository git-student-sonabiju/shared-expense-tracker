package com.sharedexpenses.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AuthAndAccessIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void registerLoginMeAndLogout() throws Exception {
        String username = uniqueUsername();
        register(username, "Sona");

        String token = login(username, "password123");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.displayName").value("Sona"));

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/groups").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usernamesAreCaseInsensitiveAndUnique() throws Exception {
        String username = uniqueUsername();
        register(username, "First");
        mvc.perform(json(post("/api/auth/register"), """
                        {"username": "%s", "displayName": "Second", "password": "password123"}
                        """.formatted(username.toUpperCase())))
                .andExpect(status().isConflict());
        login(username.toUpperCase(), "password123");
    }

    @Test
    void rejectsBadCredentialsAndMissingToken() throws Exception {
        String username = uniqueUsername();
        register(username, "Someone");

        mvc.perform(json(post("/api/auth/login"), """
                        {"username": "%s", "password": "wrong-password"}
                        """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        mvc.perform(json(post("/api/auth/login"), """
                        {"username": "nobody-here", "password": "password123"}
                        """))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/groups")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/groups").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWeakRegistration() throws Exception {
        mvc.perform(json(post("/api/auth/register"), """
                        {"username": " ", "displayName": "", "password": "short"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
    }

    @Test
    void groupsAreVisibleOnlyToTheirOwner() throws Exception {
        String ownerToken = registerAndLogin();
        String otherToken = registerAndLogin();

        JsonNode group = json.readTree(mvc.perform(auth(json(post("/api/groups"), """
                        {"name": "Private", "members": ["A", "B"]}
                        """), ownerToken))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long groupId = group.get("id").asLong();

        mvc.perform(auth(get("/api/groups"), otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(auth(get("/api/groups/" + groupId), otherToken)).andExpect(status().isNotFound());
        mvc.perform(auth(json(post("/api/groups/" + groupId + "/members"), "{\"name\": \"Intruder\"}"), otherToken))
                .andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/groups/" + groupId), ownerToken)).andExpect(status().isOk());
    }

    @Test
    void renameMemberUpdatesNameEverywhereAndRejectsDuplicates() throws Exception {
        String token = registerAndLogin();
        JsonNode group = json.readTree(mvc.perform(auth(json(post("/api/groups"), """
                        {"name": "Flat", "members": ["Asha", "Ben"]}
                        """), token))
                .andReturn().getResponse().getContentAsString());
        long groupId = group.get("id").asLong();
        long asha = group.get("members").get(0).get("id").asLong();
        long ben = group.get("members").get(1).get("id").asLong();

        mvc.perform(auth(json(post("/api/groups/" + groupId + "/expenses"), """
                        {"description": "Rent", "amount": 100, "paidByMemberId": %d, "splitType": "EQUAL",
                         "splits": [{"memberId": %d}, {"memberId": %d}]}
                        """.formatted(asha, asha, ben)), token))
                .andExpect(status().isCreated());

        mvc.perform(auth(json(patch("/api/groups/" + groupId + "/members/" + asha), "{\"name\": \" Asha K \"}"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Asha K"));
        mvc.perform(auth(get("/api/groups/" + groupId + "/expenses"), token))
                .andExpect(jsonPath("$[0].paidBy.name").value("Asha K"));

        mvc.perform(auth(json(patch("/api/groups/" + groupId + "/members/" + asha), "{\"name\": \"ben\"}"), token))
                .andExpect(status().isConflict());
        mvc.perform(auth(json(patch("/api/groups/" + groupId + "/members/" + ben), "{\"name\": \"BEN\"}"), token))
                .andExpect(status().isOk());
        mvc.perform(auth(json(patch("/api/groups/" + groupId + "/members/" + ben), "{\"name\": \"\"}"), token))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(json(patch("/api/groups/" + groupId + "/members/999999"), "{\"name\": \"X\"}"), token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteGroupRemovesEverythingAndOnlyOwnerCanDelete() throws Exception {
        String ownerToken = registerAndLogin();
        String otherToken = registerAndLogin();
        JsonNode group = json.readTree(mvc.perform(auth(json(post("/api/groups"), """
                        {"name": "Trip", "members": ["A", "B"]}
                        """), ownerToken))
                .andReturn().getResponse().getContentAsString());
        long groupId = group.get("id").asLong();
        long a = group.get("members").get(0).get("id").asLong();
        long b = group.get("members").get(1).get("id").asLong();
        mvc.perform(auth(json(post("/api/groups/" + groupId + "/expenses"), """
                        {"description": "Fuel", "amount": 50, "paidByMemberId": %d, "splitType": "EQUAL",
                         "splits": [{"memberId": %d}, {"memberId": %d}]}
                        """.formatted(a, a, b)), ownerToken))
                .andExpect(status().isCreated());
        mvc.perform(auth(json(post("/api/groups/" + groupId + "/settlements"), """
                        {"fromMemberId": %d, "toMemberId": %d, "amount": 25}
                        """.formatted(b, a)), ownerToken))
                .andExpect(status().isCreated());

        mvc.perform(auth(delete("/api/groups/" + groupId), otherToken)).andExpect(status().isNotFound());

        mvc.perform(auth(delete("/api/groups/" + groupId), ownerToken)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/groups/" + groupId), ownerToken)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/groups"), ownerToken)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(auth(delete("/api/groups/" + groupId), ownerToken)).andExpect(status().isNotFound());
    }

    private String registerAndLogin() throws Exception {
        String username = uniqueUsername();
        register(username, "User");
        return login(username, "password123");
    }

    private void register(String username, String displayName) throws Exception {
        mvc.perform(json(post("/api/auth/register"), """
                        {"username": "%s", "displayName": "%s", "password": "password123"}
                        """.formatted(username, displayName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(json(post("/api/auth/login"), """
                        {"username": "%s", "password": "%s"}
                        """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String content) {
        return request.contentType(MediaType.APPLICATION_JSON).content(content);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private static String uniqueUsername() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
