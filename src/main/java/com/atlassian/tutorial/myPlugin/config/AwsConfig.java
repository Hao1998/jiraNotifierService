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
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Configuration;

import javax.inject.Named;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
        try {
            String url = baseUrl + path;

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

        } catch (Exception e) {
            // Log and handle exception
            throw new RuntimeException("Failed to invoke API", e);
        }
    }


    private String executeSignedRequest(Request<?> request) {
        try {
            URL url = request.getEndpoint().toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(request.getHttpMethod().name());

            // Add all headers from the signed request
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Write content if it exists
            if (request.getContent() != null) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    IOUtils.copy(request.getContent(), out);
                }
            }

            // Get response
            int responseCode = connection.getResponseCode();
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
                throw new RuntimeException("API request failed with status: " + responseCode);
            }
        } catch (IOException e) {
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