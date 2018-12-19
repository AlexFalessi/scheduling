/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.scheduler.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.ow2.proactive.addons.email.exception.EmailException;
import org.ow2.proactive.resourcemanager.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.TaskState;
import org.ow2.proactive.scheduler.core.db.SchedulerDBManager;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scheduler.util.JobLogger;
import org.ow2.proactive.scheduler.util.SendMail;


public class JobEmailNotification {

    public static final String GENERIC_INFORMATION_KEY_EMAIL = "EMAIL";

    public static final String GENERIC_INFORMATION_KEY_NOTIFICATION_EVENT = "NOTIFICATION_EVENTS";

    private static final Logger logger = Logger.getLogger(JobEmailNotification.class);

    private static final JobLogger jlogger = JobLogger.getInstance();

    private static final String SUBJECT_TEMPLATE = "ProActive Job %s : %s";

    private static final String BODY_TEMPLATE = "Job ID: %s\n" + "New Status: %s\n\n" + "--\n" + "Job Details: \n" +
                                                "%s" + "\n\n" + "--\n" +
                                                "This email was auto-generated by ProActive Scheduling\n" +
                                                "Hostname: %s";

    private JobState jobState;

    private SchedulerEvent eventType;

    private SendMail sender;

    private final ExecutorService asyncMailSender;

    private SchedulerDBManager dbManager = null;

    public JobEmailNotification(JobState js, NotificationData<JobInfo> notification, SendMail sender) {
        this.asyncMailSender = Executors.newCachedThreadPool();
        this.jobState = js;
        this.eventType = notification.getEventType();
        this.sender = sender;
    }

    public JobEmailNotification(JobState js, NotificationData<JobInfo> notification) {
        this(js, notification, new SendMail());
    }

    public JobEmailNotification(JobState js, NotificationData<JobInfo> notification, SchedulerDBManager dbManager) {
        this(js, notification);
        this.dbManager = dbManager;
    }

    public boolean doCheckAndSend(boolean withAttachment)
            throws JobEmailNotificationException, IOException, UnknownJobException, PermissionException {
        String jobStatus = jobState.getGenericInformation().get(GENERIC_INFORMATION_KEY_NOTIFICATION_EVENT);
        List<String> jobStatusList = new ArrayList<>();
        if (jobStatus != null) {
            if ("all".equals(jobStatus.toLowerCase())) {
                jobStatusList = Arrays.asList("JOB_CHANGE_PRIORITY",
                                              "JOB_IN_ERROR",
                                              "JOB_PAUSED",
                                              "JOB_PENDING_TO_FINISHED",
                                              "JOB_PENDING_TO_RUNNING",
                                              "JOB_RESTARTED_FROM_ERROR",
                                              "JOB_RESUMED",
                                              "JOB_RUNNING_TO_FINISHED",
                                              "JOB_SUBMITTED")
                                      .stream()
                                      .map(status -> status.toLowerCase())
                                      .collect(Collectors.toList());
            } else {
                jobStatusList = Arrays.asList(jobStatus.toLowerCase().split("\\s*,\\s*"));
            }
        }

        switch (eventType) {
            case JOB_CHANGE_PRIORITY:
            case JOB_IN_ERROR:
            case JOB_PAUSED:
            case JOB_PENDING_TO_FINISHED:
            case JOB_PENDING_TO_RUNNING:
            case JOB_RESTARTED_FROM_ERROR:
            case JOB_RESUMED:
            case JOB_RUNNING_TO_FINISHED:
            case JOB_SUBMITTED:
                break;
            default:
                logger.trace("Event not in the list of email notification, doing nothing");
                return false;
        }
        if (!PASchedulerProperties.EMAIL_NOTIFICATIONS_ENABLED.getValueAsBoolean()) {
            logger.debug("Notification emails disabled, doing nothing");
            return false;
        }
        if (!jobStatusList.contains(eventType.toString().toLowerCase())) {
            return false;
        }

        try {
            if (withAttachment) {
                String attachment = getAttachment();
                if (attachment != null) {
                    sender.sender(getTo(), getSubject(), getBody(), attachment, getAttachmentName());
                    FileUtils.deleteQuietly(new File(attachment));
                } else {
                    sender.sender(getTo(), getSubject(), getBody());
                }
            } else {
                sender.sender(getTo(), getSubject(), getBody());
            }
            return true;
        } catch (EmailException e) {
            throw new JobEmailNotificationException(String.join(",", getTo()),
                                                    "Error sending email: " + e.getMessage(),
                                                    e);
        }
    }

