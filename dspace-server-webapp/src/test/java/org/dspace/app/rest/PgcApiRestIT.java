/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.lyncode.xoai.dataprovider.core.Granularity.Second;
import static org.dspace.xoai.util.ItemUtils.retrieveMetadata;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.persistence.OrderBy;
import javax.xml.stream.XMLStreamException;

import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.configuration.SignatureValidationUtil;
import org.dspace.app.configuration.ThreeLeggedTokenFilter;
import org.dspace.app.configuration.TwoLeggedTokenFilter;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.util.Util;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.handle.Handle;
import org.dspace.pgc.solr.DSpaceSolrCoreServer;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.SolrUtils;
import org.dspace.xoai.app.XOAIExtensionItemCompilePlugin;
import org.dspace.xoai.services.api.CollectionsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import com.nimbusds.jwt.JWTClaimsSet.Builder;

/**
 * Testing class for search pgc-api module controller
 *
 * @author Alba Aliu
 */

public class PgcApiRestIT extends AbstractControllerIntegrationTest {
    private static final Logger log = LogManager.getLogger(DSpaceSolrCoreServer.class);

    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    protected AuthorizeService authorizeService;
    @Autowired
    private CollectionsService collectionsService;

    private Community community;
    private Collection collection;
    @OrderBy("id ASC")
    private List<Handle> handles = new ArrayList<>();
    private List<XOAIExtensionItemCompilePlugin> xOAIItemCompilePlugins;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Explicitly use solr commit in SolrLoggerServiceImpl#postView
        configurationService.setProperty("solr-oai.autoCommit", false);
        requestFilters.add(new TwoLeggedTokenFilter());
        requestFilters.add(new ThreeLeggedTokenFilter());
    }

    @Test
    public void itemsOaiPeopleCollection() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String key = getRsaKeyAsString(keyPair);
        String token = getTokenPublic(DateUtils.addHours(new Date(), 1), keyPair.getPrivate());
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        // create the item to be set into items table
        Item item = ItemBuilder.createItem(context, collection).withEntityType("Person")
                .withPersonAffiliation("OrgUnit")
                .withTitle("title one")
                .build();
        addNewOaiSolrDocument(item);
        Item item2 = ItemBuilder.createItem(context, collection).withEntityType("Person")
                .withPersonAffiliation("OrgUnit")
                .withTitle("other")
                .build();
        addNewOaiSolrDocument(item2);
