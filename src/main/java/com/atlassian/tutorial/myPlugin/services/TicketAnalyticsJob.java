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
import com.google.gson.Gson;

import javax.annotation.Nullable;


import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class TicketAnalyticsJob implements JobRunner {

    private final SearchService searchService;
    private final JqlQueryParser jqlQueryParser;
    private final RequestFactory requestFactory;
    private final String apiGatewayUrl;
    private final String apiKey;


    public TicketAnalyticsJob(@ComponentImport SearchService searchService,
                              @ComponentImport JqlQueryParser jqlQueryParser,
                              @ComponentImport RequestFactory requestFactory,
                              String apiGatewayUrl,
                              String apiKey) {
        this.searchService = searchService;
        this.jqlQueryParser = jqlQueryParser;
        this.requestFactory = requestFactory;
        this.apiGatewayUrl = apiGatewayUrl;
        this.apiKey = apiKey;
    }

    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
        try {
            ApplicationUser adminUser = ComponentAccessor.getUserManager().getUserByKey("admin");

            // Get all projects
            Collection<String> projectKeys = getProjectKeys();

            for (String projectKey : projectKeys) {
                try {
                    // Fetch and send tickets for each project
                    sendProjectTickets(projectKey, adminUser);
                } catch (Exception e) {
                    System.out.println("Error processing project: " + projectKey + " - " + e.getMessage());
                }
            }

            return JobRunnerResponse.success();

        } catch (Exception e) {
            System.out.println("Job failed: " + e.getMessage());
            return JobRunnerResponse.failed(e);
        }
    }

    private void sendProjectTickets(String projectKey, ApplicationUser adminUser) throws Exception {
        // Fetch tickets
        List<Map<String, Object>> tickets = fetchTickets(projectKey, adminUser);

        // Prepare payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("projectId", projectKey);
        payload.put("tickets", tickets);

        // Convert payload to JSON
        String jsonPayload = new Gson().toJson(payload);

        // Create and configure request
        Request request = requestFactory.createRequest(Request.MethodType.POST, apiGatewayUrl);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("x-api-key", apiKey);
        request.setRequestBody(jsonPayload);

        // Send Request
        try {
            String response = request.execute();
            System.out.println("API Gateway response: " + response);
        } catch (Exception e) {
            System.out.println("Failed to send data to API Gateway: " + e.getMessage());
            throw e;
        }
    }

    private String getSeverityFromIssue(Issue issue) {
        // Try different common field names for severity
        String[] possibleFieldNames = {
                "severity",
                "priority",
                "Severity",
                "Priority",
                "Impact"
        };

        for (String fieldName : possibleFieldNames) {
            Object fieldValue = issue.getCustomFieldValue(
                    ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(fieldName)
            );

            if (fieldValue != null) {
                return fieldValue.toString();
            }
        }

        // Default to priority if no severity field found
        return issue.getPriority().getName();
    }


    private List<Map<String, Object>> fetchTickets(String projectKey, ApplicationUser adminUser) throws Exception {
        // Build JQL query
        String jql = "project = " + projectKey;
        Query query = jqlQueryParser.parseQuery(jql);
        SearchResults<Issue> results = searchService.search(adminUser,
                query,
                PagerFilter.getUnlimitedFilter());

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
