package ai.berticloud.webapp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;

@Component
public class GcpOidcInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GcpOidcInterceptor.class);
    private static final String METADATA_SERVER_URL = "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity?audience=";
    
    private final String adminServiceUrl;

    public GcpOidcInterceptor(@Value("${app.admin-service.url}") String adminServiceUrl) {
        this.adminServiceUrl = adminServiceUrl;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // Applica il token solo se stiamo chiamando l'admin-service
        if (request.getURI().toString().startsWith(adminServiceUrl)) {
            try {
                String token = fetchOidcToken(adminServiceUrl);
                if (token != null && !token.isEmpty()) {
                    request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch GCP OIDC token for audience {}: {}", adminServiceUrl, e.getMessage());
            }
        }
        return execution.execute(request, body);
    }

    private String fetchOidcToken(String audience) throws IOException {
        URL url = new URL(METADATA_SERVER_URL + audience);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Metadata-Flavor", "Google");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Metadata server returned HTTP " + responseCode);
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response.toString();
        }
    }
}
