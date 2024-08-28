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
package com.ericsson.oss.services.shm.job.service.cpp.backup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.remote.api.NeTypePropertiesProvider;
import com.ericsson.oss.services.shm.job.remote.api.ShmBackupJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.impl.NeTypePropertiesHelper;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;

/**
 * 
 * @author xnagvar
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = com.ericsson.oss.services.shm.job.activity.JobType.BACKUP)
public class CppBackupNeTypePropertiesProvider implements NeTypePropertiesProvider {

    private static final String CV_NAME = "CV_NAME";

    private static final String CV_TYPE = "CV_TYPE";

    private static final String CV_COMMENT = "CV_COMMENT";

    private static final String STANDARD = "STANDARD";

    private static final String GENERATE_BACKUP_NAME = "GENERATE_BACKUP_NAME";

    @Inject
    NeTypePropertiesHelper neTypePropertiesHelper;

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.job.remote.api.NeTypePropertiesProvider#getNeTypeProperties(java.util.List)
     */
    @Override
    public List<Map<String, Object>> getNeTypeProperties(final List<String> activityProperties, final ShmRemoteJobData shmRemoteJobData) {
        final ShmBackupJobData shmBackupJobData = (ShmBackupJobData) shmRemoteJobData;

        final List<Map<String, Object>> propertieslist = new ArrayList<Map<String, Object>>();
        propertieslist.add(neTypePropertiesHelper.createPropertyMap(CV_NAME, shmBackupJobData.getBackupName()));
        if (shmBackupJobData.getBackupType() == null) {
            propertieslist.add(neTypePropertiesHelper.createPropertyMap(CV_TYPE, getDefaultBackupType()));
        } else {
            propertieslist.add(neTypePropertiesHelper.createPropertyMap(CV_TYPE, shmBackupJobData.getBackupType()));
        }
        propertieslist.add(neTypePropertiesHelper.createPropertyMap(GENERATE_BACKUP_NAME, shmBackupJobData.isAutoGenerateBackupName()));
        if (shmBackupJobData.getBackupComment() != null) {
            propertieslist.add(neTypePropertiesHelper.createPropertyMap(CV_COMMENT, shmBackupJobData.getBackupComment()));
        }
        return neTypePropertiesHelper.getNeTypeProperties(activityProperties, propertieslist);
    }

    private static String getDefaultBackupType() {
        return STANDARD;
    }

}