//        String token = getTokenPublic(DateUtils.addHours(new Date(), 1));
        configurationService.setProperty("pgc-api." + "people" + ".id", collection.getID().toString());
        MockedStatic signatureValidationUtil = Mockito.mockStatic(SignatureValidationUtil.class, CALLS_REAL_METHODS);
        signatureValidationUtil.when(SignatureValidationUtil::getKey).thenReturn(key);
        signatureValidationUtil.when(SignatureValidationUtil::validateSignature).thenCallRealMethod();  // Real implementation
        getClient(token).perform(
                        get("/pgc-api/people?query=dc.title:title*&page=0&size=10"))
                .andExpect(status().isOk())
                // evaluate header part
                .andExpect(xpath("Search-API/Header/source").string("pgc-api"))
                .andExpect(xpath("Search-API/Header/api-version").string(Util.getSourceVersion()))
                .andExpect(xpath("Search-API/Header/page").string("0")) // default values
                .andExpect(xpath("Search-API/Header/size").string("10")) // default values
                .andExpect(xpath("Search-API/Header/totalResults").string("1"))
                .andExpect(xpath("Search-API/Header/resultsInPage").string("1"))
                .andExpect(xpath("Search-API/Payload/Cerif/Person/Affiliation/OrgUnit/Name").string("OrgUnit"));
        signatureValidationUtil.close();
    }
    @Test
    public void requestTwoLeggedTokenWithDifferentSignTokenFromPkPair() throws Exception {
        KeyPair keyPair = generateKeyPair();
        KeyPair keyPair1 = generateKeyPair();
        String key = getRsaKeyAsString(keyPair);
        String token = getTokenPublic(DateUtils.addHours(new Date(), 1), keyPair1.getPrivate());
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        // create the item to be set into items table
        Item item = ItemBuilder.createItem(context, collection).withEntityType("Person")
                .withPersonAffiliation("OrgUnit")
                .withTitle("title one")
                .build();
        addNewOaiSolrDocument(item);
        Item item2 = ItemBuilder.createItem(context, collection).withEntityType("Person")
                .withPersonAffiliation("OrgUnit")
                .withTitle("other")
                .build();
        addNewOaiSolrDocument(item2);
//        String token = getTokenPublic(DateUtils.addHours(new Date(), 1));
        configurationService.setProperty("pgc-api." + "people" + ".id", collection.getID().toString());
        MockedStatic signatureValidationUtil = Mockito.mockStatic(SignatureValidationUtil.class, CALLS_REAL_METHODS);
        signatureValidationUtil.when(SignatureValidationUtil::getKey).thenReturn(key);
        signatureValidationUtil.when(SignatureValidationUtil::validateSignature).thenCallRealMethod();  // Real implementation
        getClient(token).perform(
                        get("/pgc-api/people?query=dc.title:title*&page=0&size=10"))
                .andExpect(status().isUnauthorized());
        signatureValidationUtil.close();
    }
    @Test
    public void requestThreeLeggedTokenWithDifferentSignTokenFromPkPair() throws Exception {
        KeyPair keyPair = generateKeyPair();
        KeyPair keyPairFail = generateKeyPair();
        String key = getRsaKeyAsString(keyPair);
        String token = getTokenPublic(DateUtils.addHours(new Date(), 1), keyPairFail.getPrivate());
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        MockedStatic signatureValidationUtil = Mockito.mockStatic(SignatureValidationUtil.class, CALLS_REAL_METHODS);
        signatureValidationUtil.when(SignatureValidationUtil::getKey).thenReturn(key);
        signatureValidationUtil.when(SignatureValidationUtil::validateSignature).thenCallRealMethod();  // Real implementation
        // create the item to be set into items table
        Item item = ItemBuilder.createItem(context, collection).build();
        Item item_related = ItemBuilder
                .createItem(context, collection)
                .withEntityType("Patent")
                .withCtiVitaeOwner("600", item.getID().toString())
                .build();
        addNewOaiSolrDocument(item);
        addNewOaiSolrDocument(item_related);
        getClient(token).perform(
                        get("/pgc-api/ctivitae/" + item.getID() + "/patents?page=0&size=5"))
                .andExpect(status().isUnauthorized());
        signatureValidationUtil.close();
    }
    @Test
    public void itemsOaiPeopleCollectionExpiredToken() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String key = getRsaKeyAsString(keyPair);
        String token = getTokenPublic(DateUtils.addHours(new Date(), -1), keyPair.getPrivate());
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        // create the item to be set into items table
        Item item = ItemBuilder.createItem(context, collection).withEntityType("Person")
                .withPersonAffiliation("OrgUnit")
                .withTitle("title one")
                .build();
        addNewOaiSolrDocument(item);
        Item item2 = ItemBuilder.createItem(context, collection).withEntityType("Person")
                .withPersonAffiliation("OrgUnit")
                .withTitle("other")
                .build();
        addNewOaiSolrDocument(item2);
