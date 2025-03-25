package com.atlassian.tutorial.myPlugin.config;


import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Configuration;



@Configuration
public class AwsConfig {

    @Value("${aws.api.base.url}")
    private String baseUrl;

    @Value("${aws.api.key}")
    private String apiKey;

    @Value("${aws.api.endpoints.messages}")
    private String messagesEndpoint;

    @Value("${aws.api.endpoints.analytics}")
    private String analyticsEndpoint;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getMessagesUrl() {
        return baseUrl + messagesEndpoint;
    }

    public String getAnalyticsUrl() {
        return baseUrl + analyticsEndpoint;
    }

    public String getApiKey() {
        return apiKey;
    }
}