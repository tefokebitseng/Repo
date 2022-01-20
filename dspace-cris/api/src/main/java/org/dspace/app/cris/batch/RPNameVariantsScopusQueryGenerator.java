package org.dspace.app.cris.batch;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class RPNameVariantsScopusQueryGenerator extends AFeedVariantsQueryGenerator {

    private static Logger log = Logger.getLogger(RPNameVariantsScopusQueryGenerator.class);

    @Override
    public HashMap<UUID, List<String>> generate(Context context) {
        HashMap<UUID,List<String>> map = new HashMap<>();
        SearchService searchService = new DSpace().getSingletonService(SearchService.class);
        SolrQuery solrQuery = new SolrQuery("search.resourcetype:9");
        solrQuery.setRows(Integer.MAX_VALUE);
        if (getSolrExtraFilter() != null) {
            solrQuery.addFilterQuery(getSolrExtraFilter());
        }
        try {
            QueryResponse res = searchService.search(solrQuery);
            SolrDocumentList docs = res.getResults();
            Iterator<SolrDocument> iter = docs.iterator();

            List<String> notOwnedList = new ArrayList<String>();
            while(iter.hasNext()) {

                SolrDocument doc = iter.next();

                List<Object> variants = isUseVariants()
                        ? (List<Object>) doc.getFieldValues("crisrp.variants")
                        : new ArrayList<Object>();
                StringBuffer buffer = new StringBuffer();
                int x=0;
                for(Object variant: variants) {
                    if(x>0) {
                        buffer.append(" OR ");
                    }
                    buffer.append("(");
                    if(StringUtils.contains(variant.toString(), ",")) {
                        buffer.append("AUTHOR-NAME(");
                    }else {
                        buffer.append("AUTHLASTNAME(");
                    }
                    buffer.append(variant).append("))");
                    x++;
                }

                for(String filter : getExtraFilters()) {
                    buffer.append(" ");
                    buffer.append(filter);
                }
                if(doc.containsKey("owner_s")){
                    UUID submitterId = UUID.fromString((String) doc.getFirstValue("owner_s"));
                    List<String> list = new ArrayList<String>();
                    list.add(buffer.toString());
                    map.put(submitterId, list);
                }else {
                    notOwnedList.add(buffer.toString());
                }
            }
            if(notOwnedList != null && !notOwnedList.isEmpty()) {
                UUID defaultSubmitter = FeedUtils.retrieveDefaultSubmitter(context, getDefaultSubmitter());
                if (defaultSubmitter != null) {
                    map.put(defaultSubmitter, notOwnedList);
                } else {
                    log.error("Skip record - default submitter not found");
                }
            }
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
        }
        return map;
    }
}
