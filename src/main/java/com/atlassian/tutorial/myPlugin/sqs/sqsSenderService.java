package com.atlassian.tutorial.myPlugin.sqs;


import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.atlassian.jira.issue.Issue;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class sqsSenderService {

    private final AmazonSQS sqsClient = AmazonSQSClientBuilder.defaultClient();
    private static final String QUEUE_URL = "your-sqs-queue-url";

    public void sendToQueue(Issue issue) {
        Map<String, String> messageAttributes = new HashMap<>();
        messageAttributes.put("issueKey", issue.getKey());
        messageAttributes.put("priority", issue.getPriority().getName());
        messageAttributes.put("summary", issue.getSummary());

        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(QUEUE_URL)
                .withMessageBody(new Gson().toJson(messageAttributes))
                .withDelaySeconds(0);

        sqsClient.sendMessage(sendMessageRequest);

    }


}
