/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.externalservice;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.dspace.app.elasticsearch.ElasticsearchIndexQueue;
import org.dspace.app.elasticsearch.consumer.ElasticsearchIndexManager;
import org.dspace.app.elasticsearch.exception.ElasticsearchException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Event;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The scope of this class is to manage sending of requests to Elasticsearch.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchProvider {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ElasticsearchConnector elasticsearchConnector;

    @Autowired
    private ElasticsearchIndexManager elasticsearchIndexManager;

    /**
     * Processes record according to the operation type
     * 
     * @param context        DSpace context object
     * @param record         ElasticsearchIndexQueue object
     * @param json           Json representation of item related to the record
     * @throws IOException   if IO error
     * @throws SQLException  If there's a database problem
     */
    public void processRecord(Context context, ElasticsearchIndexQueue record, String json)
            throws IOException, SQLException {
        String index = getIndex(context, record);
        if (StringUtils.isBlank(index)) {
            throw new RuntimeException("Not found index for ElasticsearchIndexQueue with uuid: " + record.getId());
        }
        switch (record.getOperationType()) {
            case Event.CREATE : addDocument(record, json, index);
                break;
            case Event.DELETE : deleteDocument(record, index);
                break;
            case Event.MODIFY : updateDocument(record, json, index);
                break;
            case Event.MODIFY_METADATA : updateDocument(record, json, index);
                break;
            default:
                throw new RuntimeException("The operation type : " + record.getOperationType() +
                                     " for ElasticsearchProvider with uuid: " + record.getId() + " is not supported!");
        }
    }

    private void addDocument(ElasticsearchIndexQueue record, String json, String index) throws IOException {
        HttpResponse responce =  elasticsearchConnector.create(json, index, null);
        int status = responce.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_CREATED) {
            throw new ElasticsearchException("It was not possible to CREATE document with uuid: " + record.getId() +
                                       "  Elasticsearch returned status code : " + status);
        }
    }

    private void updateDocument(ElasticsearchIndexQueue record, String json, String index) throws IOException {
        String docID = getDocIdByField(index, record.getId().toString());
        HttpResponse response = elasticsearchConnector.update(json, index, docID);
        int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
            throw new ElasticsearchException("It was not possible to UPDATE document with uuid: " + record.getId() +
                                       "  Elasticsearch returned status code : " + status);
        }
    }

    private void deleteDocument(ElasticsearchIndexQueue record, String index) throws IOException {
        String docID = getDocIdByField(index, record.getId().toString());
        HttpResponse response = elasticsearchConnector.delete(index, docID);
        int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
            throw new ElasticsearchException("It was not possible to DELETE document with uuid: " + record.getId() +
                                       "  Elasticsearch returned status code : " + status);
        }
    }

    private String getIndex(Context context, ElasticsearchIndexQueue record) throws SQLException, IOException {
        if (record.getOperationType() == Event.DELETE) {
            for (String index : elasticsearchIndexManager.getEntityType2Index().values()) {
                String docID = getDocIdByField(index, record.getId().toString());
                HttpResponse responce = elasticsearchConnector.searchByIndexAndDoc(index, docID);
                int status = responce.getStatusLine().getStatusCode();
                if (status == HttpStatus.SC_OK) {
                    return index;
                }
            }
        }
        Item item = itemService.find(context, record.getId());
        if (Objects.nonNull(item)) {
            String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
            return elasticsearchIndexManager.getEntityType2Index().containsKey(entityType)
                         ? elasticsearchIndexManager.getEntityType2Index().get(entityType) : StringUtils.EMPTY;
        }
        return StringUtils.EMPTY;
    }

    private String getDocIdByField(String index, String id) throws IOException {
        HttpResponse response = elasticsearchConnector.searchByFieldAndValue(index, "resourceId", id);
        int status = response.getStatusLine().getStatusCode();
        InputStream is = response.getEntity().getContent();
        if (status != HttpStatus.SC_OK || Objects.isNull(is)) {
            throw new ElasticsearchException("It was not possible to retrieve document by field 'resourceId'"
                    + " and value: " + id + "  Elasticsearch returned status code : " + status);
        }
        return getDocumentIdFromResponse(is);
    }

    private String getDocumentIdFromResponse(InputStream is) throws IOException {
        JSONObject json = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
        if (json.has("hits")) {
            json = new JSONObject(json.get("hits").toString());
            if (json.has("hits")) {
                JSONArray array = json.getJSONArray("hits");
                if (!array.isNull(0)) {
                    json = new JSONObject(array.get(0).toString());
                    if (json.has("_id")) {
                        return json.get("_id").toString();
                    }
                }
            }
        }
        return StringUtils.EMPTY;
    }

    public ElasticsearchConnector getElasticsearchConnector() {
        return elasticsearchConnector;
    }

    public void setElasticsearchConnector(ElasticsearchConnector elasticsearchConnector) {
        this.elasticsearchConnector = elasticsearchConnector;
    }

}