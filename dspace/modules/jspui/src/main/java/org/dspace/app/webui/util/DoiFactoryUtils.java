package org.dspace.app.webui.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.webui.servlet.DoiFactoryServlet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.integration.batch.ScriptCrossrefSender;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.eperson.EPerson;
import org.hibernate.Session;

public class DoiFactoryUtils {
	
	public static String TABLE_NAME = ScriptCrossrefSender.TABLE_NAME_DOI2ITEM;
	public static String PREFIX_DOI = ConfigurationManager.getProperty("doi.prefix");
	public final static String CODE_END_CROSSREF_FLOW = "999";
	private static String dbName = ConfigurationManager.getProperty("db.name");
	
	/**
	 * 
	 * Factory method to build doi, if doi is passed as parameter don't call builder logic
	 * and insert it on database, remove olds doi and add new metadata dc.identifier.doi too,
	 * finally update item and commit context.
	 * Warning this method only update items in transaction, it do not commit context and 
	 * if you need to solr immediately commit, therefore, must be do it on caller method.
	 * 
	 * @param context
	 * @param items
	 * @param eperson
	 * @param type
	 * @param doi
	 * @throws IOException
	 * @throws ServletException
	 * @throws SQLException
	 * @throws AuthorizeException
	 */
	public static void internalBuildDOIAndAddToQueue(Context context,
			List<Item> items, EPerson eperson, String type, Map<UUID,String> dois)
			throws IOException, ServletException, SQLException,
			AuthorizeException {

		for (Item target : items) {
			boolean isInserted = false;
			int result = -1;
			
			String doi = null; 
			if(dois!=null) {
				doi = dois.get(target.getID());
			}			
			if (doi == null || doi.isEmpty()) {

				doi = buildDoi(context, type, target);
			}
			try {
				result = getHibernateSession(context).createSQLQuery(
								getQuery()).setParameter(0, target.getID()).setParameter(1, eperson.getID()).setParameter(2, doi.trim()).setParameter(3, type).executeUpdate();
								
				target.getItemService().addMetadata(context, target, "dc", "utils", "processdoi", null, "pending");
				
				target.getItemService().update(context, target);				
			} catch (SQLException e) {
				DoiFactoryServlet.log.error(e.getMessage(), e);
			}
			if (result > 0) {
				isInserted = true;
			} else {
				break;
			}
		}

		return;
	}


	public static String buildDoi(Context context, String type, Item target) throws IOException, SQLException, AuthorizeException,
			UnsupportedEncodingException {
		String doi = "";
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		String configurationCitation = ConfigurationManager
				.getProperty("tool.doi.citation." + type);
		if (configurationCitation == null) {
			configurationCitation = ConfigurationManager
					.getProperty("tool.doi.citation.default");
		}
		final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService()
				.getNamedPlugin(StreamDisseminationCrosswalk.class,
						configurationCitation);
		try {
			streamCrosswalkDefault.disseminate(context, target, output);
			doi = output.toString("UTF-8");
		} catch (CrosswalkException e) {
			DoiFactoryServlet.log.error(e.getMessage(), e);
		}
		return doi;
	}
	
	
	/**
	 * Get Items from a solr response
	 * 
	 * @param docs
	 * @param context
	 * @return
	 * @throws SQLException
	 */
	public static List<Item> getItemsFromSolrResult(SolrDocumentList docs,
			Context context) throws SQLException {
		List<Item> resultsItems = new ArrayList<Item>();
		if (docs != null) {
		    
		    
			for (SolrDocument doc : docs) {
				UUID resourceId = UUID.fromString((String) doc
						.getFieldValue("search.resourceid"));
				resultsItems.add(ContentServiceFactory.getInstance().getItemService().find(context, resourceId));
			}
		}
		return resultsItems;
	}

	
	
	/**
	 * Get items from list of ids
	 * 
	 * @param context
	 * @param items
	 * @return
	 * @throws SQLException
	 */
	public static List<Item> getItems(Context context, List<UUID> items)
			throws SQLException {
		List<Item> result = new ArrayList<Item>();
		for (UUID i : items) {
			Item item = ContentServiceFactory.getInstance().getItemService().find(context, i);
			result.add(item);
		}
		return result;
	}
	
	
	/**
	 * Set default query to get item with none doi metadata.
	 * 
	 * @param solrQuery
	 */
	public static void prepareDefaultSolrQuery(SolrQuery solrQuery) {
		solrQuery.addFilterQuery("-(dc.identifier.doi:[* TO *])");
		solrQuery.addFilterQuery("NOT(withdrawn:true)");
		solrQuery.addFilterQuery("search.resourcetype:2");
	}


	public static String getDoiFromDoi2Item(Context context, UUID itemId) throws SQLException {
		return (String)getHibernateSession(context).createSQLQuery("select identifier_doi from "+ TABLE_NAME +" where item_id = :par0").addScalar("identifier_doi").setParameter(0, itemId).uniqueResult();
	}

	private static String getQuery() {
	    if("oracle".equals(dbName)) {
	        return "INSERT INTO "+ TABLE_NAME +" (ID, ITEM_ID, EPERSON_ID, REQUEST_DATE, LAST_MODIFIED, IDENTIFIER_DOI, CRITERIA, RESPONSE_CODE, NOTE) "
	        + "VALUES (doi2item_seq.NEXTVAL,?,?, CURRENT_TIMESTAMP,NULL,?, ?, NULL, NULL)";
	    }
	    return "INSERT INTO "+ TABLE_NAME +" (ID, ITEM_ID, EPERSON_ID, REQUEST_DATE, LAST_MODIFIED, IDENTIFIER_DOI, CRITERIA, RESPONSE_CODE, NOTE) "
        + "VALUES (nextval(\'doi2item_seq\'),?,?,CURRENT_TIMESTAMP,NULL,?, ?, NULL, NULL)";
	}
	
    protected static Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }
}