/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
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
import com.ericsson.oss.services.shm.webpush.api.JobWebPushEvent;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;
import com.ericsson.oss.uisdk.restsdk.webpush.api.WebPushClient;
import com.ericsson.oss.uisdk.restsdk.webpush.api.WebPushEndpoint;
import com.ericsson.oss.uisdk.restsdk.webpush.api.WebPushRestEvent;
import com.ericsson.oss.uisdk.restsdk.webpush.api.exception.WebPushBroadcastException;
import com.ericsson.oss.uisdk.restsdk.webpush.api.impl.WebPushRestEventImpl;

@Singleton
@Lock(LockType.READ)
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Startup
@Profiled
public class NhcWebPushHandler {

    private static final Logger logger = LoggerFactory.getLogger(NhcWebPushHandler.class);

    @Inject
    @WebPushEndpoint(resourceUrn = WebPushConstants.NHC_RESOURCE, channelName = WebPushConstants.NHC_CHANNEL)
    private WebPushClient clientForNHCPage;

    @Inject
    @WebPushEndpoint(resourceUrn = WebPushConstants.NHC_JOB_DETAILS_RESOURCE, channelName = WebPushConstants.NHC_JOB_DETAILS_CHANNEL)
    private WebPushClient clientForNHCJobDetailsPage;

    @Inject
    @WebPushEndpoint(resourceUrn = WebPushConstants.NHC_JOB_RESOURCE, channelName = WebPushConstants.NHC_JOB_CREATE_DELETE_CHANNEL)
    private WebPushClient clientForNHCJobCreationAndDeletion;

    @Inject
    @WebPushEndpoint(resourceUrn = WebPushConstants.NHC_JOB_LOGS_RESOURCE, channelName = WebPushConstants.NHC_JOB_LOGS_CHANNEL)
    private WebPushClient clientForNHCJobLogsPage;

    public void pushToNhcPage(final JobWebPushEvent jobWebPushEvent) throws WebPushBroadcastException {
        logger.debug("Pushing event to NHCPage");
        pushAsEvent(clientForNHCPage, jobWebPushEvent.getAttributeMap(), jobWebPushEvent.getJobId());
    }

    public void pushToNhcJobDetailsPage(final JobWebPushEvent jobWebPushEvent) throws WebPushBroadcastException {
        logger.debug("Pushing event to NHCJobDetailsPage");
        pushAsEvent(clientForNHCJobDetailsPage, jobWebPushEvent.getAttributeMap(), jobWebPushEvent.getJobId());
    }

    public void pushToNhcJobsPage(final JobWebPushEvent jobWebPushEvent) throws WebPushBroadcastException {
        logger.debug("Pushing create/delete events to NHCJobsPage");
        final String jobId = "";
        pushAsEvent(clientForNHCJobCreationAndDeletion, jobWebPushEvent.getAttributeMap(), jobId);
    }

    public void pushToNhcJobLogsPage(final JobWebPushEvent jobWebPushEvent) throws WebPushBroadcastException {
        logger.debug("Pushing event to  NHCJobLogsPage");
        pushAsEvent(clientForNHCJobLogsPage, jobWebPushEvent.getAttributeMap(), jobWebPushEvent.getJobId());
    }

    private void pushAsEvent(final WebPushClient webPushClient, final Map<String, Object> attributeMap, final String jobId) throws WebPushBroadcastException {

        final WebPushRestEvent event = new WebPushRestEventImpl((Serializable) attributeMap);
        final Map<String, String> filterableAttributes = new HashMap();
        if (jobId != null && !(jobId.isEmpty())) {
            filterableAttributes.put(WebPushConstants.JOB_ID, jobId);
        }
        webPushClient.broadcast(event, filterableAttributes);
    }

}
