/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.webpush;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.webpush.api.JobWebPushEvent;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;
import com.ericsson.oss.uisdk.restsdk.webpush.api.WebPushClient;
import com.ericsson.oss.uisdk.restsdk.webpush.api.WebPushEndpoint;
import com.ericsson.oss.uisdk.restsdk.webpush.api.WebPushRestEvent;
import com.ericsson.oss.uisdk.restsdk.webpush.api.exception.WebPushBroadcastException;
import com.ericsson.oss.uisdk.restsdk.webpush.api.impl.WebPushRestEventImpl;

/**
 * This class provides the facility for publishing channels, pushing events and unpublishing channels.
 * 
 * @author xcharoh
 */

@Singleton
@Lock(LockType.READ)
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Startup
@Profiled
@Traceable
public class ShmWebPushHandler {

    private static final Logger logger = LoggerFactory.getLogger(ShmWebPushHandler.class);

    //Three different web push clients are required for shm and shmJobDetails page.
    @Inject
    @WebPushEndpoint(resourceUrn = WebPushConstants.SHM_RESOURCE, channelName = WebPushConstants.SHM_CHANNEL)
    private WebPushClient clientForShmPage;

    @Inject
    @WebPushEndpoint(resourceUrn = WebPushConstants.SHM_JOB_DETAILS_RESOURCE, channelName = WebPushConstants.SHM_JOB_DETAILS_CHANNEL)
    private WebPushClient clientForShmJobDetailsPage;

    @Inject
    @WebPushEndpoint(resourceUrn = WebPushConstants.SHM_JOB_RESOURCE, channelName = WebPushConstants.SHM_JOB_CREATE_DELETE_CHANNEL)
    private WebPushClient clientForShmJobCreationAndDeletion;

    @Inject
    @WebPushEndpoint(resourceUrn = WebPushConstants.SHM_JOB_LOGS_RESOURCE, channelName = WebPushConstants.SHM_JOB_LOGS_CHANNEL)
    private WebPushClient clientForShmJobLogsPage;

    public void pushToShmPage(final JobWebPushEvent jobWebPushEvent) throws WebPushBroadcastException {
        logger.debug("Pushing event to shmPage");
        pushAsEvent(clientForShmPage, jobWebPushEvent.getAttributeMap(), jobWebPushEvent.getJobId());
    }

    public void pushToShmJobDetailsPage(final JobWebPushEvent jobWebPushEvent) throws WebPushBroadcastException {
        logger.debug("Pushing event to shmJobDetailsPage");
        pushAsEvent(clientForShmJobDetailsPage, jobWebPushEvent.getAttributeMap(), jobWebPushEvent.getJobId());
    }

    public void pushToShmJobsPage(final JobWebPushEvent jobWebPushEvent) throws WebPushBroadcastException {
        logger.debug("Pushing create/delete events to shmJobsPage");
        final String jobId = "";
        pushAsEvent(clientForShmJobCreationAndDeletion, jobWebPushEvent.getAttributeMap(), jobId);
    }

    public void pushToShmJobLogsPage(final JobWebPushEvent jobWebPushEvent) throws WebPushBroadcastException {
        logger.debug("Pushing event to  shmJobLogsPage");
        pushAsEvent(clientForShmJobLogsPage, jobWebPushEvent.getAttributeMap(), jobWebPushEvent.getJobId());
    }

    /**
     * This private method is used for push events to the respective clients.
     * 
     * @param webPushClient
     * @param jsonObject
     * @param jobId
     * @return
     * @throws WebPushBroadcastException
     */
    private void pushAsEvent(final WebPushClient webPushClient, final Map<String, Object> attributeMap, final String jobId) throws WebPushBroadcastException {

        final WebPushRestEvent event = new WebPushRestEventImpl((Serializable) attributeMap);
        final Map<String, String> filterableAttributes = new HashMap<String, String>();
        if (jobId != null && !("".equals(jobId))) {
            filterableAttributes.put(WebPushConstants.JOB_ID, jobId);
        }

        webPushClient.broadcast(event, filterableAttributes);

    }

}
