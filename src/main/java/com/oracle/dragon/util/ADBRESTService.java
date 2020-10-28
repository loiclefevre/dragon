package com.oracle.dragon.util;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Runs SQL queries using the REST service of Autonomous Databases (ADBs).
 */
public class ADBRESTService {
    /**
     * URL of the service.
     */
    private final String urlSQLService;

    /**
     * SODA service URL.
     */
    private final String urlSODAService;

    private final String urlPrefix;

    /**
     * User for authentication.
     */
    private final String user;

    /**
     * Password for authentication.
     */
    private final String password;

    public ADBRESTService(final String sqlDevWebUrl, final String user, final String password) {
        final String url = sqlDevWebUrl;
        int ordsPos = url.indexOf("/ords/");
        this.urlPrefix = url.substring(0, ordsPos + 6) + user.toLowerCase() + "/";
        this.urlSQLService = this.urlPrefix + "_/sql";
        this.urlSODAService = this.urlPrefix + "soda/latest/";
        this.user = user;
        this.password = password;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public String getUrlSQLService() {
        return urlSQLService;
    }

    public String getUrlSODAService() {
        return urlSODAService;
    }

    public String execute(final String command) {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlSQLService))
                    .headers("Content-Type", "application/sql",
                            "Authorization", basicAuth(user, password))
                    .POST(HttpRequest.BodyPublishers.ofString(command))
                    .build();

            final HttpResponse<String> response = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(ProxySelector.getDefault())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Request was not successful (" + response.statusCode() + ")");
            }

            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("REST SQL Service could not run " + command, e);
        }
    }

    /**
     * BASIC authentication encoding in base 64.
     *
     * @param user  user to use for authentication
     * @param password  password to use for authentication
     * @return the base 64 encoded authentication signature
     */
    private String basicAuth(final String user, final String password) {
        return String.format("Basic %s", Base64.getEncoder().encodeToString((String.format("%s:%s",user,  password)).getBytes()));
    }

    /**
     * Creates a SODA collection.
     *
     * @param collectionName    the name of the SODA collection
     * @return the HTTPS response body
     */
    public String createSODACollection(final String collectionName) {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlSODAService + collectionName))
                    .headers("Authorization", basicAuth(user, password))
                    .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            final HttpResponse<String> response = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(ProxySelector.getDefault())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new RuntimeException("Request was not successful (" + response.statusCode() + "):\n" + response.body());
            }

            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("REST SODA Service could not create collection " + collectionName, e);
        }
    }

    public String insertDocument(final String collectionName, final String document) {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlSODAService + collectionName))
                    .headers("Content-Type", "application/json", "Authorization", basicAuth(user, password))
                    .POST(HttpRequest.BodyPublishers.ofString(document, StandardCharsets.UTF_8))
                    .build();

            final HttpResponse<String> response = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(ProxySelector.getDefault())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new RuntimeException("Request was not successful (" + response.statusCode() + "):\n" + response.body());
            }

            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("REST SODA Service could not insert document " + document + " into collection " + collectionName, e);
        }
    }
}
