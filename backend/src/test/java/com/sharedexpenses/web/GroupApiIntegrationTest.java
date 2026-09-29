package com.sharedexpenses.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GroupApiIntegrationTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    long groupId;
    long alice;
    long bob;
    long carol;

    /** Registers a fresh user and makes every request in the test carry that user's token. */
    @BeforeEach
    void createGroup() throws Exception {
        MockMvc anonymous = MockMvcBuilders.webAppContextSetup(context).build();
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String session = anonymous.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"displayName\": \"Tester\", \"password\": \"password123\"}"
                                .formatted(username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(session).get("token").asText();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .defaultRequest(get("/").header("Authorization", "Bearer " + token))
                .build();

        JsonNode group = body(post("/api/groups", """
                {"name": "Trip", "members": ["Alice", "Bob", "Carol"]}
                """).andExpect(status().isCreated()));
        groupId = group.get("id").asLong();
        alice = group.get("members").get(0).get("id").asLong();
        bob = group.get("members").get(1).get("id").asLong();
        carol = group.get("members").get(2).get("id").asLong();
    }

    @Test
    void equalSplitDistributesCentsAndBalancesSumToZero() throws Exception {
        post("/api/groups/" + groupId + "/expenses", """
                {"description": "Dinner", "amount": 100.00, "paidByMemberId": %d, "splitType": "EQUAL",
                 "splits": [{"memberId": %d}, {"memberId": %d}, {"memberId": %d}]}
                """.formatted(alice, alice, bob, carol))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shares[*].amount", contains(33.34, 33.33, 33.33)));

        JsonNode balances = body(mvc.perform(get("/api/groups/" + groupId + "/balances")).andExpect(status().isOk()));
        assertThat(balanceOf(balances, alice)).isEqualByComparingTo("66.66");
        assertThat(balanceOf(balances, bob)).isEqualByComparingTo("-33.33");
        assertThat(balanceOf(balances, carol)).isEqualByComparingTo("-33.33");
        assertThat(sumOfBalances(balances)).isEqualByComparingTo("0");
        assertThat(balances.get("suggestedPayments")).hasSize(2);
    }

    @Test
    void settlementUpdatesBalancesAndSuggestions() throws Exception {
        post("/api/groups/" + groupId + "/expenses", """
                {"description": "Hotel", "amount": "90", "paidByMemberId": %d, "splitType": "EXACT",
                 "splits": [{"memberId": %d, "amount": 30}, {"memberId": %d, "amount": "60.00"}]}
                """.formatted(bob, bob, carol)).andExpect(status().isCreated());

        mvc.perform(get("/api/groups/" + groupId + "/balances"))
                .andExpect(jsonPath("$.suggestedPayments", hasSize(1)))
                .andExpect(jsonPath("$.suggestedPayments[0].fromMemberId").value(carol))
                .andExpect(jsonPath("$.suggestedPayments[0].toMemberId").value(bob))
                .andExpect(jsonPath("$.suggestedPayments[0].amount").value(60.00));

        post("/api/groups/" + groupId + "/settlements", """
                {"fromMemberId": %d, "toMemberId": %d, "amount": 60}
                """.formatted(carol, bob)).andExpect(status().isCreated());

        JsonNode balances = body(get("/api/groups/" + groupId + "/balances"));
        assertThat(balances.get("suggestedPayments")).isEmpty();
        for (JsonNode b : balances.get("balances")) {
            assertThat(b.get("balance").decimalValue()).isEqualByComparingTo("0");
        }
    }

    @Test
    void rejectsExactSplitThatDoesNotAddUp() throws Exception {
        post("/api/groups/" + groupId + "/expenses", """
                {"description": "Taxi", "amount": 50, "paidByMemberId": %d, "splitType": "EXACT",
                 "splits": [{"memberId": %d, "amount": 20}, {"memberId": %d, "amount": 20}]}
                """.formatted(alice, alice, bob))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must add up to the expense amount 50.00")));
    }

    @Test
    void rejectsInvalidExpenseFields() throws Exception {
        post("/api/groups/" + groupId + "/expenses", """
                {"description": "", "amount": 10.555, "paidByMemberId": %d, "splitType": "EQUAL", "splits": []}
                """.formatted(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("description", "amount", "splits")));
    }

    @Test
    void rejectsNegativeAmountUnknownSplitTypeAndOutsiders() throws Exception {
        post("/api/groups/" + groupId + "/expenses", """
                {"description": "X", "amount": -5, "paidByMemberId": %d, "splitType": "EQUAL", "splits": [{"memberId": %d}]}
                """.formatted(alice, alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));

        post("/api/groups/" + groupId + "/expenses", """
                {"description": "X", "amount": 5, "paidByMemberId": %d, "splitType": "PERCENT", "splits": [{"memberId": %d}]}
                """.formatted(alice, alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("splitType must be one of [EQUAL, EXACT]")));

        post("/api/groups/" + groupId + "/expenses", """
                {"description": "X", "amount": 5, "paidByMemberId": 999999, "splitType": "EQUAL", "splits": [{"memberId": %d}]}
                """.formatted(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("paidByMemberId must be a member of this group"));
    }

    @Test
    void rejectsSelfSettlementAndDuplicateMember() throws Exception {
        post("/api/groups/" + groupId + "/settlements", """
                {"fromMemberId": %d, "toMemberId": %d, "amount": 10}
                """.formatted(alice, alice))
                .andExpect(status().isBadRequest());

        post("/api/groups/" + groupId + "/members", """
                {"name": "  alice "}
                """).andExpect(status().isConflict());
    }

    @Test
    void memberWithHistoryCannotBeRemovedButNewMemberCan() throws Exception {
        post("/api/groups/" + groupId + "/settlements", """
                {"fromMemberId": %d, "toMemberId": %d, "amount": 10}
                """.formatted(alice, bob)).andExpect(status().isCreated());
        mvc.perform(delete("/api/groups/" + groupId + "/members/" + alice)).andExpect(status().isConflict());

        long dave = body(post("/api/groups/" + groupId + "/members", """
                {"name": "Dave"}
                """).andExpect(status().isCreated())).get("id").asLong();
        mvc.perform(delete("/api/groups/" + groupId + "/members/" + dave)).andExpect(status().isNoContent());
    }

    @Test
    void unknownGroupReturns404() throws Exception {
        mvc.perform(get("/api/groups/999999/balances"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Group 999999 not found"));
        mvc.perform(get("/api/groups/abc")).andExpect(status().isBadRequest());
    }

    private ResultActions post(String url, String content) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(content));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private JsonNode body(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return body(mvc.perform(request));
    }

    private static BigDecimal balanceOf(JsonNode balances, long memberId) {
        for (JsonNode b : balances.get("balances")) {
            if (b.get("memberId").asLong() == memberId) {
                return b.get("balance").decimalValue();
            }
        }
        throw new AssertionError("No balance for member " + memberId);
    }

    private static BigDecimal sumOfBalances(JsonNode balances) {
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode b : balances.get("balances")) {
            sum = sum.add(b.get("balance").decimalValue());
        }
        return sum;
    }
}
