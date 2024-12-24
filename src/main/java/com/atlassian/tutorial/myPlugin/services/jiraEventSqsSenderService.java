package com.atlassian.tutorial.myPlugin.services;


import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.atlassian.jira.issue.Issue;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class jiraEventSqsSenderService {


    @Value("${aws.sqs.queue.url}")
    private String QUEUE_URL;

    @Autowired
    private AmazonSQS sqsClient;

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
