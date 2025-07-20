package com.atlassian.tutorial.myPlugin.config;

import javax.annotation.PostConstruct;

import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.util.IOUtils;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import java.net.HttpURLConnection;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Configuration;

import javax.inject.Named;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


@Configuration
@ExportAsService({AwsConfig.class})
@Named("awsConfig")
public class AwsConfig {

    @Value("${aws.api.base.url}")
    private String baseUrl;

    @Value("${aws.api.endpoints.messages}")
    private String messagesEndpoint;

    @Value("${aws.api.endpoints.analytics}")
    private String analyticsEndpoint;

    private AWSCredentialsProvider credentialsProvider;

    public String getBaseUrl() {
        return baseUrl;
    }

    @PostConstruct
    public void init() {
        // Create a credentials provider that assumes the role
        credentialsProvider = new STSAssumeRoleSessionCredentialsProvider
                .Builder("arn:aws:iam::637423205741:role/jira-plugin-api-invoker", "jira-plugin-session")
                .withRoleSessionDurationSeconds(3600) // 1 hour
                .build();
    }

    // Method to sign and send API requests
    public String invokeApi(String path, String method, String payload) {
            String url = path;
            System.out.println("Making request to invokeApi: " + url);
            // Create request
            DefaultRequest<String> request = new DefaultRequest<>("execute-api");
            request.setHttpMethod(HttpMethodName.fromValue(method));
            request.setEndpoint(URI.create(url));

            if (payload != null && !payload.isEmpty()) {
                request.setContent(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)));
            }

            // Sign the request using SigV4
            AWS4Signer signer = new AWS4Signer();
            signer.setServiceName("execute-api");
            signer.setRegionName("us-east-1");
            signer.sign(request, credentialsProvider.getCredentials());

            // Execute the signed request using your HTTP client
            // (implementation will depend on your HTTP client library)
            return executeSignedRequest(request);

    }


    private String executeSignedRequest(Request<?> request) {
        try {
            URL url = request.getEndpoint().toURL();
            HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

            connection.setRequestMethod(request.getHttpMethod().name());

            // Log request details
            System.out.println("Making request to: " + url);
            System.out.println("HTTP Method: " + request.getHttpMethod().name());

            // Add and log all headers from the signed request
            System.out.println("Request Headers:");
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                System.out.println("  " + header.getKey() + ": " + header.getValue());
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Write content if it exists
            if (request.getContent() != null) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    IOUtils.copy(request.getContent(), out);
                }
                System.out.println("Request body sent");
            }

            // Get response
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Log response headers
            System.out.println("Response Headers:");
            for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                if (header.getKey() != null) {
                    System.out.println("  " + header.getKey() + ": " + String.join(", ", header.getValue()));
                }
            }

            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } else {
                // Read and log the error response
                String errorResponse = "";
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    errorResponse = response.toString();
                    System.out.println("Error Response: " + errorResponse);
                } catch (Exception e) {
                    System.out.println("Could not read error response: " + e.getMessage());
                }
                throw new RuntimeException("API request failed with status: " + responseCode +
                        " Error details: " + errorResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute HTTP request", e);
        }
    }

    public String getMessagesUrl() {
        return baseUrl + messagesEndpoint;
    }

    public String getAnalyticsUrl() {
        return baseUrl + analyticsEndpoint;
    }
}