/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.service.cpp.deleteupgradepackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.job.remote.api.NeTypePropertiesProvider;
import com.ericsson.oss.services.shm.job.remote.api.ShmDeleteUpgradePkgJobData;
import com.ericsson.oss.services.shm.job.remote.api.ShmRemoteJobData;
import com.ericsson.oss.services.shm.job.remote.impl.NeTypePropertiesHelper;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;

/**
 * 
 * @author xneranu
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = com.ericsson.oss.services.shm.job.activity.JobType.DELETE_UPGRADEPACKAGE)
public class CppDeleteUpgradePkgNeTypePropertiesProvider implements NeTypePropertiesProvider {


    @Inject
    private NeTypePropertiesHelper neTypePropertiesHelper;

    @Override
    public List<Map<String, Object>> getNeTypeProperties(final List<String> activityProperties, final ShmRemoteJobData shmRemoteJobData) {
        final ShmDeleteUpgradePkgJobData shmDeleteUpgradePkgJobData = (ShmDeleteUpgradePkgJobData) shmRemoteJobData;
        final List<Map<String, Object>> propertieslist = new ArrayList<Map<String, Object>>();
        propertieslist.add(neTypePropertiesHelper.createPropertyMap(JobPropertyConstants.DELETE_UP_LIST,
                shmDeleteUpgradePkgJobData.getProductNumber() + UpgradeActivityConstants.UPGRADEPACKAGES_REQUEST_DELIMTER + shmDeleteUpgradePkgJobData.getProductRevision()));
        propertieslist.add(neTypePropertiesHelper.createPropertyMap(JobPropertyConstants.DELETE_REFERRED_UPS, shmDeleteUpgradePkgJobData.getDeleteReferredUPs()));
        propertieslist.add(neTypePropertiesHelper.createPropertyMap(JobPropertyConstants.DELETE_FROM_ROLLBACK_LIST, shmDeleteUpgradePkgJobData.getDeleteFromRollbackList()));
        return neTypePropertiesHelper.getNeTypeProperties(activityProperties, propertieslist);
    }
}
