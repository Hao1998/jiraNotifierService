package com.atlassian.tutorial.myPlugin.config;

import javax.annotation.PostConstruct;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import java.net.HttpURLConnection;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Configuration;

import javax.inject.Named;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;


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


    public String getBaseUrl() {
        return baseUrl;
    }

    public String getMessagesEndpoint() {
        return messagesEndpoint;
    }

    // Method to send API requests without AWS SDK
    public String invokeApi(String url, String method, String payload, String apiKey) {
        try {
            System.out.println("Making request to invokeApi: " + url);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            if (apiKey != null && !apiKey.isEmpty()) {
                connection.setRequestProperty("x-api-key", apiKey);
            }
            connection.setDoOutput(true);
            if (payload != null && !payload.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            int responseCode = connection.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300) ? connection.getInputStream() : connection.getErrorStream();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
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