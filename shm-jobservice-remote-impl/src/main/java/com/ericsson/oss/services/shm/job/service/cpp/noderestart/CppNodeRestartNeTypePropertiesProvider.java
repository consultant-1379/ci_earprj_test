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
package com.ericsson.oss.services.shm.job.service.cpp.noderestart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.job.remote.api.NeTypePropertiesProvider;
import com.ericsson.oss.services.shm.job.remote.api.ShmNodeRestartJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.impl.NeTypePropertiesHelper;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * 
 * @author xnagvar
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = com.ericsson.oss.services.shm.job.activity.JobType.NODERESTART)
public class CppNodeRestartNeTypePropertiesProvider implements NeTypePropertiesProvider {

    private static final String RESTART_RANK = "restartRank";

    private static final String RESTART_REASON = "restartReason";

    private static final String RESTART_INFO = "restartInfo";

    @Inject
    NeTypePropertiesHelper neTypePropertiesHelper;

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.job.remote.api.NeTypePropertiesProvider#getNeTypeProperties(java.util.List, com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData)
     */
    @Override
    public List<Map<String, Object>> getNeTypeProperties(final List<String> activityProperties, final ShmRemoteJobData shmRemoteJobData) {
        final ShmNodeRestartJobData shmNodeRestartJobData = (ShmNodeRestartJobData) shmRemoteJobData;

        final List<Map<String, Object>> propertieslist = new ArrayList<Map<String, Object>>();
        propertieslist.add(neTypePropertiesHelper.createPropertyMap(RESTART_RANK, shmNodeRestartJobData.getRestartRank()));
        propertieslist.add(neTypePropertiesHelper.createPropertyMap(RESTART_REASON, shmNodeRestartJobData.getRestartReason()));
        if (shmNodeRestartJobData.getRestartInfo() != null) {
            propertieslist.add(neTypePropertiesHelper.createPropertyMap(RESTART_INFO, shmNodeRestartJobData.getRestartInfo()));
        } else {
            propertieslist.add(neTypePropertiesHelper.createPropertyMap(RESTART_INFO, ShmConstants.DEFAULTVALUE_RESTART_INFO));
        }
        return neTypePropertiesHelper.getNeTypeProperties(activityProperties, propertieslist);
    }

}