//        String token = getTokenPublic(DateUtils.addHours(new Date(), 1));
        configurationService.setProperty("pgc-api." + "people" + ".id", collection.getID().toString());
        MockedStatic signatureValidationUtil = Mockito.mockStatic(SignatureValidationUtil.class, CALLS_REAL_METHODS);
        signatureValidationUtil.when(SignatureValidationUtil::getKey).thenReturn(key);
        signatureValidationUtil.when(SignatureValidationUtil::validateSignature).thenCallRealMethod();  // Real implementation
        getClient(token).perform(
                        get("/pgc-api/people?query=dc.title:title*&page=0&size=10"))
                .andExpect(status().isUnauthorized());
        signatureValidationUtil.close();
    }

    @Test
    public void itemsOaiPublicationCollection() throws Exception {
        // ** WHEN **
        KeyPair keyPair = generateKeyPair();
        String key = getRsaKeyAsString(keyPair);
        String token = getTokenPublic(DateUtils.addHours(new Date(), 1), keyPair.getPrivate());
        context.turnOffAuthorisationSystem();
        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword(password)
                .build();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        // create the item to be set into items table
        Item item = ItemBuilder
                .createItem(context, collection).withEntityType("Publication")
                .withTitle("Publ1").withAuthor("Author1").build();
        configurationService.setProperty("pgc-api." + "publications" + ".id", collection.getID().toString());
        MockedStatic signatureValidationUtil = Mockito.mockStatic(SignatureValidationUtil.class, CALLS_REAL_METHODS);
        signatureValidationUtil.when(SignatureValidationUtil::getKey).thenReturn(key);
        signatureValidationUtil.when(SignatureValidationUtil::validateSignature).thenCallRealMethod();  // Real implementation
        String[] args = new String[]{"oai", "import", "-c"};
        addNewOaiSolrDocument(item);
        getClient(token).perform(
                        get("/pgc-api/publications?query=*:*&page=0&size=10"))
                .andExpect(status().isOk())
                // evaluate header part
                .andExpect(xpath("Search-API/Header/source").string("pgc-api"))
                .andExpect(xpath("Search-API/Header/api-version").string(Util.getSourceVersion()))
                .andExpect(xpath("Search-API/Header/page").string("0")) // default values
                .andExpect(xpath("Search-API/Header/size").string("10")) // default values
                .andExpect(xpath("Search-API/Header/totalResults").string("1"))
                .andExpect(xpath("Search-API/Header/resultsInPage").string("1"))
                .andExpect(xpath("Search-API/Payload/Cerif/Publication/Title").string("Publ1"));
        signatureValidationUtil.close();
    }

    @Test
    public void itemsOaiOrgUnitsCollection() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String key = getRsaKeyAsString(keyPair);
        String token = getTokenPublic(DateUtils.addHours(new Date(), 1), keyPair.getPrivate());
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        // create the item to be set into items table
        Item item = ItemBuilder.createItem(context, collection).withEntityType("Item").build();
        configurationService.setProperty("pgc-api." + "orgunits" + ".id", collection.getID().toString());
        MockedStatic signatureValidationUtil = Mockito.mockStatic(SignatureValidationUtil.class, CALLS_REAL_METHODS);
        signatureValidationUtil.when(SignatureValidationUtil::getKey).thenReturn(key);
        signatureValidationUtil.when(SignatureValidationUtil::validateSignature).thenCallRealMethod();  // Real implementation
        addNewOaiSolrDocument(item);
        getClient(token).perform(
                        get("/pgc-api/orgunits?query=*:*&page=0&size=5"))
                .andExpect(status().isOk())
                // evaluate header part
                .andExpect(xpath("Search-API/Header/source").string("pgc-api"))
                .andExpect(xpath("Search-API/Header/api-version").string(Util.getSourceVersion()))
                .andExpect(xpath("Search-API/Header/page").string("0")) // default values
                .andExpect(xpath("Search-API/Header/size").string("5")) // default values
                .andExpect(xpath("Search-API/Header/totalResults").string("1"))
                .andExpect(xpath("Search-API/Header/resultsInPage").string("1"));
        signatureValidationUtil.close();
    }

    @Test
    public void missingQueryEndsInBadRequest() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String key = getRsaKeyAsString(keyPair);
        String token = getTokenPublic(DateUtils.addHours(new Date(), 1), keyPair.getPrivate());
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        // create the item to be set into items table
        Item item = ItemBuilder.createItem(context, collection).withEntityType("Item").build();
        configurationService.setProperty("pgc-api." + "orgunits" + ".id", collection.getID().toString());
        MockedStatic signatureValidationUtil = Mockito.mockStatic(SignatureValidationUtil.class, CALLS_REAL_METHODS);
        signatureValidationUtil.when(SignatureValidationUtil::getKey).thenReturn(key);
        signatureValidationUtil.when(SignatureValidationUtil::validateSignature).thenCallRealMethod();  // Real implementationmentation
        addNewOaiSolrDocument(item);
        getClient(token).perform(
                        get("/pgc-api/orgunits?page=0&size=5"))
                .andExpect(status().isBadRequest());
        signatureValidationUtil.close();
    }

    @Test
    public void oaiXmlRepresentationPeopleCollectionPerId() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String key = getRsaKeyAsString(keyPair);
        String token = getTokenPublic(DateUtils.addHours(new Date(), 1), keyPair.getPrivate());
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        // create the item to be set into items table
        Item item = ItemBuilder.createItem(context, collection).withEntityType("Person")
                .withPersonAffiliation("OrgUnit")
                .build();
        addNewOaiSolrDocument(item);
        MockedStatic signatureValidationUtil = Mockito.mockStatic(SignatureValidationUtil.class, CALLS_REAL_METHODS);
        signatureValidationUtil.when(SignatureValidationUtil::getKey).thenReturn(key);
        signatureValidationUtil.when(SignatureValidationUtil::validateSignature).thenCallRealMethod();  // Real implementation
        getClient(token).perform(
                        get("/pgc-api/people/" + item.getID()))
                .andExpect(status().isOk())
                // evaluate header part
                .andExpect(xpath("Search-API/Header/source").string("pgc-api"))
                .andExpect(xpath("Search-API/Header/api-version").string(Util.getSourceVersion()))
                .andExpect(xpath("Search-API/Header/page").string("0")) // default values
                .andExpect(xpath("Search-API/Header/size").string("10")) // default values
                .andExpect(xpath("Search-API/Header/totalResults").string("1"))
                .andExpect(xpath("Search-API/Header/resultsInPage").string("1"))
                .andExpect(xpath("Search-API/Payload/Cerif/Person/Affiliation/OrgUnit/Name").string("OrgUnit"));
        signatureValidationUtil.close();
    }

    @Test
    public void oaiXmlRepresentationCtivitaeCollectionPerIdNotValidToken() throws Exception {
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword(password)
                .build();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        // create the item to be set into items table
        Item item = ItemBuilder.createItem(context, collection).withEntityType("Person").build();
        addNewOaiSolrDocument(item);
        getClient("").perform(get("/pgc-api/people/" + item.getID()))
                .andExpect(status().isUnauthorized());
     }

    @Test
    public void expiredTokenReturnsUnauthorized() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String key = getRsaKeyAsString(keyPair);
        String token = getTokenPublic(DateUtils.addHours(new Date(), -1), keyPair.getPrivate());
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        // create the item to be set into items table
        Item item = ItemBuilder.createItem(context, collection).withEntityType("Person").build();
        MockedStatic signatureValidationUtil = Mockito.mockStatic(SignatureValidationUtil.class, CALLS_REAL_METHODS);
        signatureValidationUtil.when(SignatureValidationUtil::getKey).thenReturn(key);
        signatureValidationUtil.when(SignatureValidationUtil::validateSignature).thenCallRealMethod();  // Real implementation
        addNewOaiSolrDocument(item);
        getClient("").perform(get("/pgc-api/people/" + item.getID()))
                .andExpect(status().isUnauthorized());
        signatureValidationUtil.close();
    }

    @Test
    public void oaiXmlRepresentationPatentsPerIdAndInverseRelation() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String key = getRsaKeyAsString(keyPair);
        String token = getTokenPublic(DateUtils.addHours(new Date(), 1), keyPair.getPrivate());
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        // create People collection
        collection = CollectionBuilder.createCollection(context, community).build();
        MockedStatic signatureValidationUtil = Mockito.mockStatic(SignatureValidationUtil.class, CALLS_REAL_METHODS);
        signatureValidationUtil.when(SignatureValidationUtil::getKey).thenReturn(key);
        signatureValidationUtil.when(SignatureValidationUtil::validateSignature).thenCallRealMethod();  // Real implementation
        // create the item to be set into items table
        Item item = ItemBuilder.createItem(context, collection).build();
        Item item_related = ItemBuilder
                .createItem(context, collection)
                .withEntityType("Patent")
                .withCtiVitaeOwner("600", item.getID().toString())
                .build();
        addNewOaiSolrDocument(item);
        addNewOaiSolrDocument(item_related);
        getClient(token).perform(
                        get("/pgc-api/ctivitae/" + item.getID() + "/patents?page=0&size=5"))
                .andExpect(status().isOk())
                // evaluate header part
                .andExpect(xpath("Search-API/Header/source").string("pgc-api"))
                .andExpect(xpath("Search-API/Header/api-version").string(Util.getSourceVersion()))
                .andExpect(xpath("Search-API/Header/page").string("0")) // default values
                .andExpect(xpath("Search-API/Header/size").string("5")) // default values
                .andExpect(xpath("Search-API/Header/totalResults").string("1"))
                .andExpect(xpath("Search-API/Header/resultsInPage").string("1"));
        signatureValidationUtil.close();
    }

    private String getTokenPublic(final Date expirationTIme, PrivateKey privateKey) throws Exception {
        Builder claimsSetBuilder = new Builder()
                .claim("scope", "pgc-public")
                .expirationTime(expirationTIme);
        RSASSASigner signer = new RSASSASigner(privateKey);
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.RS512),
                claimsSetBuilder.build());
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            log.error(noSuchAlgorithmException.getMessage());
        }
        return null;
    }

    private String getRsaKeyAsString(KeyPair keyPair) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .algorithm(JWSAlgorithm.RS512)
                .keyID(UUID.randomUUID().toString())
                .build();
        return rsaKey.toString();
    }

    private byte[] getSharedKey() {
        SecureRandom random = new SecureRandom();
        byte[] sharedKey = new byte[32];
        random.nextBytes(sharedKey);
        return sharedKey;
    }


    private void addNewOaiSolrDocument(Item item) {
        try {
            SolrClient oaiSolrServer = getSolrOaiServer();
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("item.id", item.getID().toString());
            String handle = item.getHandle();
            doc.addField("item.handle", handle);
            boolean isEmbargoed = !this.isPublic(item);
            boolean isIndexed = false;
            boolean isPublic = !isEmbargoed;
            doc.addField("item.public", isPublic);
            doc.addField("item.willChangeStatus", willChangeStatus(item));
            doc.addField("item.deleted", item.isWithdrawn() || !item.isDiscoverable());
            doc.addField("item.lastmodified", SolrUtils.getDateFormatter()
                    .format(this.getMostRecentModificationDate(item)));
            if (item.getSubmitter() != null) {
                doc.addField("item.submitter", item.getSubmitter().getEmail());
            }

            for (Collection col : item.getCollections()) {
                doc.addField("item.collections", "col_" + col.getHandle().replace("/", "_"));
            }
            for (Community com : collectionsService.flatParentCommunities(context, item)) {
                doc.addField("item.communities", "com_" + com.getHandle().replace("/", "_"));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            XmlOutputContext xmlContext = XmlOutputContext.emptyContext(out, Second);
            Metadata metadata = retrieveMetadata(context, item);
            for (XOAIExtensionItemCompilePlugin xOAIItemCompilePlugin : getxOAIItemCompilePlugins()) {
                metadata = xOAIItemCompilePlugin.additionalMetadata(context, metadata, item);
            }
            metadata.write(xmlContext);
            xmlContext.getWriter().flush();
            xmlContext.getWriter().close();
            doc.addField("item.compile", out.toString());
            oaiSolrServer.add(doc);
            oaiSolrServer.commit();
        } catch (SolrServerException | IOException | SQLException | XMLStreamException |
                WritingXmlException solrServerException) {
            log.error(solrServerException.getMessage());
        }

    }


    private boolean isPublic(Item item) {
        boolean pub = false;
        try {
            // Check if READ access allowed on this Item
            pub = authorizeService.authorizeActionBoolean(context, item, Constants.READ);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
        }
        return pub;
    }


    private String getHandle() {
        return (CollectionUtils.isNotEmpty(handles) ? handles.get(0).getHandle() : null);
    }


    private SolrClient getSolrOaiServer() throws SolrServerException {
        return DSpaceSolrCoreServer.getServer();
    }


    private boolean willChangeStatus(Item item) throws SQLException {
        List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, item, Constants.READ);
        for (ResourcePolicy policy : policies) {
            if ((policy.getGroup() != null) && (policy.getGroup().getName().equals("Anonymous"))) {
                if (policy.getStartDate() != null && policy.getStartDate().after(new Date())) {
                    return true;
                }
                if (policy.getEndDate() != null && policy.getEndDate().after(new Date())) {
                    return true;
                }
            }
            context.uncacheEntity(policy);
        }
        return false;
    }

    private Date getMostRecentModificationDate(Item item) throws SQLException {
        List<Date> dates = new LinkedList<>();
        List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, item, Constants.READ);
        for (ResourcePolicy policy : policies) {
            if ((policy.getGroup() != null) && (policy.getGroup().getName().equals("Anonymous"))) {
                if (policy.getStartDate() != null) {
                    dates.add(policy.getStartDate());
                }
                if (policy.getEndDate() != null) {
                    dates.add(policy.getEndDate());
                }
            }
            context.uncacheEntity(policy);
        }
        dates.add(item.getLastModified());
        Collections.sort(dates);
        Date now = new Date();
        Date lastChange = null;
        for (Date d : dates) {
            if (d.before(now)) {
                lastChange = d;
            }
        }
        return lastChange;
    }

    /**
     * Do any additional content on "item.compile" field, depends on the plugins
     *
     * @return
     */
    private List<XOAIExtensionItemCompilePlugin> getxOAIItemCompilePlugins() {
        if (xOAIItemCompilePlugins == null) {
            xOAIItemCompilePlugins = DSpaceServicesFactory.getInstance().getServiceManager()
                    .getServicesByType(XOAIExtensionItemCompilePlugin.class);
        }
        return xOAIItemCompilePlugins;
    }
}
