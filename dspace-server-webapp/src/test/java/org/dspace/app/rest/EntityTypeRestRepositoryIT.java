/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Objects;

import org.dspace.app.rest.matcher.EntityTypeMatcher;
import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.service.EntityTypeService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityTypeRestRepositoryIT extends AbstractEntityIntegrationTest {


    @Autowired
    private EntityTypeService entityTypeService;
    private EntityType publicationType;
    private EntityType journalType;
    private EntityType journalIssueType;
    private EntityType orgUnitType;
    private EntityType dataPackageType;
    private EntityType personType;
    private EntityType journalVolumeType;
    private EntityType projectType;
    private EntityType CvPersonType;
    private EntityType CvPersonCloneType;
    private EntityType CvPublicationType;
    private EntityType CvPublicationCloneType;
    private EntityType CvProjectType;
    private EntityType CvProjectCloneType;
    private EntityType CvPatentType;
    private EntityType CvPatentCloneType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        publicationType = entityTypeService.findByEntityType(context, "Publication");
        if (publicationType == null) {
            publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        }
        journalType = entityTypeService.findByEntityType(context, "Journal");
        if (journalType == null) {
            journalType = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();
        }
        personType = entityTypeService.findByEntityType(context, "Person");
        if (personType == null) {
            personType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        }
        projectType = entityTypeService.findByEntityType(context, "Project");
        if (projectType == null) {
            projectType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        }
        journalVolumeType = entityTypeService.findByEntityType(context, "JournalVolume");
        if (journalVolumeType == null) {
            journalVolumeType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalVolume").build();
        }
        journalIssueType = entityTypeService.findByEntityType(context, "JournalIssue");
        if (journalIssueType == null) {
            journalIssueType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue").build();
        }
        orgUnitType = entityTypeService.findByEntityType(context, "OrgUnit");
        if (orgUnitType == null) {
            orgUnitType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();
        }
        CvPersonType = entityTypeService.findByEntityType(context, "CvPerson");
        if (Objects.isNull(CvPersonType)) {
            CvPersonType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPerson").build();
        }
        CvPersonCloneType = entityTypeService.findByEntityType(context, "CvPersonClone");
        if (Objects.isNull(CvPersonCloneType)) {
            CvPersonCloneType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPersonClone").build();
        }
        CvPublicationType = entityTypeService.findByEntityType(context, "CvPublication");
        if (Objects.isNull(CvPublicationType)) {
            CvPublicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPublication").build();
        }
        CvPublicationCloneType = entityTypeService.findByEntityType(context, "CvPublicationClone");
        if (Objects.isNull(CvPublicationCloneType)) {
            CvPublicationCloneType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPublicationClone").build();
        }
        CvProjectType = entityTypeService.findByEntityType(context, "CvProject");
        if (Objects.isNull(CvProjectType)) {
            CvProjectType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvProject").build();
        }
        CvProjectCloneType = entityTypeService.findByEntityType(context, "CvProjectClone");
        if (Objects.isNull(CvProjectCloneType)) {
            CvProjectCloneType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvProjectClone").build();
        }
        CvPatentType = entityTypeService.findByEntityType(context, "CvPatent");
        if (Objects.isNull(CvPatentType)) {
            CvPatentType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPatent").build();
        }
        CvPatentCloneType = entityTypeService.findByEntityType(context, "CvPatentClone");
        if (Objects.isNull(CvPatentCloneType)) {
            CvPatentCloneType = EntityTypeBuilder.createEntityTypeBuilder(context, "CvPatentClone").build();
        }

        context.restoreAuthSystemState();
    }

    @Test
    public void getAllEntityTypeEndpoint() throws Exception {
        //When we call this facets endpoint
        getClient().perform(get("/api/core/entitytypes"))

                   //We expect a 200 OK status
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.page.totalElements", is(15)))
                   //There needs to be a self link to this endpoint
                   .andExpect(jsonPath("$._links.self.href", containsString("api/core/entitytypes")))
                   //We have 4 facets in the default configuration, they need to all be present in the embedded section
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Publication")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Person")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Project")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "OrgUnit")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Journal")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalVolume")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalIssue")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPerson")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPersonClone")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPublication")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPublicationClone")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvProject")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvProjectClone")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPatent")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPatentClone"))
                   )));
    }

    @Test
    public void getAllEntityTypeEndpointWithPaging() throws Exception {
        getClient().perform(get("/api/core/entitytypes").param("size", "5"))

                   //We expect a 200 OK status
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.page.size", is(5)))
                   .andExpect(jsonPath("$.page.totalElements", is(15)))
                   .andExpect(jsonPath("$.page.totalPages", is(3)))
                   //There needs to be a self link to this endpoint
                   .andExpect(jsonPath("$._links.self.href", containsString("api/core/entitytypes")))
                   //We have 4 facets in the default configuration, they need to all be present in the embedded section
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Publication")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Person")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Project")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "OrgUnit")),
                       EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Journal"))
                   )));

        getClient().perform(get("/api/core/entitytypes").param("size", "5").param("page", "1"))

                   //We expect a 200 OK status
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.page.size", is(5)))
                   .andExpect(jsonPath("$.page.totalElements", is(15)))
                   .andExpect(jsonPath("$.page.totalPages", is(3)))
                   .andExpect(jsonPath("$.page.number", is(1)))
                   //There needs to be a self link to this endpoint
                   .andExpect(jsonPath("$._links.self.href", containsString("api/core/entitytypes")))
                   //We have 4 facets in the default configuration, they need to all be present in the embedded section
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalVolume")),
                       EntityTypeMatcher
                            .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalIssue")),
                       EntityTypeMatcher
                                .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPerson")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPersonClone")),
                       EntityTypeMatcher
                           .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPublication"))
                   )));
    }

    @Test
    public void retrieveOneEntityType() throws Exception {
        EntityType entityType = entityTypeService.findByEntityType(context, "Publication");
        getClient().perform(get("/api/core/entitytypes/" + entityType.getID()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", EntityTypeMatcher.matchEntityTypeEntry(entityType)));
    }

    @Test
    public void retrieveOneEntityTypeThatDoesNotExist() throws Exception {
        getClient().perform(get("/api/core/entitytypes/" + 5555))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findAllByAuthorizedCollection() throws Exception {
        try {
            context.turnOffAuthorisationSystem();

            //** GIVEN **
            //1. A community-collection structure with one parent community with sub-community and one collection.
            parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
            Collection col1 =
                CollectionBuilder.createCollection(context, parentCommunity)
                        .withEntityType("JournalIssue")
                        .withSubmitterGroup(eperson)
                        .withName("Collection 1")
                        .build();
            Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("Publication")
                    .withSubmitterGroup(eperson)
                     .withName("Collection 2")
                    .build();
            Collection col3 = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("Project")
                    .withSubmitterGroup(eperson)
                    .withName("Collection 3")
                    .build();
            Collection col4 = CollectionBuilder.createCollection(context, parentCommunity)
                    .withEntityType("Journal")
                    .withSubmitterGroup(eperson)
                    .withName("Collection 4")
                    .build();

            context.restoreAuthSystemState();


            context.setCurrentUser(eperson);
            String token = getAuthToken(eperson.getEmail(), password);
            getClient(token).perform(get("/api/core/entitytypes/search/findAllByAuthorizedCollection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                    EntityTypeMatcher
                        .matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalIssue")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Publication")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Project")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Journal"))
                )));
        } finally {
            CommunityBuilder.deleteCommunity(parentCommunity.getID());
        }
    }

    @Test
    public void findAllPaginationTest() throws Exception {
        getClient().perform(get("/api/core/entitytypes")
                   .param("page", "0")
                   .param("size", "3"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Publication")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Person")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Project"))
                    )))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=1"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=4"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$.page.size", is(3)))
                   .andExpect(jsonPath("$.page.totalElements", is(15)))
                   .andExpect(jsonPath("$.page.totalPages", is(5)))
                   .andExpect(jsonPath("$.page.number", is(0)));

        getClient().perform(get("/api/core/entitytypes")
                   .param("page", "1")
                   .param("size", "3"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "OrgUnit")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Journal")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalVolume"))
                    )))
                   .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=1"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=2"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                           Matchers.containsString("/api/core/entitytypes?"),
                           Matchers.containsString("page=4"), Matchers.containsString("size=3"))))
                   .andExpect(jsonPath("$.page.size", is(3)))
                   .andExpect(jsonPath("$.page.totalElements", is(15)))
                   .andExpect(jsonPath("$.page.totalPages", is(5)))
                   .andExpect(jsonPath("$.page.number", is(1)));

        getClient().perform(get("/api/core/entitytypes")
                .param("page", "2")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                 EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalIssue")),
                 EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPerson")),
                 EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "CvPersonClone"))
                 )))
                .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                        Matchers.containsString("/api/core/entitytypes?"),
                        Matchers.containsString("page=0"), Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                        Matchers.containsString("/api/core/entitytypes?"),
                        Matchers.containsString("page=1"), Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                        Matchers.containsString("/api/core/entitytypes?"),
                        Matchers.containsString("page=2"), Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                        Matchers.containsString("/api/core/entitytypes?"),
                        Matchers.containsString("page=3"), Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                        Matchers.containsString("/api/core/entitytypes?"),
                        Matchers.containsString("page=4"), Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$.page.size", is(3)))
                .andExpect(jsonPath("$.page.totalElements", is(15)))
                .andExpect(jsonPath("$.page.totalPages", is(5)))
                .andExpect(jsonPath("$.page.number", is(2)));
    }

    @Test
    public void findAllByAuthorizedCollectionByEPersonWithOutCvEntitiesTest() throws Exception {
        try {
            context.turnOffAuthorisationSystem();

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("JournalIssue")
                             .withSubmitterGroup(eperson)
                             .withName("Collection 1")
                             .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("Publication")
                             .withSubmitterGroup(eperson)
                             .withName("Collection 2")
                             .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("CvProject")
                             .withSubmitterGroup(eperson)
                             .withName("Collection 3")
                             .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("CvPatentClone")
                             .withSubmitterGroup(eperson)
                             .withName("Collection 4")
                             .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("CvPerson")
                             .withSubmitterGroup(eperson)
                             .withName("Collection 5")
                             .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("CvPublication")
                             .withSubmitterGroup(eperson)
                             .withName("Collection 6")
                             .build();
            context.restoreAuthSystemState();

            String token = getAuthToken(eperson.getEmail(), password);
            getClient(token).perform(get("/api/core/entitytypes/search/findAllByAuthorizedCollection"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalIssue")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Publication"))
                    )))
                    .andExpect(jsonPath("$.page.totalPages", is(1)))
                    .andExpect(jsonPath("$.page.totalElements", is(2)))
                    .andExpect(jsonPath("$.page.number", is(0)));
        } finally {
            CommunityBuilder.deleteCommunity(parentCommunity.getID());
        }
    }

    @Test
    public void findAllByAuthorizedCollectionByAdminWithOutCvEntitiesTest() throws Exception {
        try {
            context.turnOffAuthorisationSystem();

            parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("Parent Community")
                                              .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("JournalIssue")
                             .withName("Collection 1")
                             .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("Publication")
                             .withName("Collection 2")
                             .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("CvProject")
                             .withName("Collection 3")
                             .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("CvPatentClone")
                             .withName("Collection 4")
                             .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("CvPerson")
                             .withName("Collection 5")
                             .build();
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withEntityType("CvPublication")
                             .withName("Collection 6")
                             .build();
            context.restoreAuthSystemState();

            String adminToken = getAuthToken(admin.getEmail(), password);
            getClient(adminToken).perform(get("/api/core/entitytypes/search/findAllByAuthorizedCollection"))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$._embedded.entitytypes", containsInAnyOrder(
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "JournalIssue")),
                    EntityTypeMatcher.matchEntityTypeEntry(entityTypeService.findByEntityType(context, "Publication"))
                    )))
                    .andExpect(jsonPath("$.page.totalPages", is(1)))
                    .andExpect(jsonPath("$.page.totalElements", is(2)))
                    .andExpect(jsonPath("$.page.number", is(0)));
        } finally {
            CommunityBuilder.deleteCommunity(parentCommunity.getID());
        }
    }
}
