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
package com.ericsson.oss.services.shm.webpush.notifications.impl;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.notification.event.AttributeChangeData;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsAttributeChangedEvent;
import com.ericsson.oss.itpf.datalayer.dps.notification.event.DpsObjectCreatedEvent;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.loadcontrol.local.api.SHMLoadControllerLocalService;
import com.ericsson.oss.services.shm.webpush.api.WebPushConstants;
import com.ericsson.oss.services.shm.webpush.utils.WebPushServiceUtil;

public class JobNotificationUtil {
    private static final Logger logger = LoggerFactory.getLogger(JobNotificationUtil.class);

    @Inject
    WebPushServiceUtil webPushServiceUtil;

    @Inject
    SHMLoadControllerLocalService shmLoadControllerService;

    /**
     * This method retrieves the type of Job from DPS attribute change event and notifies application to update.
     * 
     * @param dpsAttributeChangedEvent
     *            payLoad
     */
    public void notifyAsUpdate(final DpsAttributeChangedEvent dpsAttributeChangedEvent) {

        logger.trace("Payload is instance of DpsAttributeChangedEvent : {}", dpsAttributeChangedEvent);
        final String typeOfJob = dpsAttributeChangedEvent.getType();
        final long poId = dpsAttributeChangedEvent.getPoId();
        final Set<AttributeChangeData> attributeChangeData = dpsAttributeChangedEvent.getChangedAttributes();

        switch (typeOfJob) {
        case WebPushConstants.JOB_KIND:
            webPushServiceUtil.prepareAndPushMainJob(poId);
            break;
        case WebPushConstants.NE_JOB_KIND:
            webPushServiceUtil.prepareAndPushNeJob(poId);
            break;
        case WebPushConstants.ACTIVITY_JOB_KIND:
            webPushServiceUtil.prepareAndPushActivityJob(poId, attributeChangeData);
            deleteStagedPOs(poId, attributeChangeData);
            break; 
        default:
            logger.debug("Some other PO's notification came which is extending AbstractJob other than above mentioned three. Type of Job : {}", typeOfJob);
        }
    }
    
    private void deleteStagedPOs(final long poId, final Set<AttributeChangeData> attributeChangeData) {
        for (final AttributeChangeData everyAttributeChange : attributeChangeData) {
            if (ShmConstants.ACTIVITY_RESULT.equals(everyAttributeChange.getName())) {
                shmLoadControllerService.deleteShmStageActivity(poId);
                break;
            }else{
            logger.debug("deleteShmStageActivity is Skipped");
            }
        }
    } 
    /**
     * This method retrieves the type of Job from DPS attribute change event and notifies application to create. * @param dpsAttributeChangedEvent payLoad
     */
    public void notifyAsCreate(final DpsObjectCreatedEvent dpsObjectCreatedEvent) {

        logger.trace("Payload is instance of DpsObjectCreatedEvent : {}", dpsObjectCreatedEvent);
        final long poId = dpsObjectCreatedEvent.getPoId();
        final String typeOfJob = dpsObjectCreatedEvent.getType();
        switch (typeOfJob) {
        case WebPushConstants.JOB_KIND:
            webPushServiceUtil.prepareAndPushCreateJobEvent(poId);
            break;
        case WebPushConstants.NE_JOB_KIND:
            webPushServiceUtil.prepareAndPushCreateNeJobEvent(poId);
            break;
        default:
            logger.debug("Creation of Some other Job PO's notification came. Type of Job : {}", typeOfJob);
        }
    }
}
