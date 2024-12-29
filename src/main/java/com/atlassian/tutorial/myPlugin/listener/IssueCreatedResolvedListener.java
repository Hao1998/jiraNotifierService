package com.atlassian.tutorial.myPlugin.listener;


import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class IssueCreatedResolvedListener implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(IssueCreatedResolvedListener.class);
    private final CommentManager commentManager = ComponentAccessor.getCommentManager();

    @JiraImport
    private final EventPublisher eventPublisher;

    @Autowired
    public IssueCreatedResolvedListener(EventPublisher eventPublisher1) {
        log.info("Constructing IssueCreatedResolvedListener");
        this.eventPublisher = eventPublisher1;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Enabling plugin");
        eventPublisher.register(this);
    }


    @Override
    public void destroy() throws Exception {
        log.info("Disabling plugin");
        eventPublisher.unregister(this);
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        Long eventTypeId = issueEvent.getEventTypeId();
        Issue issue = issueEvent.getIssue();
        log.info("Processing event type: {} for issue: {}", eventTypeId, issue.getKey());
        if (eventTypeId.equals(EventType.ISSUE_CREATED_ID)) {
            log.info("Issue {} has been created at {}.", issue.getKey(), issue.getCreated());

            ApplicationUser creator = issue.getCreator();

            if (creator != null) {
                commentManager.create(issue, creator, "This issue was created by " + creator.getDisplayName() + ".", true);
            }

//            if (isCriticalIssue(issue)) {
//                String message = String.format("Critical Issue Created: [%s] %s",
//                        issue.getKey(),
//                        issue.getSummary());
//
//                PublishRequest publishRequest = new PublishRequest()
//                        .withTopicArn(snsTopicArn)
//                        .withMessage(message);
//                snsClient.publish(publishRequest);
//                log.info("Notification sent for critical issue: {}", issue.getKey());
//            }


        } else if (eventTypeId.equals(EventType.ISSUE_RESOLVED_ID)) {
            log.info("Issue {} has been resolved at {}.", issue.getKey(), issue.getResolutionDate());
        } else if (eventTypeId.equals(EventType.ISSUE_CLOSED_ID)) {
            log.info("Issue {} has been closed at {}.", issue.getKey(), issue.getUpdated());
        }
    }


    private boolean isCriticalIssue(Issue issue) {
        return "Highest".equals(issue.getPriority().getName()) ||
                "Critical".equals(issue.getPriority().getName());
    }
}
