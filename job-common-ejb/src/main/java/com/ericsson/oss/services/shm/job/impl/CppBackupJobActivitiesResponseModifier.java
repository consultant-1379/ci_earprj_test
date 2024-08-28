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
package com.ericsson.oss.services.shm.job.impl;

import java.util.Iterator;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.activity.Activity;
import com.ericsson.oss.services.shm.job.activity.NeActivityInformation;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * To Retrieve the Activity information and update it to the Response.
 * 
 * @author xmalsru
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = com.ericsson.oss.services.shm.job.activity.JobType.BACKUP)
public class CppBackupJobActivitiesResponseModifier implements ManageBackupActivitiesResponseModifier {

    private static final Logger logger = LoggerFactory.getLogger(CppBackupJobActivitiesResponseModifier.class);

    /**
     * Returns the activities needed for ManageBackup
     * 
     * @param jobActivitiesQuery
     * @param jobActivitiesResponse
     * @return
     */
    @Override
    public JobActivitiesResponse getManageBackupActivities(final JobActivitiesResponse jobActivitiesResponse, final Boolean multipleBackups) {
        logger.info("Json Modification will start.");
        for (final NeActivityInformation neActivityInformation : jobActivitiesResponse.getNeActivityInformation()) {
            for (final Iterator<Activity> activityIter = neActivityInformation.getActivity().listIterator(); activityIter.hasNext();) {
                final String activityName = activityIter.next().getName();
                logger.debug("ActivityName = {}", activityName);
                if ((ShmConstants.CREATE_CV__ACTIVITY.equals(activityName))
                        || ((multipleBackups) && (ShmConstants.SET_STARTABLE__ACTIVITY.equals(activityName) || ShmConstants.SET_FIRST_IN_THE_ROLLBACKLIST_ACTIVITY.equals(activityName)))) {
                    activityIter.remove();
                }
            }
        }
        return jobActivitiesResponse;
    }

}
