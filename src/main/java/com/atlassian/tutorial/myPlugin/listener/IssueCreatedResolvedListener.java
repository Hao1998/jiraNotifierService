package com.atlassian.tutorial.myPlugin.listener;


import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.net.*;
import com.atlassian.tutorial.myPlugin.config.AwsConfig;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class IssueCreatedResolvedListener implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(IssueCreatedResolvedListener.class);
    private final CommentManager commentManager = ComponentAccessor.getCommentManager();

    @JiraImport
    private final EventPublisher eventPublisher;

//    @Value("${aws.api.endpoint}")
//    private String apiEndpoint;
//
//    @Value("${aws.api.key}")
//    private String apiKey;
//
//    @Value("${aws.sns.topic.arn}")
//    private String snsTopicArn;

    private final String apiEndpoint;
    private final String apiKey;
    private final String snsTopicArn;

    private final Gson gson = new Gson();

    @ComponentImport
    private final RequestFactory requestFactory;
    private final AwsConfig awsConfig;


    @Autowired
    public IssueCreatedResolvedListener(EventPublisher eventPublisher,
                                        RequestFactory requestFactory,
                                        @Value("${aws.api.base.url}") String apiEndpoint,
                                        @Value("${aws.api.key}") String apiKey,
                                        @Value("${aws.sns.topic.arn}") String snsTopicArn, AwsConfig awsConfig) {
        System.out.println("Constructing IssueCreatedResolvedListener");
        this.eventPublisher = eventPublisher;
        this.requestFactory = requestFactory;
        this.apiEndpoint = apiEndpoint;
        this.apiKey = apiKey;
        this.snsTopicArn = snsTopicArn;
        this.awsConfig = awsConfig;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("Enabling plugin");
        eventPublisher.register(this);
        System.out.println("topicArn: " + snsTopicArn);
        System.out.println("apiEndpoint: " + apiEndpoint);
        System.out.println("x-api-key: " + apiKey);
    }


    @Override
    public void destroy() throws Exception {
        System.out.println("Disabling plugin");
        eventPublisher.unregister(this);
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        Long eventTypeId = issueEvent.getEventTypeId();
        Issue issue = issueEvent.getIssue();
        System.out.println("Processing event type: {} for issue: {}");
        if (eventTypeId.equals(EventType.ISSUE_CREATED_ID)) {
            System.out.println("Issue {} has been created at {}.");

            ApplicationUser creator = issue.getCreator();

            if (creator != null) {
                commentManager.create(issue, creator, "This issue was created by " + creator.getDisplayName() + ".", true);
            }

            if (isCriticalIssue(issue)) {
                sendCriticalIssueNotification(issue);
            }


        } else if (eventTypeId.equals(EventType.ISSUE_RESOLVED_ID)) {
            System.out.println("Issue {} has been resolved at {}.");
        } else if (eventTypeId.equals(EventType.ISSUE_CLOSED_ID)) {
            System.out.println("Issue {} has been closed at {}.");
        }
    }


    private boolean isCriticalIssue(Issue issue) {
        System.out.println("asdasdasdasdasdsad: " + issue.getPriority().getName());
        return "Highest".equals(issue.getPriority().getName()) ||
                "Critical".equals(issue.getPriority().getName());
    }

    // Add this new method to your class:
    private void sendCriticalIssueNotification(Issue issue) {
        try {
            // Prepare the message payload
            Map<String, Object> message = new HashMap<>();
            message.put("title", "Critical Issue Created: " + issue.getKey());
            message.put("content", String.format("A critical issue has been created:%n" +
                            "Key: %s%n" +
                            "Summary: %s%n" +
                            "Priority: %s%n" +
                            "Creator: %s",
                    issue.getKey(),
                    issue.getSummary(),
                    issue.getPriority().getName(),
                    issue.getCreator().getDisplayName()));

            // Prepare the full request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", message);
            payload.put("topicArn", snsTopicArn);
            System.out.println("topicArn: " + snsTopicArn);
            System.out.println("apiEndpoint: " + awsConfig.getMessagesUrl());
            System.out.println("x-api-key: " + apiKey);
            // Convert payload to JSON
            String jsonPayload = gson.toJson(payload);

            // Create and execute request
//            Request request = requestFactory.createRequest(Request.MethodType.POST, awsConfig.getMessagesUrl());
//            request.setHeader("Content-Type", "application/json");
//            request.setHeader("x-api-key", apiKey);
//            request.setRequestBody(jsonPayload);
//
//            String response = request.execute();
            String response = awsConfig.invokeApi(awsConfig.getMessagesUrl(), String.valueOf(Request.MethodType.POST), jsonPayload);
            System.out.println("API Response: " + response);
        } catch (Exception e) {
            System.out.println("Error sending critical issue notification: " + e);
        }
    }
}
