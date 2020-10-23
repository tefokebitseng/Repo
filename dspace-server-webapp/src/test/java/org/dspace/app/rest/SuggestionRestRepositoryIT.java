/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.SuggestionMatcher.matchSuggestion;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.suggestion.SuggestionTarget;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.SuggestionTargetBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration Tests against the /api/integration/suggestions endpoint
 */
public class SuggestionRestRepositoryIT extends AbstractControllerIntegrationTest {
    private Collection colPeople;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // We turn off the authorization system in order to create the structure as
        // defined below
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        colPeople = CollectionBuilder.createCollection(context, parentCommunity).withName("People")
                .withRelationshipType("Person").build();
        context.restoreAuthSystemState();
    }

    @Test
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item itemFirst = ItemBuilder.createItem(context, colPeople).withTitle("Bollini, Andrea").build();
        SuggestionTarget targetFirstScopus = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("scopus", 3).build();
        context.restoreAuthSystemState();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestions"))
                .andExpect(status().isMethodNotAllowed());
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/integration/suggestions")).andExpect(status().isMethodNotAllowed());
        getClient().perform(get("/api/integration/suggestions")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findByTargetAndSourceTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item itemFirst = ItemBuilder.createItem(context, colPeople).withTitle("Bollini, Andrea").build();
        SuggestionTarget targetFirstReciter = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("reciter", 31).build();
        SuggestionTarget targetFirstScopus = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("scopus", 3).build();
        SuggestionTarget targetSecond = SuggestionTargetBuilder
                .createTarget(context, colPeople, "Digilio, Giuseppe", eperson).withSuggestionCount("reciter", 11)
                .build();
        context.restoreAuthSystemState();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(get("/api/integration/suggestions/search/findByTargetAndSource").param("source", "scopus")
                        .param("target", itemFirst.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestions", Matchers.contains(
                        matchSuggestion("scopus", itemFirst, "Suggestion scopus 1", "1"),
                        matchSuggestion("scopus", itemFirst, "Suggestion scopus 2", "2"),
                        matchSuggestion("scopus", itemFirst, "Suggestion scopus 3", "3"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString(
                                "/api/integration/suggestions/search/findByTargetAndSource?"),
                        Matchers.containsString("source=scopus"),
                        Matchers.containsString("target=" + itemFirst.getID().toString()))))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(3)));
        Item itemSecond = targetSecond.getTarget();
        getClient(adminToken)
                .perform(get("/api/integration/suggestions/search/findByTargetAndSource").param("source", "reciter")
                        .param("target", itemSecond.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestions", Matchers.contains(
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 1", "1"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 2", "2"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 3", "3"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 4", "4"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 5", "5"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 6", "6"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 7", "7"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 8", "8"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 9", "9"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 10", "10"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 11", "11"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString(
                                "/api/integration/suggestions/search/findByTargetAndSource?"),
                        Matchers.containsString("source=reciter"),
                        Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(11)));
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken)
                .perform(get("/api/integration/suggestions/search/findByTargetAndSource").param("source", "reciter")
                        .param("target", itemSecond.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestions", Matchers.contains(
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 1", "1"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 2", "2"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 3", "3"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 4", "4"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 5", "5"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 6", "6"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 7", "7"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 8", "8"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 9", "9"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 10", "10"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 11", "11"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString(
                                "/api/integration/suggestions/search/findByTargetAndSource?"),
                        Matchers.containsString("source=reciter"),
                        Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(11)));
    }

    @Test
    public void findByTargetAndSourcePaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();
        SuggestionTarget targetSecond = SuggestionTargetBuilder
                .createTarget(context, colPeople, "Digilio, Giuseppe", eperson).withSuggestionCount("reciter", 11)
                .build();
        context.restoreAuthSystemState();
        String adminToken = getAuthToken(admin.getEmail(), password);
        Item itemSecond = targetSecond.getTarget();
        getClient(adminToken)
                .perform(get("/api/integration/suggestions/search/findByTargetAndSource").param("source", "reciter")
                        .param("size", "5")
                        .param("target", itemSecond.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestions", Matchers.contains(
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 1", "1"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 2", "2"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 3", "3"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 4", "4"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 5", "5"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString(
                                "/api/integration/suggestions/search/findByTargetAndSource?"),
                        Matchers.containsString("source=reciter"),
                        Matchers.containsString("size=5"),
                        Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString(
                                        "/api/integration/suggestions/search/findByTargetAndSource?"),
                                Matchers.containsString("source=reciter"),
                                Matchers.containsString("page=1"),
                                Matchers.containsString("size=5"),
                                Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString(
                                        "/api/integration/suggestions/search/findByTargetAndSource?"),
                                Matchers.containsString("source=reciter"),
                                Matchers.containsString("page=2"),
                                Matchers.containsString("size=5"),
                                Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString(
                                        "/api/integration/suggestions/search/findByTargetAndSource?"),
                                Matchers.containsString("source=reciter"),
                                Matchers.containsString("page=0"),
                                Matchers.containsString("size=5"),
                                Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.prev.href").doesNotExist())
                .andExpect(jsonPath("$.page.size", is(5))).andExpect(jsonPath("$.page.totalElements", is(11)));

        getClient(adminToken)
                .perform(get("/api/integration/suggestions/search/findByTargetAndSource").param("source", "reciter")
                        .param("size", "5")
                        .param("page", "1")
                        .param("target", itemSecond.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestions", Matchers.contains(
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 6", "6"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 7", "7"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 8", "8"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 9", "9"),
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 10", "10"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString(
                                "/api/integration/suggestions/search/findByTargetAndSource?"),
                        Matchers.containsString("source=reciter"),
                        Matchers.containsString("page=1"),
                        Matchers.containsString("size=5"),
                        Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString(
                                        "/api/integration/suggestions/search/findByTargetAndSource?"),
                                Matchers.containsString("source=reciter"),
                                Matchers.containsString("page=2"),
                                Matchers.containsString("size=5"),
                                Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(
                                Matchers.containsString(
                                        "/api/integration/suggestions/search/findByTargetAndSource?"),
                                Matchers.containsString("source=reciter"),
                                Matchers.containsString("page=0"),
                                Matchers.containsString("size=5"),
                                Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString(
                                        "/api/integration/suggestions/search/findByTargetAndSource?"),
                                Matchers.containsString("source=reciter"),
                                Matchers.containsString("page=2"),
                                Matchers.containsString("size=5"),
                                Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString(
                                        "/api/integration/suggestions/search/findByTargetAndSource?"),
                                Matchers.containsString("source=reciter"),
                                Matchers.containsString("page=0"),
                                Matchers.containsString("size=5"),
                                Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$.page.size", is(5))).andExpect(jsonPath("$.page.totalElements", is(11)));

        getClient(adminToken)
                .perform(get("/api/integration/suggestions/search/findByTargetAndSource").param("source", "reciter")
                        .param("size", "5")
                        .param("page", "2")
                        .param("target", itemSecond.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestions", Matchers.contains(
                        matchSuggestion("reciter", itemSecond, "Suggestion reciter 11", "11"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString(
                                "/api/integration/suggestions/search/findByTargetAndSource?"),
                        Matchers.containsString("source=reciter"),
                        Matchers.containsString("page=2"),
                        Matchers.containsString("size=5"),
                        Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(
                                Matchers.containsString(
                                        "/api/integration/suggestions/search/findByTargetAndSource?"),
                                Matchers.containsString("source=reciter"),
                                Matchers.containsString("page=1"),
                                Matchers.containsString("size=5"),
                                Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString(
                                        "/api/integration/suggestions/search/findByTargetAndSource?"),
                                Matchers.containsString("source=reciter"),
                                Matchers.containsString("page=2"),
                                Matchers.containsString("size=5"),
                                Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString(
                                        "/api/integration/suggestions/search/findByTargetAndSource?"),
                                Matchers.containsString("source=reciter"),
                                Matchers.containsString("page=0"),
                                Matchers.containsString("size=5"),
                                Matchers.containsString("target=" + itemSecond.getID().toString()))))
                .andExpect(jsonPath("$._links.next.href").doesNotExist())
                .andExpect(jsonPath("$.page.size", is(5))).andExpect(jsonPath("$.page.totalElements", is(11)));
    }

    @Test
    public void findByTargetAndSourceNotAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item itemFirst = ItemBuilder.createItem(context, colPeople).withTitle("Bollini, Andrea").build();
        SuggestionTarget targetFirstReciter = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("reciter", 31).build();
        SuggestionTarget targetFirstScopus = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("scopus", 3).build();
        SuggestionTarget targetSecond = SuggestionTargetBuilder
                .createTarget(context, colPeople, "Digilio, Giuseppe", eperson).withSuggestionCount("reciter", 11)
                .build();
        context.restoreAuthSystemState();
        // anonymous cannot access the suggestions source endpoint
        getClient()
                .perform(get("/api/integration/suggestions/search/findByTargetAndSource")
                        .param("target", UUID.randomUUID().toString()).param("source", "reciter"))
                .andExpect(status().isUnauthorized());
        // nor normal user
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson)
                .perform(get("/api/integration/suggestions/search/findByTargetAndSource")
                        .param("target", UUID.randomUUID().toString()).param("source", "reciter"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item itemFirst = ItemBuilder.createItem(context, colPeople).withTitle("Bollini, Andrea").build();
        SuggestionTarget targetFirstReciter = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("reciter", 31).build();
        SuggestionTarget targetFirstScopus = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("scopus", 3).build();
        SuggestionTarget targetSecond = SuggestionTargetBuilder
                .createTarget(context, colPeople, "Digilio, Giuseppe", eperson).withSuggestionCount("reciter", 11)
                .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String suggestionId = "reciter:" + itemFirst.getID().toString() + ":6";
        getClient(adminToken).perform(get("/api/integration/suggestions/" + suggestionId)).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", matchSuggestion("reciter", itemFirst, "Suggestion reciter 6", "6")))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/integration/suggestions/" + suggestionId)));
        Item itemSecond = targetSecond.getTarget();
        String epersonSuggestionId = "reciter:" + itemSecond.getID().toString() + ":2";
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/integration/suggestions/" + epersonSuggestionId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", matchSuggestion("reciter", itemSecond, "Suggestion reciter 2", "2")))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/integration/suggestions/" + epersonSuggestionId)));
    }

    @Test
    public void findOneNotAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item itemFirst = ItemBuilder.createItem(context, colPeople).withTitle("Bollini, Andrea").build();
        SuggestionTarget targetFirstReciter = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("reciter", 31).build();
        SuggestionTarget targetFirstScopus = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("scopus", 3).build();
        SuggestionTarget targetSecond = SuggestionTargetBuilder
                .createTarget(context, colPeople, "Digilio, Giuseppe", eperson).withSuggestionCount("reciter", 11)
                .build();
        context.restoreAuthSystemState();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        String suggestionId = "reciter:" + itemFirst.getID().toString() + ":6";
        getClient(epersonToken).perform(get("/api/integration/suggestions/" + suggestionId))
                .andExpect(status().isForbidden());
        getClient(epersonToken).perform(get("/api/integration/suggestions/not-exist"))
                .andExpect(status().isForbidden());
        getClient().perform(get("/api/integration/suggestions/" + suggestionId)).andExpect(status().isUnauthorized());
        getClient().perform(get("/api/integration/suggestions/not-exist")).andExpect(status().isUnauthorized());
    }

}