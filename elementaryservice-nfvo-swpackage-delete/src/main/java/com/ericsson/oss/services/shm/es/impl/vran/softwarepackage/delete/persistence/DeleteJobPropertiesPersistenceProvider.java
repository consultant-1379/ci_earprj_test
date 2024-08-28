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
package com.ericsson.oss.services.shm.es.impl.vran.softwarepackage.delete.persistence;

import java.util.*;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityUtil;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

@Stateless
public class DeleteJobPropertiesPersistenceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteJobPropertiesPersistenceProvider.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    @Inject
    private VranJobActivityUtil vranJobActivityUtil;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    public void initializeActivityVariables(final long activityJobId) {
        int totalSoftwarePackagesInNfvo = 0;
        int totalSoftwarePackagesInEnm = 0;

        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final long neJobId = jobContext.getNeJobId();
        final Map<String, String> packagesToBeDeleted = getPackagesToBeDeleted(jobContext);

        final String packagesFromNfvo = packagesToBeDeleted.get(VranJobConstants.DELETE_VNF_PACKAGES_FROM_NFVO);
        final String packagesFromEnm = packagesToBeDeleted.get(VranJobConstants.DELETE_VNF_PACKAGES_FROM_ENM);

        totalSoftwarePackagesInNfvo = getSelectedPackagesCount(packagesFromNfvo);
        totalSoftwarePackagesInEnm = getSelectedPackagesCount(packagesFromEnm);

        LOGGER.debug("ActivityJob ID - [{}] :Total number of software packages to be deleted from ENM: {} and NFVO: {}", activityJobId, totalSoftwarePackagesInEnm, totalSoftwarePackagesInNfvo);

        List<Map<String, Object>> neJobProperties = vranJobActivityService.getNeJobAttributes(jobContext);
        if (neJobProperties == null) {
            neJobProperties = new ArrayList<Map<String, Object>>();
        }
        buildNeJobProperties(totalSoftwarePackagesInNfvo, totalSoftwarePackagesInEnm, neJobProperties);

        jobAttributesPersistenceProvider.persistJobProperties(neJobId, neJobProperties);

    }

    public void incrementSoftwarePackageCurrentIndexInNfvo(final long activityJobId, final JobEnvironment jobContext) {
        updateDeleteSoftwarePackageActivityVariables(activityJobId, VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_NFVO, jobContext);

    }

    public void incrementSoftwarePackageCurrentIndexInEnm(final long activityJobId, final JobEnvironment jobContext) {
        updateDeleteSoftwarePackageActivityVariables(activityJobId, VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_ENM, jobContext);
    }

    public void incrementFailedSoftwarePackageCountInNfvo(final long activityJobId, final JobEnvironment jobContext) {
        updateDeleteSoftwarePackageActivityVariables(activityJobId, VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_NFVO, jobContext);
    }

    public void incrementFailedSoftwarePackageCountForEnm(final long activityJobId, final JobEnvironment jobContext) {
        updateDeleteSoftwarePackageActivityVariables(activityJobId, VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_ENM, jobContext);
    }

    public void incrementSuccessSoftwarePackageCountInNfvo(final long activityJobId, final JobEnvironment jobContext) {
        updateDeleteSoftwarePackageActivityVariables(activityJobId, VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_NFVO, jobContext);
    }

    public void incrementSuccessSoftwarePackageCountForEnm(final long activityJobId, final JobEnvironment jobContext) {
        updateDeleteSoftwarePackageActivityVariables(activityJobId, VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_ENM, jobContext);
    }

    public void updateFailedSoftwarePackagesInNfvo(final long activityJobId, final String failedPackageName, final JobEnvironment jobContext) {
        updateFailedPackages(activityJobId, failedPackageName, VranJobConstants.FAILED_PACKAGES_FROM_NFVO, jobContext);
    }

    public void updateFailedSoftwarePackagesInEnm(final long activityJobId, final String failedPackageName, final JobEnvironment jobContext) {
        updateFailedPackages(activityJobId, failedPackageName, VranJobConstants.FAILED_PACKAGES_FROM_ENM, jobContext);
    }

    private void buildNeJobProperties(final int totalSoftwarePackagesInNfvo, final int totalSoftwarePackagesInEnm, final List<Map<String, Object>> neJobProperties) {

        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.TOTAL_NUMBER_OF_PACKAGES_IN_NFVO, Integer.toString(totalSoftwarePackagesInNfvo)));
        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_NFVO, Integer.toString(0)));
        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_NFVO, Integer.toString(0)));
        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_NFVO, Integer.toString(0)));

        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.TOTAL_NUMBER_OF_PACKAGES_IN_ENM, Integer.toString(totalSoftwarePackagesInEnm)));
        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.CURRENT_DELETED_PACKAGE_INDEX_IN_ENM, Integer.toString(0)));
        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.NUMBER_OF_FAILURE_PACKAGES_IN_ENM, Integer.toString(0)));
        neJobProperties.add(vranJobActivityService.buildJobProperty(VranJobConstants.NUMBER_OF_SUCCESS_PACKAGES_IN_ENM, Integer.toString(0)));
    }

    private Map<String, String> getPackagesToBeDeleted(final JobEnvironment jobContext) {

        final String nodeName = jobContext.getNodeName();
        final Map<String, Object> jobConfigurationDetails = vranJobActivityService.getMainJobAttributes(jobContext);
        final List<String> requiredJobPropertyKeys = new ArrayList<String>();

        requiredJobPropertyKeys.add(VranJobConstants.DELETE_VNF_PACKAGES_FROM_NFVO);
        requiredJobPropertyKeys.add(VranJobConstants.DELETE_VNF_PACKAGES_FROM_ENM);

        return jobPropertyUtils.getPropertyValue(requiredJobPropertyKeys, jobConfigurationDetails, nodeName, nodeName, PlatformTypeEnum.vRAN.name());

    }

    private int getSelectedPackagesCount(final String selectedPackages) {

        int selectedPackagesCount = 0;
        if (selectedPackages != null && !selectedPackages.isEmpty()) {
            final String[] packages = vranJobActivityUtil.splitSoftwarePackages(selectedPackages);
            selectedPackagesCount = packages.length;
        }

        return selectedPackagesCount;
    }

    private void updateDeleteSoftwarePackageActivityVariables(final long activityJobId, final String propertyName, final JobEnvironment jobContext) {
        List<Map<String, Object>> neJobProperties = vranJobActivityService.getNeJobAttributes(jobContext);
        final String indexValue = (String) vranJobActivityService.getJobPropertyValue(propertyName, neJobProperties);
        if (indexValue != null) {

            int currentIndex = Integer.parseInt(indexValue);
            LOGGER.trace("ActivityJob ID - [{}] :{} : {}", activityJobId, propertyName, currentIndex);

            final long neJobId = jobContext.getNeJobId();
            if (neJobProperties == null) {
                neJobProperties = new ArrayList<Map<String, Object>>();
            }

            neJobProperties.add(vranJobActivityService.buildJobProperty(propertyName, Integer.toString(++currentIndex)));
            jobAttributesPersistenceProvider.persistJobProperties(neJobId, neJobProperties);
        }
    }

    private void updateFailedPackages(final long activityJobId, final String failedSoftwarePackage, final String propertyName, final JobEnvironment jobContext) {
        List<Map<String, Object>> neJobProperties = vranJobActivityService.getNeJobAttributes(jobContext);

        String persistedFailedSoftwarePackages = (String) vranJobActivityService.getJobPropertyValue(propertyName, neJobProperties);
        if (persistedFailedSoftwarePackages != null) {
            persistedFailedSoftwarePackages = persistedFailedSoftwarePackages + failedSoftwarePackage + ";";
        } else {
            persistedFailedSoftwarePackages = failedSoftwarePackage + ";";
        }
        final long neJobId = jobContext.getNeJobId();
        if (neJobProperties == null) {
            neJobProperties = new ArrayList<Map<String, Object>>();
        }
        LOGGER.trace("ActivityJob ID - [{}] :Updating {} property with: {}", activityJobId, propertyName, persistedFailedSoftwarePackages);
        neJobProperties.add(vranJobActivityService.buildJobProperty(propertyName, persistedFailedSoftwarePackages));
        jobAttributesPersistenceProvider.persistJobProperties(neJobId, neJobProperties);
    }
}