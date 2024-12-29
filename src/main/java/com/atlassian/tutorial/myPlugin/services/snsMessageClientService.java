package com.atlassian.tutorial.myPlugin.services;





import com.atlassian.jira.util.json.JSONObject;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class snsMessageClientService {

    private static final String API_ENDPOINT = "https://your-api-gateway-url";
    private static final String TOPIC_ARN = "your-sns-topic-arn";

    public void sendToSns(String message) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            JSONObject requestBody = new JSONObject();
            requestBody.put("message", message);
            requestBody.put("topicArn", TOPIC_ARN);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to publish message: " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message to SNS", e);
        }
    }

}
