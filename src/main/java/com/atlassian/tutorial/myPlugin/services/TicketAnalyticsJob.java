package com.atlassian.tutorial.myPlugin.services;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;

import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;

import com.atlassian.sal.api.net.Response;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.tutorial.myPlugin.config.AwsConfig;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;


import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Component
public class TicketAnalyticsJob implements JobRunner {


    private final SearchService searchService;
    private final JqlQueryParser jqlQueryParser;
    private final RequestFactory requestFactory;
    private final AwsConfig awsConfig;


//    @Value("${aws.api.endpoint.analytic}")
//    private String apiGatewayUrl;
//
//    @Value("${aws.api.key}")
//    private String apiKey;


    public TicketAnalyticsJob(@ComponentImport SearchService searchService,
                              @ComponentImport JqlQueryParser jqlQueryParser,
                              @ComponentImport RequestFactory requestFactory,
                              AwsConfig awsConfig) {
        this.searchService = searchService;
        this.jqlQueryParser = jqlQueryParser;
        this.requestFactory = requestFactory;
        this.awsConfig = awsConfig;

        // Log initialization
        System.out.println("TicketAnalyticsJob initialized with API Gateway URL: ");
    }

    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
        System.out.println("Starting TicketAnalyticsJob at: " + LocalDateTime.now());
        try {
            ApplicationUser adminUser = ComponentAccessor.getUserManager().getUserByKey("admin");
            System.out.println("Retrieved admin user: " + (adminUser != null ? adminUser.getDisplayName() : "null"));

            // Get all projects
            Collection<String> projectKeys = getProjectKeys();
            System.out.println("Found " + projectKeys.size() + " projects to process");

            int processedProjects = 0;
            int failedProjects = 0;

            for (String projectKey : projectKeys) {
                System.out.println("\nProcessing project: " + projectKey);
                try {
                    // Fetch and send tickets for each project
                    sendProjectTickets(projectKey, adminUser);
                    processedProjects++;
                    System.out.println("Successfully processed project: " + projectKey);
                } catch (Exception e) {
                    failedProjects++;
                    System.out.println("Error processing project: " + projectKey);
                    System.out.println("Error details: " + e.getMessage());
                    e.printStackTrace();;
                }
            }

            System.out.println("\nJob completion summary:");
            System.out.println("Total projects: " + projectKeys.size());
            System.out.println("Successfully processed: " + processedProjects);
            System.out.println("Failed: " + failedProjects);

            return JobRunnerResponse.success();

        } catch (Exception e) {
            System.out.println("Job failed: " + e.getMessage());
            return JobRunnerResponse.failed(e);
        }
    }

    private void sendProjectTickets(String projectKey, ApplicationUser adminUser) throws Exception {
        System.out.println("Fetching tickets for project: " + projectKey);

        // Fetch tickets
        List<Map<String, Object>> tickets = fetchTickets(projectKey, adminUser);
        System.out.println("Fetched " + tickets.size() + " tickets for project: " + projectKey);

        // Prepare payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("projectId", projectKey);
        payload.put("tickets", tickets);

        // Convert payload to JSON
        String jsonPayload = new Gson().toJson(payload);
        System.out.println("Prepared payload size (bytes): " + jsonPayload.getBytes().length);

        // Create and configure request
        System.out.println("Sending request to API Gateway: " + awsConfig.getBaseUrl());
        Request request = requestFactory.createRequest(Request.MethodType.POST, awsConfig.getAnalyticsUrl());
        request.setHeader("Content-Type", "application/json");
        request.setHeader("x-api-key", awsConfig.getApiKey());
        request.setRequestBody(jsonPayload);

        // Send Request
        try {
            String response = request.execute();
            System.out.println("API Gateway response: " + response);
        } catch (Exception e) {
            System.out.println("Failed to send data to API Gateway: " + e.getMessage());
            System.out.println("Error details: " + e.getMessage());
            throw e;
        }
    }

    private String getSeverityFromIssue(Issue issue) {
        System.out.println("Getting severity from issue: " + issue.getKey());

        try {
            // First try to get priority (most reliable)
            if (issue.getPriority() != null) {
                String priority = issue.getPriority().getName();
                System.out.println("Using priority value: " + priority);
                return priority;
            }

            // If priority is null, try custom fields
            String[] possibleFieldNames = {
                    "severity",
                    "priority",
                    "Severity",
                    "Priority",
                    "Impact"
            };

            for (String fieldName : possibleFieldNames) {
                try {
                    com.atlassian.jira.issue.fields.CustomField customField =
                            ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(fieldName);

                    if (customField != null) {
                        Object fieldValue = issue.getCustomFieldValue(customField);
                        if (fieldValue != null) {
                            System.out.println("Found value in custom field '" + fieldName + "': " + fieldValue);
                            return fieldValue.toString();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error checking custom field '" + fieldName + "': " + e.getMessage());
                    // Continue to next field
                }
            }

            // If nothing found, return default
            System.out.println("No severity or priority found, using default value: 'Medium'");
            return "Medium";

        } catch (Exception e) {
            System.out.println("Error in getSeverityFromIssue: " + e.getMessage());
            return "Medium";
        }
    }


    private List<Map<String, Object>> fetchTickets(String projectKey, ApplicationUser adminUser) throws Exception {
        System.out.println("Starting ticket fetch for project: " + projectKey);

        // Build JQL query
        String jql = "project = " + projectKey;
        System.out.println("Using JQL query: " + jql);

        Query query = jqlQueryParser.parseQuery(jql);
        SearchResults<Issue> results = searchService.search(adminUser,
                query,
                PagerFilter.getUnlimitedFilter());

        System.out.println("Found " + results.getTotal() + " tickets for project: " + projectKey);

        List<Map<String, Object>> tickets = new ArrayList<>();
        for (Issue issue : results.getResults()) {
            Map<String, Object> ticketData = new HashMap<>();

            ticketData.put("key", issue.getKey());
            ticketData.put("summary", issue.getSummary());
            ticketData.put("status", issue.getStatus().getName());
            ticketData.put("severity", getSeverityFromIssue(issue));
            ticketData.put("createdAt", formatDate(issue.getCreated()));

            if (issue.getResolutionDate() != null) {
                ticketData.put("resolvedAt", formatDate(issue.getResolutionDate()));
            }

            ticketData.put("assignee", issue.getAssignee() != null ?
                    issue.getAssignee().getDisplayName() : null);
            ticketData.put("reporter", issue.getReporter() != null ?
                    issue.getReporter().getDisplayName() : null);

            tickets.add(ticketData);
        }
        return tickets;
    }

    private String formatDate(Timestamp timestamp) {
        return LocalDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC)
                .toString();
    }

    private Collection<String> getProjectKeys() {
        return ComponentAccessor.getProjectManager().getProjects()
                .stream()
                .map(p -> p.getKey())
                .toList();
    }
}
