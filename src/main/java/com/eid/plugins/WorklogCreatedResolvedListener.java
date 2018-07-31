package com.eid.plugins;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class WorklogCreatedResolvedListener implements InitializingBean, DisposableBean {
     private static final Logger log = LoggerFactory.getLogger(WorklogCreatedResolvedListener.class);
     public static final String ACTIVITY_OPALE = "Code Activité OPALE";

     @JiraImport
    private final EventPublisher eventPublisher;

    @Autowired
    public WorklogCreatedResolvedListener(@JiraImport EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        //eventPublisher.register(this);    // Demonstration only -- don't do this in real code!
    }

    /**
     * Called when the plugin has been enabled.
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Enabling plugin");
        eventPublisher.register(this);
    }

    /**
     * Called when the plugin is being disabled or removed.
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        log.info("Disabling plugin");
        eventPublisher.unregister(this);
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) 
    {
        Long eventTypeId = issueEvent.getEventTypeId();
        Issue issue = issueEvent.getIssue();

        if (eventTypeId.equals(EventType.ISSUE_CREATED_ID)) {
            log.info("Issue {} has been created at {}.", issue.getKey(), issue.getCreated());
        } else if (eventTypeId.equals(EventType.ISSUE_RESOLVED_ID)) {
            log.info("Issue {} has been resolved at {}.", issue.getKey(), issue.getResolutionDate());
        } else if (eventTypeId.equals(EventType.ISSUE_CLOSED_ID)) {
            log.info("Issue {} has been closed at {}.", issue.getKey(), issue.getUpdated());
        } else if (eventTypeId.equals(EventType.ISSUE_WORKLOGGED_ID )) {
 
            // Update OPALE
            CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
            CustomField cfOpale = customFieldManager.getCustomFieldObjectByName(ACTIVITY_OPALE);
            String codeActivity = (String)issue.getCustomFieldValue(cfOpale);
            while (codeActivity == null) {
                Issue parent = issue.getParentObject();
                codeActivity = (String)parent.getCustomFieldValue(cfOpale);
                issue = parent;
            }
            CommentManager commentManager = ComponentAccessor.getCommentManager();
            String comment = "Activité OPALE " + codeActivity + " mise à jour du temps passé ";
            String currentUser = currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getUsername();
            commentManager.create(issue, currentUser, comment, true);

            log.info(comment);
        }
    }
}

