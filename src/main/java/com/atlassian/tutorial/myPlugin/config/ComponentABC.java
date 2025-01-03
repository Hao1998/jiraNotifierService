package com.atlassian.tutorial.myPlugin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertyTestComponent {

    @Value("${aws.api.endpoint}")
    private String apiEndpoint;

    @PostConstruct
    public void testProperties() {
        System.out.println("Test Component Properties:");
        System.out.println("API Endpoint: " + apiEndpoint);
    }
}
