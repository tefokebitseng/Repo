/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.InputStream;
import javax.annotation.PostConstruct;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * This class deals with logic management to connect to the SUNEDU outdoor service
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SuneduRestConnector {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(SuneduRestConnector.class);

    private static final String DNI = "dni";

    private String clientId;
    private String clientSecret;

    private String suneduUrl;

    private HttpClient httpClient;

    @PostConstruct
    private void setup() {
        this.httpClient = HttpClientBuilder.create()
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .build();
    }

    public InputStream get(String id) {
        InputStream result = null;
        HttpResponse response = null;
        try {
            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(clientId, clientSecret);
            provider.setCredentials(AuthScope.ANY, credentials);
            httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
            HttpPost httpPost = new HttpPost(suneduUrl);
            httpPost.setHeader("Content-type", "text/plain");
            httpPost.setHeader("Accept", "*/*");
            httpPost.setHeader("Accept-Encoding", "gzip, deflate, br");
            httpPost.setHeader("Connection", "keep-alive");

            JSONObject jsonBody = new JSONObject();
            jsonBody.put(DNI, id);

            StringEntity stringEntity = new StringEntity(jsonBody.toString());
            httpPost.getRequestLine();
            httpPost.setEntity(stringEntity);
            response = httpClient.execute(httpPost);
            result = response.getEntity().getContent();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return result;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getSuneduUrl() {
        return suneduUrl;
    }

    public void setSuneduUrl(String suneduUrl) {
        this.suneduUrl = suneduUrl;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}