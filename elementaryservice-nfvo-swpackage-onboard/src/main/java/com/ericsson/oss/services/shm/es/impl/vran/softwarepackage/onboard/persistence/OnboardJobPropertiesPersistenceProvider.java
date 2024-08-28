/*------------------------------------------------------------------------------
 *******************************************************************************
k * COPYRIGHT Ericsson 2017
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.onboard.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

/**
 * 
 * This Service will provide software package onboard job information
 * 
 * @author xjhosye
 */

public class OnboardJobPropertiesPersistenceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnboardJobPropertiesPersistenceProvider.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private VranJobActivityUtil vranJobActivityUtil;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    /**
     * Initializes counters for software package onboard activity. Ex: total software packages count, initial package index, initial failed packages count etc.
     * 
     * @param activityJobId
     */
    public void initializeOnboardActivityVariables(final long activityJobId) {
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final String packagesToBeOnboarded = getPackagesToBeOnboarded(jobContext);
        final String[] packages = vranJobActivityUtil.splitSoftwarePackages(packagesToBeOnboarded);
        final int totalPackageCount = packages.length;
        LOGGER.debug("ActivityJob ID - [{}] : Total packages selected for onboard: {}", activityJobId, totalPackageCount);

        final Map<String, Object> activityJobAttributes = jobContext.getActivityJobAttributes();
        List<Map<String, Object>> neJobProperties = vranJobActivityService.getNeJobAttributes(jobContext);
        final long neJobId = (long) activityJobAttributes.get(ShmConstants.NE_JOB_ID);
        if (neJobProperties == null) {
            neJobProperties = new ArrayList<Map<String, Object>>();
        }
        buildNeJobProperties(totalPackageCount, neJobProperties);

        jobAttributesPersistenceProvider.persistJobProperties(neJobId, neJobProperties);

    }

    /**
     * @param totalPackageCount
     * @param neJobProperties
     */
    private void buildNeJobProperties(final int totalPackageCount, final List<Map<String, Object>> neJobProperties) {

        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.TOTAL_NUMBER_OF_PACKAGES_TO_BE_ONBOARDED, Integer.toString(totalPackageCount)));
        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.CURRENT_PACKAGE_INDEX_TO_BE_ONBOARDED, Integer.toString(0)));
        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.ONBOARD_FAILURE_PACKAGES_COUNT, Integer.toString(0)));
        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.ONBOARD_SUCCESS_PACKAGES_COUNT, Integer.toString(0)));
    }

    /**
     * @param jobContext
     * @return
     */
    private String getPackagesToBeOnboarded(final JobEnvironment jobContext) {

        final Map<String, Object> jobConfigurationDetails = vranJobActivityService.getMainJobAttributes(jobContext);

        return vranJobActivityService.getPropertyFromNEJobPropeties(VranJobConstants.VNF_PACKAGES_TO_ONBOARD, jobConfigurationDetails, jobContext.getNodeName());

    }

    /**
     * Method to update current software package index in job properties
     * 
     * @param activityJobId
     * @param property
     */
    public void updateCurrentSoftwarePackageIndex(final long activityJobId, final String property) {
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final Map<String, Object> activityJobAttributes = jobContext.getActivityJobAttributes();
        List<Map<String, Object>> neJobProperties = vranJobActivityService.getNeJobAttributes(jobContext);
        if ((String) vranJobActivityService.getJobPropertyValue(property, neJobProperties) != null) {
            int currentIndex = Integer.parseInt((String) vranJobActivityService.getJobPropertyValue(property, neJobProperties));
            LOGGER.debug("ActivityJob ID - [{}] : current package index before updating: {}", activityJobId, currentIndex);
            final long neJobId = (long) activityJobAttributes.get(ShmConstants.NE_JOB_ID);
            if (neJobProperties == null) {
                neJobProperties = new ArrayList<Map<String, Object>>();
            }
            neJobProperties.add(vranJobActivityService.buildJobProperty(property, Integer.toString(++currentIndex)));
            jobAttributesPersistenceProvider.persistJobProperties(neJobId, neJobProperties);
        }

    }

    /**
     * To set current index of software packages to job properties
     * 
     * @param activityJobId
     */
    public void incrementSoftwarePackageCurrentIndexToBeOnboarded(final long activityJobId, final JobEnvironment jobContext) {
        updateOnboardSoftwarePackageActivityVariables(activityJobId, VranJobConstants.CURRENT_PACKAGE_INDEX_TO_BE_ONBOARDED, jobContext);

    }

    /**
     * To set failed software packages count to job properties
     */
    public void incrementOnboardFailedSoftwarePackagesCount(final long activityJobId, final JobEnvironment jobContext) {
        updateOnboardSoftwarePackageActivityVariables(activityJobId, VranJobConstants.ONBOARD_FAILURE_PACKAGES_COUNT, jobContext);
    }

    /**
     * To set success software packages count to job properties
     * 
     * @param jobContext
     */
    public void incrementOnboardSuccessSoftwarePackagesCount(final long activityJobId, final JobEnvironment jobContext) {
        updateOnboardSoftwarePackageActivityVariables(activityJobId, VranJobConstants.ONBOARD_SUCCESS_PACKAGES_COUNT, jobContext);
    }

    /**
     * Method to set the current software package index
     * 
     * @param activityJobId
     * @param softwarePackageIndex
     * @param jobContext
     */
    private void updateOnboardSoftwarePackageActivityVariables(final long activityJobId, final String softwarePackageIndex, final JobEnvironment jobContext) {
        List<Map<String, Object>> neJobProperties = vranJobActivityService.getNeJobAttributes(jobContext);
        final String indexValue = (String) vranJobActivityService.getJobPropertyValue(softwarePackageIndex, neJobProperties);
        if (indexValue != null) {

            int currentIndex = Integer.parseInt(indexValue);
            LOGGER.trace("ActivityJob ID - [{}] :{} : {}", activityJobId, softwarePackageIndex, currentIndex);

            final long neJobId = jobContext.getNeJobId();
            if (neJobProperties == null) {
                neJobProperties = new ArrayList<Map<String, Object>>();
            }

            neJobProperties.add(vranJobActivityService.buildJobProperty(softwarePackageIndex, Integer.toString(++currentIndex)));
            jobAttributesPersistenceProvider.persistJobProperties(neJobId, neJobProperties);
        }
    }

}
