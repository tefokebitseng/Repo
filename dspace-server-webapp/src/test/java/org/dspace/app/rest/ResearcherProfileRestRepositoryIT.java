/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.app.rest.matcher.HalMatcher.matchLinks;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataNotEmpty;
import static org.dspace.builder.RelationshipBuilder.createRelationshipBuilder;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.CLONE;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.MERGED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.ORIGINATED;
import static org.dspace.xmlworkflow.ConcytecWorkflowRelation.SHADOW_COPY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.jayway.jsonpath.JsonPath;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.ResearcherProfileRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.RelationshipService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.GroupService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link ResearcherProfileRestRepository}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class ResearcherProfileRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MetadataSchemaService metadataSchemaService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizeService authorizeService;

    private EPerson user;

    private EPerson anotherUser;

    private Collection cvPersonCollection;

    /**
     * Tests setup.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        user = EPersonBuilder.createEPerson(context)
            .withEmail("user@example.com")
            .withPassword(password)
            .build();

        anotherUser = EPersonBuilder.createEPerson(context)
            .withEmail("anotherUser@example.com")
            .withPassword(password)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        cvPersonCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Profile Collection")
            .withEntityType("CvPerson")
            .withSubmitterGroup(user)
            .build();

        configurationService.setProperty("researcher-profile.collection.uuid", cvPersonCollection.getID().toString());

        context.setCurrentUser(user);

        context.restoreAuthSystemState();

    }

    /**
     * Verify that the findById endpoint returns the own profile.
     *
     * @throws Exception
     */
    @Test
    public void testFindById() throws Exception {

        UUID id = user.getID();
        String name = user.getFullName();

        String authToken = getAuthToken(user.getEmail(), password);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, cvPersonCollection)
            .withCrisOwner(name, id.toString())
            .build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(true)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(name)));

    }

    /**
     * Verify that the an admin user can call the findById endpoint to get a
     * profile.
     *
     * @throws Exception
     */
    @Test
    public void testFindByIdWithAdmin() throws Exception {

        UUID id = user.getID();
        String name = user.getFullName();

        String authToken = getAuthToken(admin.getEmail(), password);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, cvPersonCollection)
            .withCrisOwner(name, id.toString())
            .build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(true)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(name)));

    }

    /**
     * Verify that a standard user can't access the profile of another user.
     *
     * @throws Exception
     */
    @Test
    public void testFindByIdWithoutOwnerUser() throws Exception {

        UUID id = user.getID();
        String name = user.getFullName();

        String authToken = getAuthToken(anotherUser.getEmail(), password);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, cvPersonCollection)
            .withCrisOwner(name, id.toString())
            .build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isForbidden());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isForbidden());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
            .andExpect(status().isForbidden());

    }

    /**
     * Verify that the createAndReturn endpoint create a new researcher profile.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturn() throws Exception {

        String id = user.getID().toString();
        String name = user.getName();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.sourceId", id, 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(name)));
    }

    /**
     * Verify that an admin can call the createAndReturn endpoint to store a new
     * researcher profile related to another user.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithAdmin() throws Exception {

        String id = user.getID().toString();
        String name = user.getName();

        configurationService.setProperty("researcher-profile.collection.uuid", null);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.sourceId", id, 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "CvPerson", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(name)));

        authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));
    }

    /**
     * Verify that a standard user can't call the createAndReturn endpoint to store
     * a new researcher profile related to another user.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithoutOwnUser() throws Exception {

        String authToken = getAuthToken(anotherUser.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .param("eperson", user.getID().toString())
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

    }

    /**
     * Verify that a conflict occurs if an user that have already a profile call the
     * createAndReturn endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithProfileAlreadyAssociated() throws Exception {

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")));

    }

    /**
     * Verify that an unprocessable entity status is back when the createAndReturn
     * is called to create a profile for an unknown user.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithUnknownEPerson() throws Exception {

        String unknownId = UUID.randomUUID().toString();
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .param("eperson", unknownId)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Verify that a user can delete his profile using the delete endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {

        configurationService.setProperty("researcher-profile.hard-delete.enabled", false);

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);
        AtomicReference<UUID> itemIdRef = new AtomicReference<UUID>();

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataNotEmpty("cris.owner"))))
            .andDo(result -> itemIdRef.set(fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(authToken).perform(get("/api/core/items/{id}", itemIdRef.get()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataDoesNotExist("cris.owner"))));

    }

    /**
     * Verify that a user can hard delete his profile using the delete endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testHardDelete() throws Exception {

        configurationService.setProperty("researcher-profile.hard-delete.enabled", true);

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);
        AtomicReference<UUID> itemIdRef = new AtomicReference<UUID>();

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataNotEmpty("cris.owner"))))
            .andDo(result -> itemIdRef.set(fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(authToken).perform(get("/api/core/items/{id}", itemIdRef.get()))
            .andExpect(status().isNotFound());

    }

    /**
     * Verify that an admin can delete a profile of another user using the delete
     * endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteWithAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());
    }

    /**
     * Verify that an user can delete his profile using the delete endpoint even if
     * was created by an admin.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteProfileCreatedByAnAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(adminToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(userToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

    }

    /**
     * Verify that a standard user can't call the delete endpoint to delete a
     * researcher profile related to another user.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteWithoutOwnUser() throws Exception {

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);
        String anotherUserToken = getAuthToken(anotherUser.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(anotherUserToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isForbidden());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

    }

    /**
     * Verify that an user can change the profile visibility using the patch endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibleAttribute() throws Exception {

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.visible", is(false)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        // change the visibility to false
        operations = asList(new ReplaceOperation("/visible", false));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));

    }

    /**
     * Verify that an user can not change the profile visibility of another user
     * using the patch endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibleAttributeWithoutOwnUser() throws Exception {

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);
        String anotherUserToken = getAuthToken(anotherUser.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.visible", is(false)));

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        // try to change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(anotherUserToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));
    }

    /**
     * Verify that an admin can change the profile visibility of another user using
     * the patch endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibleAttributeWithAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(adminToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));
    }

    /**
     * Verify that an user can change the visibility of his profile using the patch
     * endpoint even if was created by an admin.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibilityOfProfileCreatedByAnAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(adminToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));
    }

    /**
     * Verify that after an user login an automatic claim between the logged eperson
     * and possible profiles without eperson is done.
     *
     * @throws Exception
     */
    @Test
    public void testAutomaticProfileClaimByEmail() throws Exception {

        configurationService.setProperty("researcher-profile.hard-delete.enabled", false);

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);

        // create and delete a profile
        getClient(adminToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        String firstItemId = getItemIdByProfileId(adminToken, id);

        MetadataValueRest valueToAdd = new MetadataValueRest(user.getEmail());
        List<Operation> operations = asList(new AddOperation("/metadata/person.email", valueToAdd));

        getClient(adminToken).perform(patch(BASE_REST_SERVER_URL + "/api/core/items/{id}", firstItemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getPatchContent(operations)))
            .andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

        // the automatic claim is done after the user login
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        // the profile item should be the same
        String secondItemId = getItemIdByProfileId(adminToken, id);
        assertEquals("The item should be the same", firstItemId, secondItemId);

    }


    /**
     * Given a request containing an external reference URI, verifies that a researcherProfile is created with
     * data cloned from source object.
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testCloneFromExternalSource() throws Exception {
        // FIXME: unIgnore once orcid integration ready

        context.turnOffAuthorisationSystem();
        ItemBuilder.createItem(context, cvPersonCollection)
            .withFullName("Giuseppe Garibaldi")
            .withBirthDate("1807-07-04")
            .withOrcidIdentifier("0000-1111-2222-3333")
            .build();

        EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPerson").build();

        CrisLayoutBox publicBox = CrisLayoutBoxBuilder.createBuilder(context, entityType, false, false)
            .withSecurity(LayoutSecurity.PUBLIC).build();

        CrisLayoutBox ownerAndAdministratorBox = CrisLayoutBoxBuilder.createBuilder(context, entityType, false, false)
            .withSecurity(LayoutSecurity.OWNER_AND_ADMINISTRATOR).build();


        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("crisrp", "name", Optional.empty()),
            1, 1)
            .withBox(publicBox)
            .build();

        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("person", "birthDate", Optional.empty()),
            2, 1)
            .withBox(publicBox).build();

        CrisLayoutFieldBuilder.createMetadataField(context,
            metadataField("perucris", "identifier", Optional.of("dni")),
            1, 1)
            .withBox(ownerAndAdministratorBox).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(TEXT_URI_LIST).content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/0000-1111-2222-3333"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(user.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + user.getID(), "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", user.getID()))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", user.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", user.getName(), user.getID().toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("crisrp.name", "Giuseppe Garibaldi", 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("relationship.type", "CvPerson", 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("person.birthDate", "1807-07-04", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", user.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(user.getName())));
    }

    @Test
    public void testCloneFromExternalSourceRecordNotFound() throws Exception {

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(TEXT_URI_LIST).content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/FAKE"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testCloneFromExternalSourceMultipleUri() throws Exception {

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(TEXT_URI_LIST)
            .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/id \n " +
                "http://localhost:8080/server/api/integration/externalsources/dspace/entryValues/id"))
            .andExpect(status().isBadRequest());

    }

    @Test
    public void testCloneFromExternalProfileAlreadyAssociated() throws Exception {

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(TEXT_URI_LIST)
            .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/id"))
            .andExpect(status().isConflict());
    }

    @Test
    public void testCloneFromExternalCollectionNotSet() throws Exception {

        configurationService.setProperty("researcher-profile.collection.uuid", "not-existing");
        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(TEXT_URI_LIST)
            .content("http://localhost:8080/server/api/integration/externalsources/orcid/entryValues/id \n " +
                "http://localhost:8080/server/api/integration/externalsources/dspace/entryValues/id"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testPatchToClaimPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType personType = createEntityType("Person");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");
        EntityType cvPersonType = createEntityType("CvPerson");

        RelationshipType cvShadowCopy = createHasShadowCopyRelationship(cvPersonCloneType, personType);
        RelationshipType isCloneOf = createCloneRelationship(cvPersonCloneType, cvPersonType);
        RelationshipType isPersonOwner = createIsPersonOwnerRelationship(cvPersonType, personType);

        Collection cvPersonCloneCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Cv Person Clone Collection")
            .withEntityType("CvPersonClone")
            .build();

        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Person Collection")
            .withEntityType("Person")
            .build();

        Item person = ItemBuilder.createItem(context, personCollection)
            .withTitle("User")
            .build();

        configurationService.setProperty("claimable.entityType", "Person");
        configurationService.setProperty("cti-vitae.clone.person-collection-id",
            cvPersonCloneCollection.getID().toString());

        context.restoreAuthSystemState();

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", person.getID().toString()));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        List<Relationship> cvShadowCopyRelations = findRelations(person, cvShadowCopy);
        assertThat(cvShadowCopyRelations, hasSize(1));

        Item cvPersonCloneItem = cvShadowCopyRelations.get(0).getLeftItem();
        assertThat(cvPersonCloneItem.getOwningCollection(), equalTo(cvPersonCloneCollection));
        assertThat(cvPersonCloneItem.getMetadata(), hasItem(with("dc.title", "user@example.com")));

        List<Relationship> cloneRelations = findRelations(cvPersonCloneItem, isCloneOf);
        assertThat(cloneRelations, hasSize(1));

        UUID profileId = cloneRelations.get(0).getRightItem().getID();

        getClient(userToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(profileId.toString())));

        List<Relationship> isPersonOwnerRelations = findRelations(person, isPersonOwner);
        assertThat(isPersonOwnerRelations, hasSize(1));
        assertThat(isPersonOwnerRelations.get(0).getLeftItem().getID(), equalTo(profileId));

    }

    @Test
    public void testPatchToClaimWithAlreadyClonedPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType personType = createEntityType("Person");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");
        EntityType cvPersonType = createEntityType("CvPerson");

        createHasShadowCopyRelationship(cvPersonCloneType, personType);
        createCloneRelationship(cvPersonCloneType, cvPersonType);

        Collection cvPersonCloneCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Cv Person Clone Collection")
            .withEntityType("CvPersonClone")
            .build();

        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Person Collection")
            .withEntityType("Person")
            .build();

        Item person = ItemBuilder.createItem(context, personCollection)
            .withTitle("User")
            .build();

        configurationService.setProperty("claimable.entityType", "Person");
        configurationService.setProperty("cti-vitae.clone.person-collection-id",
            cvPersonCloneCollection.getID().toString());

        context.restoreAuthSystemState();

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", person.getID().toString()));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isConflict());

    }

    @Test
    public void testPatchToClaimEntityWithWrongType() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType personType = createEntityType("Person");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");
        EntityType cvPersonType = createEntityType("CvPerson");

        createHasShadowCopyRelationship(cvPersonCloneType, personType);
        createCloneRelationship(cvPersonCloneType, cvPersonType);

        Collection publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publication Collection")
            .withEntityType("Publication")
            .build();

        Item publication = ItemBuilder.createItem(context, publicationCollection)
            .withTitle("Publication")
            .build();

        configurationService.setProperty("claimable.entityType", "Person");

        context.restoreAuthSystemState();

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", publication.getID().toString()));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isBadRequest());

    }

    @Test
    public void testPatchToClaimPersonWithInvalidItemId() throws Exception {

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", "wrong-id"));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToClaimPersonWithUnkownItemId() throws Exception {

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", "873510e9-6d3f-4b1d-add0-bb1d7d53f07f"));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToClaimPersonRelatedToInstitutionPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType personType = createEntityType("Person");
        EntityType institutionPersonType = createEntityType("InstitutionPerson");
        EntityType cvPersonCloneType = createEntityType("CvPersonClone");
        EntityType cvPersonType = createEntityType("CvPerson");

        RelationshipType institutionShadowCopy = createHasShadowCopyRelationship(institutionPersonType, personType);
        RelationshipType cvShadowCopy = createHasShadowCopyRelationship(cvPersonCloneType, personType);
        RelationshipType isCloneOf = createCloneRelationship(cvPersonCloneType, cvPersonType);
        RelationshipType isOriginatedFrom = createIsOriginatedFromRelationship(personType, cvPersonCloneType);
        RelationshipType isMergedIn = createIsMergedInRelationship(personType);
        RelationshipType isPersonOwner = createIsPersonOwnerRelationship(cvPersonType, personType);

        Collection cvPersonCloneCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Cv Person Clone Collection")
            .withEntityType("CvPersonClone")
            .build();

        Collection personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Person Collection")
            .withEntityType("Person")
            .build();

        Collection institutionPersonCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Institution Person Collection")
            .withEntityType("InstitutionPerson")
            .build();

        Item person = ItemBuilder.createItem(context, personCollection)
            .withTitle("User")
            .build();

        Item institutionPerson = ItemBuilder.createItem(context, institutionPersonCollection)
            .withTitle("User")
            .build();

        createRelationshipBuilder(context, institutionPerson, person, institutionShadowCopy);

        configurationService.setProperty("claimable.entityType", "Person");
        configurationService.setProperty("cti-vitae.clone.person-collection-id",
            cvPersonCloneCollection.getID().toString());

        context.restoreAuthSystemState();

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new AddOperation("/claim", person.getID().toString()));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        assertThat(findRelations(person, cvShadowCopy), empty());

        List<Relationship> isMergedInRelations = findRelations(person, isMergedIn);
        assertThat(isMergedInRelations, hasSize(1));
        Item mergedInItem = isMergedInRelations.get(0).getLeftItem();
        assertThat(mergedInItem.isArchived(), is(false));
        assertThat(mergedInItem.isWithdrawn(), is(true));

        List<Relationship> isOriginatedFromRelations = findRelations(person, isOriginatedFrom);
        assertThat(isOriginatedFromRelations, hasSize(1));

        Item cvPersonCloneItem = isOriginatedFromRelations.get(0).getRightItem();
        assertThat(cvPersonCloneItem.getOwningCollection(), equalTo(cvPersonCloneCollection));
        assertThat(cvPersonCloneItem.getMetadata(), hasItem(with("dc.title", "user@example.com")));

        List<Relationship> cvShadowCopyRelations = findRelations(mergedInItem, cvShadowCopy);
        assertThat(cvShadowCopyRelations, hasSize(1));
        assertThat(cvShadowCopyRelations.get(0).getLeftItem(), equalTo(cvPersonCloneItem));

        List<Relationship> cloneRelations = findRelations(cvPersonCloneItem, isCloneOf);
        assertThat(cloneRelations, hasSize(1));

        UUID profileId = cloneRelations.get(0).getRightItem().getID();

        getClient(userToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(profileId.toString())));

        List<Relationship> isPersonOwnerRelations = findRelations(person, isPersonOwner);
        assertThat(isPersonOwnerRelations, hasSize(1));
        assertThat(isPersonOwnerRelations.get(0).getLeftItem().getID(), equalTo(profileId));

    }

    @Test
    public void researcherProfileSecurityAnonymousTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();

       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection 1")
                                          .build();

       EPerson researcher = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       ResearcherProfile researcherProfile = createProfileForUser(researcher);

       Item CvPublication = ItemBuilder.createItem(context, col1)
               .withTitle("CvPublication Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvPublication")
               .withIssueDate("2021-01-01")
               .build();

       Item CvProject = ItemBuilder.createItem(context, col1)
               .withTitle("CvProject Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvProject")
               .withIssueDate("2021-02-07")
               .build();

       Item CvPatent = ItemBuilder.createItem(context, col1)
               .withTitle("CvPatent Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withIssueDate("2020-10-11")
               .withEntityType("CvPatent")
               .build();

       context.restoreAuthSystemState();

       String researcherToken = getAuthToken(researcher.getEmail(), password);

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       List<Operation> operations = asList(new ReplaceOperation("/visible", true));

       getClient(researcherToken).perform(patch("/api/cris/profiles/" + researcher.getID())
                                 .content(getPatchContent(operations))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(true)));

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(true)));

       getClient().perform(get("/api/core/items/" + CvPublication.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

       getClient().perform(get("/api/core/items/" + CvProject.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

       getClient().perform(get("/api/core/items/" + CvPatent.getID()))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

       // hide the profile and linked CVs
       List<Operation> operations2 = asList(new ReplaceOperation("/visible", false));

       getClient(researcherToken).perform(patch("/api/cris/profiles/" + researcher.getID())
                                 .content(getPatchContent(operations2))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient().perform(get("/api/core/items/" + CvPublication.getID()))
                  .andExpect(status().isUnauthorized());

       getClient().perform(get("/api/core/items/" + CvProject.getID()))
                  .andExpect(status().isUnauthorized());

       getClient().perform(get("/api/core/items/" + CvPatent.getID()))
                  .andExpect(status().isUnauthorized());

    }

    @Test
    public void researcherProfileSecuritySimpleLoggedUserTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();

       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection 1")
                                          .build();

       EPerson researcher = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       ResearcherProfile researcherProfile = createProfileForUser(researcher);

       Item CvPublication = ItemBuilder.createItem(context, col1)
               .withTitle("CvPublication Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvPublication")
               .withIssueDate("2021-01-01")
               .build();

       Item CvProject = ItemBuilder.createItem(context, col1)
               .withTitle("CvProject Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvProject")
               .withIssueDate("2021-02-07")
               .build();

       Item CvPatent = ItemBuilder.createItem(context, col1)
               .withTitle("CvPatent Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withIssueDate("2020-10-11")
               .withEntityType("CvPatent")
               .build();

       context.restoreAuthSystemState();

       String researcherToken = getAuthToken(researcher.getEmail(), password);
       String epersonToken = getAuthToken(eperson.getEmail(), password);

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       List<Operation> operations = asList(new ReplaceOperation("/visible", true));

       getClient(researcherToken).perform(patch("/api/cris/profiles/" + researcher.getID())
                                 .content(getPatchContent(operations))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(true)));

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(true)));

       getClient(epersonToken).perform(get("/api/core/items/" + CvPublication.getID()))
                              .andExpect(status().isOk())
                              .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

       getClient(epersonToken).perform(get("/api/core/items/" + CvProject.getID()))
                              .andExpect(status().isOk())
                              .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

       getClient(epersonToken).perform(get("/api/core/items/" + CvPatent.getID()))
                              .andExpect(status().isOk())
                              .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

       // hide the profile and linked CVs
       List<Operation> operations2 = asList(new ReplaceOperation("/visible", false));

       getClient(researcherToken).perform(patch("/api/cris/profiles/" + researcher.getID())
                                 .content(getPatchContent(operations2))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient(epersonToken).perform(get("/api/core/items/" + CvPublication.getID()))
                              .andExpect(status().isForbidden());

       getClient(epersonToken).perform(get("/api/core/items/" + CvProject.getID()))
                              .andExpect(status().isForbidden());

       getClient(epersonToken).perform(get("/api/core/items/" + CvPatent.getID()))
                              .andExpect(status().isForbidden());
    }

    @Test
    public void researcherProfileSecurityTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();

       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection 1")
                                          .build();

       EPerson researcher = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       ResearcherProfile researcherProfile = createProfileForUser(researcher);

       Item CvPublication = ItemBuilder.createItem(context, col1)
               .withTitle("CvPublication Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvPublication")
               .withIssueDate("2021-01-01")
               .build();

       Item CvProject = ItemBuilder.createItem(context, col1)
               .withTitle("CvProject Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvProject")
               .withIssueDate("2021-02-07")
               .build();

       Item CvPatent = ItemBuilder.createItem(context, col1)
               .withTitle("CvPatent Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withIssueDate("2020-10-11")
               .withEntityType("CvPatent")
               .build();

       context.restoreAuthSystemState();

       String researcherToken = getAuthToken(researcher.getEmail(), password);

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       List<Operation> operations = asList(new ReplaceOperation("/visible", true));

       getClient(researcherToken).perform(patch("/api/cris/profiles/" + researcher.getID())
                                 .content(getPatchContent(operations))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(true)));

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(true)));

       getClient(researcherToken).perform(get("/api/core/items/" + CvPublication.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

       getClient(researcherToken).perform(get("/api/core/items/" + CvProject.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

       getClient(researcherToken).perform(get("/api/core/items/" + CvPatent.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

       // hide the profile and linked CVs
       List<Operation> operations2 = asList(new ReplaceOperation("/visible", false));

       getClient(researcherToken).perform(patch("/api/cris/profiles/" + researcher.getID())
                                 .content(getPatchContent(operations2))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient(researcherToken).perform(get("/api/core/items/" + CvPublication.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

       getClient(researcherToken).perform(get("/api/core/items/" + CvProject.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

       getClient(researcherToken).perform(get("/api/core/items/" + CvPatent.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

    }

    @Test
    public void researcherProfileSecurityAdminTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();

       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection 1")
                                          .build();

       EPerson researcher = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       ResearcherProfile researcherProfile = createProfileForUser(researcher);

       Item CvPublication = ItemBuilder.createItem(context, col1)
               .withTitle("CvPublication Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvPublication")
               .withIssueDate("2021-01-01")
               .build();

       Item CvProject = ItemBuilder.createItem(context, col1)
               .withTitle("CvProject Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvProject")
               .withIssueDate("2021-02-07")
               .build();

       Item CvPatent = ItemBuilder.createItem(context, col1)
               .withTitle("CvPatent Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withIssueDate("2020-10-11")
               .withEntityType("CvPatent")
               .build();

       context.restoreAuthSystemState();

       String researcherToken = getAuthToken(researcher.getEmail(), password);
       String adminToken = getAuthToken(admin.getEmail(), password);

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       List<Operation> operations = asList(new ReplaceOperation("/visible", true));

       getClient(researcherToken).perform(patch("/api/cris/profiles/" + researcher.getID())
                                 .content(getPatchContent(operations))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(true)));

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(true)));

       getClient(adminToken).perform(get("/api/core/items/" + CvPublication.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

       getClient(adminToken).perform(get("/api/core/items/" + CvProject.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

       getClient(adminToken).perform(get("/api/core/items/" + CvPatent.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

       // hide the profile and linked CVs
       List<Operation> operations2 = asList(new ReplaceOperation("/visible", false));

       getClient(researcherToken).perform(patch("/api/cris/profiles/" + researcher.getID())
                                 .content(getPatchContent(operations2))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient(adminToken).perform(get("/api/core/items/" + CvPublication.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.uuid", Matchers.is(CvPublication.getID().toString())));

       getClient(adminToken).perform(get("/api/core/items/" + CvProject.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.uuid", Matchers.is(CvProject.getID().toString())));

       getClient(adminToken).perform(get("/api/core/items/" + CvPatent.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.uuid", Matchers.is(CvPatent.getID().toString())));

    }

    @Test
    public void cvOwnerAuthorizedToSeeNotVisibleDataTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();

       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection 1")
                                          .build();

       EPerson researcher = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       ResearcherProfile researcherProfile = createProfileForUser(researcher);

       Item CvPublication = ItemBuilder.createItem(context, col1)
               .withTitle("CvPublication Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvPublication")
               .withIssueDate("2021-01-01")
               .build();

       Item CvProject = ItemBuilder.createItem(context, col1)
               .withTitle("CvProject Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvProject")
               .withIssueDate("2021-02-07")
               .build();

       Item CvPatent = ItemBuilder.createItem(context, col1)
               .withTitle("CvPatent Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withIssueDate("2020-10-11")
               .withEntityType("CvPatent")
               .build();

       context.restoreAuthSystemState();

       String researcherToken = getAuthToken(researcher.getEmail(), password);

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient(researcherToken).perform(get("/api/core/items/" + CvPublication.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$",  ItemMatcher
                                         .matchItemWithTitleAndDateIssued(CvPublication,
                                                 "CvPublication Title", "2021-01-01")));

       getClient(researcherToken).perform(get("/api/core/items/" + CvProject.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$",  ItemMatcher
                                               .matchItemWithTitleAndDateIssued(CvProject,
                                                       "CvProject Title", "2021-02-07")));

       getClient(researcherToken).perform(get("/api/core/items/" + CvPatent.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$",  ItemMatcher
                                                .matchItemWithTitleAndDateIssued(CvPatent,
                                                        "CvPatent Title", "2020-10-11")));
    }

    @Test
    public void cvSecurityWithResearcherProfileNotVisibleForbiddenTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();

       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection 1")
                                          .build();

       EPerson researcher = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       ResearcherProfile researcherProfile = createProfileForUser(researcher);

       Item CvPublication = ItemBuilder.createItem(context, col1)
               .withTitle("CvPublication Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvPublication")
               .withIssueDate("2021-01-01")
               .build();

       Item CvProject = ItemBuilder.createItem(context, col1)
               .withTitle("CvProject Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvProject")
               .withIssueDate("2021-02-07")
               .build();

       Item CvPatent = ItemBuilder.createItem(context, col1)
               .withTitle("CvPatent Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withIssueDate("2020-10-11")
               .withEntityType("CvPatent")
               .build();

       context.restoreAuthSystemState();

       String tokenEperson = getAuthToken(eperson.getEmail(), password);
       String researcherToken = getAuthToken(researcher.getEmail(), password);

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient(tokenEperson).perform(get("/api/core/items/" + CvPublication.getID()))
                              .andExpect(status().isForbidden());

       getClient(tokenEperson).perform(get("/api/core/items/" + CvProject.getID()))
                              .andExpect(status().isForbidden());

       getClient(tokenEperson).perform(get("/api/core/items/" + CvPatent.getID()))
                              .andExpect(status().isForbidden());
    }

    @Test
    public void cvSecurityWithResearcherProfileNotVisibleUnauthorizedTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();

       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection 1")
                                          .build();

       EPerson researcher = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       ResearcherProfile researcherProfile = createProfileForUser(researcher);

       Item CvPublication = ItemBuilder.createItem(context, col1)
               .withTitle("CvPublication Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvPublication")
               .withIssueDate("2021-01-01")
               .build();

       Item CvProject = ItemBuilder.createItem(context, col1)
               .withTitle("CvProject Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvProject")
               .withIssueDate("2021-02-07")
               .build();

       Item CvPatent = ItemBuilder.createItem(context, col1)
               .withTitle("CvPatent Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withIssueDate("2020-10-11")
               .withEntityType("CvPatent")
               .build();

       context.restoreAuthSystemState();

       String researcherToken = getAuthToken(researcher.getEmail(), password);

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient().perform(get("/api/core/items/" + CvPublication.getID()))
                  .andExpect(status().isUnauthorized());

       getClient().perform(get("/api/core/items/" + CvProject.getID()))
                  .andExpect(status().isUnauthorized());

       getClient().perform(get("/api/core/items/" + CvPatent.getID()))
                  .andExpect(status().isUnauthorized());
    }

    @Test
    public void cvSecurityWithResearcherProfileNotVisibleAdminTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();

       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection 1")
                                          .build();

       EPerson researcher = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       ResearcherProfile researcherProfile = createProfileForUser(researcher);

       Item CvPublication = ItemBuilder.createItem(context, col1)
               .withTitle("CvPublication Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvPublication")
               .withIssueDate("2021-01-01")
               .build();

       Item CvProject = ItemBuilder.createItem(context, col1)
               .withTitle("CvProject Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvProject")
               .withIssueDate("2021-02-07")
               .build();

       Item CvPatent = ItemBuilder.createItem(context, col1)
               .withTitle("CvPatent Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withIssueDate("2020-10-11")
               .withEntityType("CvPatent")
               .build();

       context.restoreAuthSystemState();

       String researcherToken = getAuthToken(researcher.getEmail(), password);
       String tokenAdmin = getAuthToken(admin.getEmail(), password);

       getClient(researcherToken).perform(get("/api/cris/profiles/" + researcher.getID()))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(false)));

       getClient(tokenAdmin).perform(get("/api/core/items/" + CvPublication.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", ItemMatcher
                                .matchItemWithTitleAndDateIssued(CvPublication, "CvPublication Title", "2021-01-01")));

       getClient(tokenAdmin).perform(get("/api/core/items/" + CvProject.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", ItemMatcher
                                       .matchItemWithTitleAndDateIssued(CvProject, "CvProject Title", "2021-02-07")));

       getClient(tokenAdmin).perform(get("/api/core/items/" + CvPatent.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", ItemMatcher
                                      .matchItemWithTitleAndDateIssued(CvPatent, "CvPatent Title", "2020-10-11")));
    }

    @Test
    public void cvSecurityWithResearcherProfileVisibleLoggedUserTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();

       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection 1")
                                          .build();

       EPerson researcher = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       ResearcherProfile researcherProfile = createProfileForUser(researcher);

       Item CvPublication = ItemBuilder.createItem(context, col1)
               .withTitle("CvPublication Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvPublication")
               .withIssueDate("2021-01-01")
               .build();

       Item CvProject = ItemBuilder.createItem(context, col1)
               .withTitle("CvProject Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvProject")
               .withIssueDate("2021-02-07")
               .build();

       Item CvPatent = ItemBuilder.createItem(context, col1)
               .withTitle("CvPatent Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withIssueDate("2020-10-11")
               .withEntityType("CvPatent")
               .build();

       context.restoreAuthSystemState();

       String tokenEperson = getAuthToken(eperson.getEmail(), password);
       String researcherToken = getAuthToken(researcher.getEmail(), password);

       List<Operation> operations = asList(new ReplaceOperation("/visible", true));

       getClient(researcherToken).perform(patch("/api/cris/profiles/" + researcher.getID())
                                 .content(getPatchContent(operations))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(true)));

        getClient(tokenEperson).perform(get("/api/core/items/" + CvPublication.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(
                                               CvPublication, "CvPublication Title", "2021-01-01")));

        getClient(tokenEperson).perform(get("/api/core/items/" + CvProject.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(
                                                       CvProject, "CvProject Title", "2021-02-07")));

        getClient(tokenEperson).perform(get("/api/core/items/" + CvPatent.getID()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", ItemMatcher.matchItemWithTitleAndDateIssued(
                                                         CvPatent, "CvPatent Title", "2020-10-11")));
    }

    @Test
    public void cvSecurityWithResearcherProfileVisibleAnonymousUserTest() throws Exception {
       context.turnOffAuthorisationSystem();

       parentCommunity = CommunityBuilder.createCommunity(context).build();

       Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection 1")
                                          .build();

       EPerson researcher = EPersonBuilder.createEPerson(context)
               .withNameInMetadata("John", "Doe")
               .withEmail("Johndoe@example.com")
               .withPassword(password).build();

       ResearcherProfile researcherProfile = createProfileForUser(researcher);

       Item CvPublication = ItemBuilder.createItem(context, col1)
               .withTitle("CvPublication Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvPublication")
               .withIssueDate("2021-01-01")
               .build();

       Item CvProject = ItemBuilder.createItem(context, col1)
               .withTitle("CvProject Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withEntityType("CvProject")
               .withIssueDate("2021-02-07")
               .build();

       Item CvPatent = ItemBuilder.createItem(context, col1)
               .withTitle("CvPatent Title")
               .withCrisOwner(researcher.getName(), researcherProfile.getId().toString())
               .withIssueDate("2020-10-11")
               .withEntityType("CvPatent")
               .build();

       context.restoreAuthSystemState();

       String researcherToken = getAuthToken(researcher.getEmail(), password);

       List<Operation> operations = asList(new ReplaceOperation("/visible", true));

       getClient(researcherToken).perform(patch("/api/cris/profiles/" + researcher.getID())
                                 .content(getPatchContent(operations))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.visible", is(true)));

        getClient().perform(get("/api/core/items/" + CvPublication.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$",
                    ItemMatcher.matchItemWithTitleAndDateIssued(CvPublication, "CvPublication Title", "2021-01-01")));

        getClient().perform(get("/api/core/items/" + CvProject.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$",
                    ItemMatcher.matchItemWithTitleAndDateIssued(CvProject, "CvProject Title", "2021-02-07")));

        getClient().perform(get("/api/core/items/" + CvPatent.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$",
                    ItemMatcher.matchItemWithTitleAndDateIssued(CvPatent, "CvPatent Title", "2020-10-11")));
    }

    private String getItemIdByProfileId(String token, String id) throws SQLException, Exception {
        MvcResult result = getClient(token).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andReturn();

        return readAttributeFromResponse(result, "$.id");
    }

    private <T> T readAttributeFromResponse(MvcResult result, String attribute) throws UnsupportedEncodingException {
        return JsonPath.read(result.getResponse().getContentAsString(), attribute);
    }

    private EntityType createEntityType(String entityType) {
        return EntityTypeBuilder.createEntityTypeBuilder(context, entityType).build();
    }

    private RelationshipType createHasShadowCopyRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, SHADOW_COPY.getLeftType(),
            SHADOW_COPY.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createCloneRelationship(EntityType leftType, EntityType rightType) {
        return createRelationshipTypeBuilder(context, leftType, rightType, CLONE.getLeftType(),
            CLONE.getRightType(), 0, 1, 0, 1).build();
    }

    private RelationshipType createIsOriginatedFromRelationship(EntityType rightType, EntityType leftType) {
        return createRelationshipTypeBuilder(context, rightType, leftType,
            ORIGINATED.getLeftType(), ORIGINATED.getRightType(), 0, null, 0, 1).build();
    }

    private RelationshipType createIsMergedInRelationship(EntityType entityType) {
        return createRelationshipTypeBuilder(context, entityType, entityType, MERGED.getLeftType(),
            MERGED.getRightType(), 0, 1, 0, null).build();
    }

    private RelationshipType createIsPersonOwnerRelationship(EntityType rightType, EntityType leftType) {
        return createRelationshipTypeBuilder(context, rightType, leftType, "isPersonOwner", "isOwnedByCvPerson",
            0, null, 0, null).build();
    }

    private List<Relationship> findRelations(Item item, RelationshipType type) throws SQLException {
        return relationshipService.findByItemAndRelationshipType(context, item, type);
    }

    private MetadataField metadataField(String schema, String element, Optional<String> qualifier)
        throws SQLException {

        MetadataSchema metadataSchema = metadataSchemaService.find(context, schema);

        return metadataFieldService.findByElement(context,
            metadataSchema,
            element,
            qualifier.orElse(null));
    }

    private ResearcherProfile createProfileForUser(EPerson ePerson) throws Exception {
        return researcherProfileService.createAndReturn(context, ePerson);
    }

}
