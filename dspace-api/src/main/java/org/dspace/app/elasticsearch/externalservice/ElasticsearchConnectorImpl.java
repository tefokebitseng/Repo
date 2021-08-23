/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch.externalservice;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.json.JSONObject;

/**
 * This class deals with logic management to connect to the Elasticsearch external service
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class ElasticsearchConnectorImpl implements ElasticsearchConnector {

    private String user;
    private String password;
    private String url;

    private HttpClient httpClient;

    private String authHeader;

    @PostConstruct
    @SuppressWarnings("deprecation")
    private void setup() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        // set credentials
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
        provider.setCredentials(AuthScope.ANY, credentials);

        //disable ssl verification
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
        });
        SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(builder.build(),
                                               SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        this.httpClient = HttpClients.custom()
                                     .setSSLSocketFactory(sslSF)
//                                     .setDefaultCredentialsProvider(provider)
                                     .build();
        String auth = this.user + ":" + this.password;

        byte[] encodedAuth = Base64.encodeBase64(
            auth.getBytes(StandardCharsets.ISO_8859_1));

        this.authHeader = "Basic " + new String(encodedAuth);
    }

    @Override
    public HttpResponse create(String json, String index, String docId) throws IOException {
        String url = this.url + index + "/_doc";
        String id = StringUtils.isBlank(docId) ? StringUtils.EMPTY : "/" + docId;
        Map<String, String> headerConfig = new HashMap<String, String>();
        headerConfig.put("Content-type", "application/json; charset=UTF-8");
        headerConfig.put("Connection", "keep-alive");
        headerConfig.put("Accept", "*/*");
        headerConfig.put("Accept-Encoding", "gzip, deflate, br");
        return httpPostRequest(url + id, headerConfig, json);
    }

    @Override
    public HttpResponse delete(String index, String docId) throws IOException {
        return httpDeleteRequest(this.url + index + "/_doc/" + docId);
    }

    @Override
    public HttpResponse deleteIndex(String index) throws IOException {
        return httpDeleteRequest(this.url + index);
    }

    @Override
    public HttpResponse findIndex(String index) throws IOException {
        return httpGetRequest(this.url + index);
    }

    @Override
    public HttpResponse searchByFieldAndValue(String index, String field, String value) throws IOException {
        JSONObject json = new JSONObject();
        json.put("query", new JSONObject().put("match", new JSONObject().put(field,value)));
        return httpPostRequest(this.url + index + "/_search", Collections.emptyMap(), json.toString());
    }

    private HttpResponse httpPostRequest(String url, Map<String, String> headerConfig, String entity)
            throws IOException {
        HttpPost httpPost = new HttpPost(url);
        try {
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, this.authHeader);
            if (!headerConfig.isEmpty()) {
                for (String config : headerConfig.keySet()) {
                    httpPost.addHeader(config, headerConfig.get(config));
                }
            }
            if (StringUtils.isNotBlank(entity)) {
                AbstractHttpEntity httpEntity = new StringEntity(entity, "UTF-8");
                httpEntity.setChunked(true);
                httpEntity.setContentType("application/json");
                httpPost.setEntity(httpEntity);
            }
            return httpClient.execute(httpPost);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            httpPost.releaseConnection();
        }
    }

    private HttpResponse httpGetRequest(String url) throws IOException {
        final HttpGet httpGet = new HttpGet(url);
        try {
            httpGet.setHeader(HttpHeaders.AUTHORIZATION, this.authHeader);
            return httpClient.execute(httpGet);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            httpGet.releaseConnection();
        }
    }

    private HttpResponse httpDeleteRequest(String url) throws IOException {
        final HttpDelete httpDelete = new HttpDelete(url);
        try {
            httpDelete.setHeader(HttpHeaders.AUTHORIZATION, this.authHeader);
            return httpClient.execute(httpDelete);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            httpDelete.releaseConnection();
        }
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

}
