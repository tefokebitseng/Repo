/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.consumer.ElasticsearchIndexManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.integration.crosswalks.ReferCrosswalk;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the converter from/to the Item
 * in the JSON/XML data model using ReferCrosswalk.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchItemBuilder {

    private static final Logger log = LogManager.getLogger(ElasticsearchItemBuilder.class);

    private final Map<String,ReferCrosswalk> entity2ReferCrosswalk;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ElasticsearchIndexManager elasticsearchIndexManager;

    @Autowired
    private ElasticsearchDenormalizer elasticsearchDenormaliser;

    public ElasticsearchItemBuilder(Map<String,ReferCrosswalk> entity2ReferCrosswalk) {
        this.entity2ReferCrosswalk = entity2ReferCrosswalk;
    }

    /**
     * Convert related Item of record in JSON/XML format
     * 
     * @param context         DSpace context object
     * @param record          ElasticsearchIndexQueue object
     * @return                as String converted object, empty if related item non exist or EntityType non supported
     * @throws SQLException   if database error
     */
    public List<String> convert(Context context, ElasticsearchIndexQueue record) throws SQLException {
        Item item = itemService.find(context, record.getId());
        return convert(context, item);
    }

    /**
     * Convert Item in JSON/XML format
     * 
     * @param context           DSpace context object
     * @param item              Item
     * @return                  as String converted object, empty if item non exist or EntityType non supported
     * @throws SQLException     if database error
     */
    public List<String> convert(Context context, Item item) throws SQLException {
        if (Objects.nonNull(item)) {
            String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
            if (elasticsearchIndexManager.isSupportedEntityType(entityType)) {
                ReferCrosswalk referCrosswalk = entity2ReferCrosswalk.get(entityType);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    referCrosswalk.disseminate(context, item, out);
                    return elasticsearchDenormaliser.denormalize(entityType, out.toString());
                } catch (CrosswalkException | IOException | AuthorizeException e) {
                    log.error(e.getMessage());
                }
            }
        }
        return Collections.emptyList();
    }

}