    public void checkAndSendAsync(boolean withAttachment) {
        this.asyncMailSender.submit(() -> {
            try {
                boolean sent = doCheckAndSend(withAttachment);
                if (sent) {
                    jlogger.info(jobState.getId(), "sent notification email for finished job to " + getTo());
                }
            } catch (JobEmailNotificationException e) {
                jlogger.warn(jobState.getId(),
                             "failed to send email notification to " + e.getEmailTarget() + ": " + e.getMessage());
                logger.trace("Stack trace:", e);
            } catch (Exception e) {
                jlogger.warn(jobState.getId(), "failed to send email notification: " + e.getMessage());
                logger.trace("Stack trace:", e);
            }
        });
    }

    private static String getFrom() throws JobEmailNotificationException {
        String from = PASchedulerProperties.EMAIL_NOTIFICATIONS_SENDER_ADDRESS.getValueAsString();
        if (from == null || from.isEmpty()) {
            throw new JobEmailNotificationException("Sender address not set in scheduler configuration");
        }
        return from;
    }

    private List<String> getTo() throws JobEmailNotificationException {
        String to = jobState.getGenericInformation().get(GENERIC_INFORMATION_KEY_EMAIL);
        if (to == null) {
            throw new JobEmailNotificationException("Recipient address is not set in generic information");
        }
        String[] toList = to.split("\\s*,\\s*");
        return Arrays.asList(toList);
    }

    private String getSubject() {
        String jobID = jobState.getId().value();
        String event = eventType.toString();
        return String.format(SUBJECT_TEMPLATE, jobID, event);
    }

    private String getBody() {
        String jobID = jobState.getId().value();
        String status = jobState.getStatus().toString();
        String hostname = "UNKNOWN";
        List<TaskState> tasks = jobState.getTasks();
        String allTaskStatusesString = String.join(System.lineSeparator(),
                                                   tasks.stream()
                                                        .map(task -> task.getId().getReadableName() + " (" +
                                                                     task.getId().toString() + ") Status: " +
                                                                     task.getStatus().toString())
                                                        .collect(Collectors.toList()));
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            logger.debug("Could not get hostname", e);
        }
        return String.format(BODY_TEMPLATE, jobID, status, allTaskStatusesString, hostname);
    }

    private String getAttachment() throws NotConnectedException, UnknownJobException, PermissionException, IOException {
        JobId jobID = jobState.getId();
        List<TaskState> tasks = jobState.getTasks();
        String attachLogPath = null;

        try {

            JobResult result = dbManager.loadJobResult(jobID);

            Stream<TaskResult> preResult = tasks.stream().map(task -> result.getAllResults()
                                                                            .get(task.getId().getReadableName()));

            Stream<TaskResult> resNonNull = preResult.filter(r -> r != null && r.getOutput() != null);

            Stream<String> resStream = resNonNull.map(taskResult -> "Task " + taskResult.getTaskId().toString() + " (" +
                                                                    taskResult.getTaskId().getReadableName() + ") :" +
                                                                    System.lineSeparator() +
                                                                    taskResult.getOutput().getAllLogs());

            String allRes = String.join(System.lineSeparator(),
                                        resStream.filter(r -> r != null).collect(Collectors.toList()));

            File file = File.createTempFile("job_logs", ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(allRes);
                attachLogPath = file.getAbsolutePath();
            } catch (IOException e) {
                jlogger.warn(jobState.getId(), "Failed to create attachment for email notification: " + e.getMessage());
                logger.warn("Error creating attachment for email notification: " + e.getMessage(), e);

            }
        } catch (Exception e) {
            logger.warn("Error creating attachment for email notification: ", e);
        }

        return attachLogPath;
    }

    private String getAttachmentName() {
        JobId jobID = jobState.getId();
        String fileName = "job_" + jobID.value() + "_log.txt";
        return fileName;
    }

}
