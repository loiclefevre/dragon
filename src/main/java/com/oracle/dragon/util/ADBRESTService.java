package com.oracle.dragon.util;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ADBRESTService {
    private final String urlSQLService;
    private final String urlSODAService;
    private final String urlPrefix;
    private final String user;
    private final String password;

    public ADBRESTService(final String sqlDevWebUrl, final String user, final String password) {
        String url = sqlDevWebUrl;
        int ordsPos = url.indexOf("/ords/");
        this.urlPrefix = url.substring(0, ordsPos + 6) + user.toLowerCase() + "/";
        this.urlSQLService = this.urlPrefix + "_/sql";
        this.urlSODAService = this.urlPrefix + "soda/latest/";
        this.user = user;
        this.password = password;
    }

    public String execute(String command) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlSQLService))
                    .headers("Content-Type", "application/sql", "Authorization", basicAuth(user, password))
                    .POST(HttpRequest.BodyPublishers.ofString(command))
                    .build();

            HttpResponse<String> response = HttpClient
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

    private String basicAuth(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }

    public String createSODACollection(String collectionName) {
//        System.out.println("\n"+urlSODAService+collectionName);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlSODAService+collectionName))
                    .headers( "Authorization", basicAuth(user, password))
                    .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(ProxySelector.getDefault())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new RuntimeException("Request was not successful (" + response.statusCode() + "):\n"+response.body());
            }

            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("REST SODA Service could not create collection " + collectionName, e);
        }
    }

/*    public static void main(String[] args) {
        System.out.println(new ADBRESTService("https://A2OWQXAVCJDY6A5-DRAGON.adb.eu-frankfurt-1.oraclecloudapps.com/ords/dragon/", "DRAGON", "My_Strong_Pa55word").insertDocument("dragon","{\"databaseServiceURL\": \"https://adb.eu-frankfurt-1.oraclecloud.com/console/index.html?tenant_name=OCID1.TENANCY.OC1..AAAAAAAARVZI7RYVDS24A2RH46ZLNAXCTTDDFGKCD57SJWIBRIWYLZH523EA&database_name=DRAGON&service_type=ATP\", \"sqlDevWebAdmin\": \"https://A2OWQXAVCJDY6A5-DRAGON.adb.eu-frankfurt-1.oraclecloudapps.com/ords/admin/_sdw/?nav=worksheet\", \"sqlDevWeb\": \"https://A2OWQXAVCJDY6A5-DRAGON.adb.eu-frankfurt-1.oraclecloudapps.com/ords/dragon/_sdw/?nav=worksheet\", \"apexURL\": \"https://A2OWQXAVCJDY6A5-DRAGON.adb.eu-frankfurt-1.oraclecloudapps.com/ords/apex\", \"omlURL\": \"https://adb.eu-frankfurt-1.oraclecloud.com/omlusers/tenants/OCID1.TENANCY.OC1..AAAAAAAARVZI7RYVDS24A2RH46ZLNAXCTTDDFGKCD57SJWIBRIWYLZH523EA/databases/DRAGON/index.html\", \"version\": \"19c\" }"));
    }
*/
    public String insertDocument(String collectionName, String document) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlSODAService+collectionName))
                    .headers("Content-Type", "application/json", "Authorization", basicAuth(user, password))
                    .POST(HttpRequest.BodyPublishers.ofString(document, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HttpClient
                    .newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .proxy(ProxySelector.getDefault())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new RuntimeException("Request was not successful (" + response.statusCode() + "):\n"+response.body());
            }

            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("REST SODA Service could not insert document "+document+" into collection " + collectionName, e);
        }
    }
}
