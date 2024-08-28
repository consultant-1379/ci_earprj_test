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
package com.ericsson.oss.services.shm.job.service.cpp.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.job.activity.JobType;
import com.ericsson.oss.services.shm.job.service.JobTypeDetailsProvider;
import com.ericsson.oss.services.shm.job.utils.ActivityParamMapper;
import com.ericsson.oss.services.shm.jobs.common.annotations.PlatformJobTypeAnnotation;
import com.ericsson.oss.services.shm.jobs.common.constants.JobPropertyConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobConfiguration;
import com.ericsson.oss.services.shm.jobs.common.restentities.JobConfigurationDetails;

/**
 * This class is to get the job configuration details of DeleteUpgradePackage Job when CPP specific node is selected, which to be displayed on Node activities panel of job details page
 * 
 */
@ApplicationScoped
@PlatformJobTypeAnnotation(platformType = PlatformTypeEnum.CPP, jobType = JobType.DELETE_UPGRADEPACKAGE)
public class DeleteUpgradePackageJobDetailsProvider implements JobTypeDetailsProvider {

    @Inject
    private ActivityParamMapper activityParamMapper;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    private static final Map<String, List<String>> acitvityParameters = new HashMap<String, List<String>>();
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBackupJobDetailsProvider.class);

    static {

        acitvityParameters.put(ShmConstants.DELETEUPGRADEPKG_ACTIVITY, Arrays.asList(JobPropertyConstants.DELETE_FROM_ROLLBACK_LIST, JobPropertyConstants.DELETE_REFERRED_UPS));
    }

    @Override
    public List<Map<String, String>> getJobConfigurationDetails(final Map<String, Object> jobConfigurationDetails, final PlatformTypeEnum platformType, final String neType, final String neName) {

        final List<Map<String, String>> deleteUpgradepkgJobDetailsList = new LinkedList<>();
        final List<String> keyList = new ArrayList<String>();
        keyList.add(UpgradeActivityConstants.DELETE_UP_LIST);
        final Map<String, String> deleteUpgradepackageValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platformType.toString());
        final String deleteUpgradepackageInfo = deleteUpgradepackageValueMap.get(UpgradeActivityConstants.DELETE_UP_LIST);
        try {
            if (deleteUpgradepackageInfo != null && !deleteUpgradepackageInfo.isEmpty() && deleteUpgradepackageInfo.contains(",")) {
                final String deleteUpgradepkgProductNumberProductRevisions[] = deleteUpgradepackageInfo.split(",");
                for (int i = 0; i < deleteUpgradepkgProductNumberProductRevisions.length; i++) {
                    final String deleteUpgradepkgProductNumberProductRevision = deleteUpgradepkgProductNumberProductRevisions[i];
                    updateProductNumberAndRevision(deleteUpgradepkgProductNumberProductRevision, deleteUpgradepkgJobDetailsList);
                }
            } else {
                updateProductNumberAndRevision(deleteUpgradepackageInfo, deleteUpgradepkgJobDetailsList);
            }

        } catch (RuntimeException runtimeException) {
            LOGGER.error("Exception occured while fetching delete Upgradepackage details in Node activites {} ", runtimeException.getMessage());
        }
        LOGGER.debug("deleteUpgradepackage details for cpp Node activites {} ", deleteUpgradepkgJobDetailsList);
        return deleteUpgradepkgJobDetailsList;
    }

    @Override
    public JobConfigurationDetails getJobConfigParamDetails(final JobConfiguration jobConfigurationDetails, final String neType) {

        return activityParamMapper.getJobConfigurationDetails(jobConfigurationDetails, neType, PlatformTypeEnum.CPP.name(), acitvityParameters);

    }

    /**
     * @param deleteUpgradepkgProductNumberProductRevision
     * @param deleteUpgradepkgJobDetailsList
     */

    private void updateProductNumberAndRevision(final String deleteUpgradepkgProductNumberProductRevision, final List<Map<String, String>> deleteUpgradepkgJobDetailsList) {
        final Map<String, String> deleteUpgradepackageJobDetailsMap = new HashMap<String, String>();
        final String UpgradepkgProductNumberProductRevision[] = deleteUpgradepkgProductNumberProductRevision.split(UpgradeActivityConstants.UPGRADEPACKAGES_PERSISTENCE_DELIMTER);
        final String deleteUpgradepkgProductNumber = UpgradepkgProductNumberProductRevision[0];
        final String deleteUpgradepkgProductRevision = UpgradepkgProductNumberProductRevision[1];
        deleteUpgradepackageJobDetailsMap.put(JobPropertyConstants.PRODUCT_NUMBER, deleteUpgradepkgProductNumber);
        deleteUpgradepackageJobDetailsMap.put(JobPropertyConstants.PRODUCT_REVISION, deleteUpgradepkgProductRevision);
        deleteUpgradepkgJobDetailsList.add(deleteUpgradepackageJobDetailsMap);

    }
}
