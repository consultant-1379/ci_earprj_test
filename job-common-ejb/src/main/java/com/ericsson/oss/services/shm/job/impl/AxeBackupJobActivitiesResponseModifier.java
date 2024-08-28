/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
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
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.activity.NeActivityInformation;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.api.JobActivitiesResponse;
import com.ericsson.oss.services.shm.jobs.common.api.NeInfoQuery;
import com.ericsson.oss.services.shm.jobs.common.api.SHMJobActivitiesResponseModifier;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * To Retrieve the Node information and updates it to the Response.
 * 
 * @author xprapav
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.AXE, jobType = JobType.BACKUP)
public class AxeBackupJobActivitiesResponseModifier implements SHMJobActivitiesResponseModifier, ManageBackupActivitiesResponseModifier {

    private static final Logger logger = LoggerFactory.getLogger(AxeBackupJobActivitiesResponseModifier.class);

    /**
     * Returns the activities needed for ManageBackup
     * 
     * @param jobActivitiesQuery
     * @param jobActivitiesResponse
     * @return
     */
    @Override
    public JobActivitiesResponse getManageBackupActivities(final JobActivitiesResponse jobActivitiesResponse, final Boolean multipleBackups) {
        logger.debug("Json Modification starting for {}", jobActivitiesResponse.getNeActivityInformation());
        for (final NeActivityInformation neActivityInformation : jobActivitiesResponse.getNeActivityInformation()) {
            for (final Iterator<Activity> activityIter = neActivityInformation.getActivity().listIterator(); activityIter.hasNext();) {
                if (ShmConstants.CREATE_BACKUP.equals(activityIter.next().getName())) {
                    activityIter.remove();
                }
            }
        }
        return jobActivitiesResponse;
    }

    @Override
    public JobActivitiesResponse getUpdatedJobActivities(final NeInfoQuery neInfoQuery, final JobActivitiesResponse jobActivitiesResponse) {
        return null;
    }
}
