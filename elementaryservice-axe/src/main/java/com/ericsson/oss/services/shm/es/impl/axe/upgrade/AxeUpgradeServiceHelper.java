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
package com.ericsson.oss.services.shm.es.impl.axe.upgrade;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.DpsAvailabilityInfoProvider;
import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.common.retry.DpsRetryConfigurationParamProvider;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageManager;
import com.ericsson.oss.services.shm.job.api.JobConfigurationService;
import com.ericsson.oss.services.shm.job.axe.cache.AxeNeUpgradeCacheData;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;

@Traceable
@Profiled
@SuppressWarnings("unchecked")
public class AxeUpgradeServiceHelper {

    @Inject
    private RemoteSoftwarePackageManager remoteSoftwarePackageManager;

    @Inject
    private JobUpdateService jobUpdateService;
    
    @Inject
    protected DpsRetryConfigurationParamProvider dpsConfigurationParamProvider;
    
    @Inject
    protected DpsAvailabilityInfoProvider dpsAvailabilityInfoProvider;
    
    @Inject
    @PlatformAnnotation(name = PlatformTypeEnum.AXE)
    private JobConfigurationService axeJobConfigurationService;
    
    private static final int NUMBER_OF_DEFAULT_DPS_RETRIES = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(AxeUpgradeServiceHelper.class);

    public Map<String, String> getJobScriptParameters(final Map<String, Object> jobConfigurationDetails, final String neName, final String activityName, final String neType) {
        final Map<String, String> scriptParameters = new HashMap<>();

        getNeJobProperties(scriptParameters, jobConfigurationDetails, neName);
        getActivityJobProperties(scriptParameters, jobConfigurationDetails, neType, activityName);
        getNeTypeJobProperties(scriptParameters, jobConfigurationDetails, neType);
        LOGGER.info("Script parameters : {} ", scriptParameters);
        return scriptParameters;

    }

    public Map<String, String> getAdditionalParameters(final String jobOwner, final NEJobStaticData neJobStaticData, final String activityName, final Map<String, String> scriptParameters,
            final String filePath, final Map<String, Map<String, AxeNeUpgradeCacheData>> neJobStaticDataPerNetype, final String neType) {

        final Map<String, String> additionalParams = new HashMap<>();
        final String softwarePackageName = scriptParameters.get(AxeUpgradeActivityConstants.PRODUCT_NAME) + AxeUpgradeActivityConstants.SPACE
                + scriptParameters.get(AxeUpgradeActivityConstants.PRODUCT_NUMBER) + AxeUpgradeActivityConstants.SPACE + scriptParameters.get(AxeUpgradeActivityConstants.PRODUCT_REVISION);
        LOGGER.info(" neJobStaticData.getNodeName() is {} ", neJobStaticData.getNodeName());
        final String numberOfApgs = neJobStaticDataPerNetype.get(neType).get(neJobStaticData.getNodeName()).getNumberOfApg();
        final String cpFunction = neJobStaticDataPerNetype.get(neType).get(neJobStaticData.getNodeName()).getCpFunction();

        String nodeName = null;
        if (neJobStaticData.getNodeName().contains(AxeUpgradeActivityConstants.CLUSTER)) {
            nodeName = neJobStaticData.getParentNodeName();
        } else {
            nodeName = neJobStaticData.getNodeName();
        }

        additionalParams.put(AxeUpgradeActivityConstants.ADJUST_PROCESSOR, AxeUpgradeActivityConstants.EMPTY_STRING);
        additionalParams.put(AxeUpgradeActivityConstants.SMO_NE_TYPE, AxeUpgradeActivityConstants.AXE);
        additionalParams.put(AxeUpgradeActivityConstants.SMO_SOFTSTOP, AxeUpgradeActivityConstants.RUN_MODE);
        additionalParams.put(AxeUpgradeActivityConstants.THREAD_IDS, nodeName);
        additionalParams.put(AxeUpgradeActivityConstants.SMO_CURRENT_ACTIVITY_NAME, activityName);
        additionalParams.put(AxeUpgradeActivityConstants.SMO_USER_NAME, jobOwner);
        additionalParams.put(AxeUpgradeActivityConstants.OPS_SCRIPT, filePath + File.separator + scriptParameters.get(AxeUpgradeActivityConstants.SCRIPT));
        additionalParams.put(AxeUpgradeActivityConstants.SMO_SW_PACKAGE_PATH, filePath);
        additionalParams.put(AxeUpgradeActivityConstants.SMO_SW_PACKAGE_NAME, softwarePackageName);
        additionalParams.put(AxeUpgradeActivityConstants.SMO_NUMBER_OF_APGS, numberOfApgs);
        additionalParams.put(AxeUpgradeActivityConstants.SMO_NUMBER_OF_NES, String.valueOf(neJobStaticDataPerNetype.get(neType).size()));
        additionalParams.put(AxeUpgradeActivityConstants.SMO_CP_FUNCTION, cpFunction);

        if (neJobStaticData.getParentNodeName() != null) {
            additionalParams.put(AxeUpgradeActivityConstants.SMO_ME_NAME, neJobStaticData.getParentNodeName());
            if (neJobStaticData.getNodeName().contains(AxeUpgradeActivityConstants.NODENAME_COMPONENT_SEPARATOR)) {
                final int index = neJobStaticData.getNodeName().lastIndexOf(AxeUpgradeActivityConstants.NODENAME_COMPONENT_SEPARATOR);
                final String cpName = neJobStaticData.getNodeName().substring(index + 2);
                additionalParams.put(AxeUpgradeActivityConstants.SMO_CP_NAME, cpName);
            } else {
                additionalParams.put(AxeUpgradeActivityConstants.SMO_CP_NAME, AxeUpgradeActivityConstants.NO_BLADE);
            }
        } else {
            additionalParams.put(AxeUpgradeActivityConstants.SMO_CP_NAME, AxeUpgradeActivityConstants.NO_BLADE);
            additionalParams.put(AxeUpgradeActivityConstants.SMO_ME_NAME, neJobStaticData.getNodeName());
        }

        prepareNeInfoForOtherNodes(neJobStaticDataPerNetype.get(neType), additionalParams, neJobStaticData.getNodeName());
        LOGGER.info("additionalParameters {}", additionalParams);
        return additionalParams;
    }

    private void prepareNeInfoForOtherNodes(final Map<String, AxeNeUpgradeCacheData> allAxeNeNesCacheDataPerNeTpe, final Map<String, String> additionalParams, final String currentNe) {
        int counter = 0;
        // Add info for the rest of the NEs in this job
        if (allAxeNeNesCacheDataPerNeTpe != null && !allAxeNeNesCacheDataPerNeTpe.isEmpty()) {
            for (final String neJobNodeName : allAxeNeNesCacheDataPerNeTpe.keySet()) {
                if (currentNe.equals(neJobNodeName)) {
                    continue;
                }
                addNeInfoForOtherNes(counter, neJobNodeName, additionalParams, allAxeNeNesCacheDataPerNeTpe);
                counter++;
            }
        }
    }

    /**
     * @param counter
     * @param neJobNodeName
     * @param additionalParams
     * @param allAxeNeNesCacheDataPerNeTpe
     */

    private void addNeInfoForOtherNes(final int counter, final String neJobNodeName, final Map<String, String> additionalParams,
            final Map<String, AxeNeUpgradeCacheData> allAxeNeNesCacheDataPerNeTpe) {
        final String parentName = allAxeNeNesCacheDataPerNeTpe.get(neJobNodeName).getParentName();
        if (parentName != null) {
            if (neJobNodeName.contains(AxeUpgradeActivityConstants.NODENAME_COMPONENT_SEPARATOR)) {
                final int parentComponentSeperatorIndex = neJobNodeName.lastIndexOf(AxeUpgradeActivityConstants.NODENAME_COMPONENT_SEPARATOR);
                final String cpName = neJobNodeName.substring(parentComponentSeperatorIndex + 2);
                additionalParams.put(AxeUpgradeActivityConstants.SMO_CP_NAME + counter, cpName);
                additionalParams.put(AxeUpgradeActivityConstants.SMO_ME_NAME + counter, parentName);
            } else {
                additionalParams.put(AxeUpgradeActivityConstants.SMO_CP_NAME + counter, AxeUpgradeActivityConstants.NO_BLADE);
                additionalParams.put(AxeUpgradeActivityConstants.SMO_ME_NAME + counter, parentName);
            }
        } else {
            additionalParams.put(AxeUpgradeActivityConstants.SMO_CP_NAME + counter, AxeUpgradeActivityConstants.NO_BLADE);
            additionalParams.put(AxeUpgradeActivityConstants.SMO_ME_NAME + counter, neJobNodeName);
        }
        additionalParams.put(AxeUpgradeActivityConstants.SMO_CP_FUNCTION + counter, allAxeNeNesCacheDataPerNeTpe.get(neJobNodeName).getCpFunction());
    }

    /**
     * @param scriptParameters
     * @param jobConfigurationDetails
     */
    private void getNeJobProperties(final Map<String, String> scriptParameters, final Map<String, Object> jobConfigurationDetails, final String nodeName) {
        final List<Map<String, Object>> neJobProperties = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmConstants.NEJOB_PROPERTIES);
        if (neJobProperties != null && !neJobProperties.isEmpty()) {
            for (final Map<String, Object> neJobProperty : neJobProperties) {
                final String neName = (String) neJobProperty.get(ShmConstants.NE_NAME);
                if (nodeName.equals(neName)) {
                    final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) neJobProperty.get(ShmConstants.JOBPROPERTIES);
                    for (final Map<String, Object> jobProperty : jobProperties) {
                        scriptParameters.put(jobProperty.get(ShmConstants.KEY).toString(), jobProperty.get(ShmConstants.VALUE).toString());
                    }
                    break;
                }
            }
        }
        LOGGER.info("ne Job Script parameters : {} ", scriptParameters);
    }

    private void getActivityJobProperties(final Map<String, String> scriptParameters, final Map<String, Object> jobConfigurationDetails, final String neType, final String activityName) {
        final List<Map<String, Object>> neTypeActivityJobProperties = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmConstants.NETYPE_ACTIVITYJOBPROPERTIES);
        if (neTypeActivityJobProperties != null && !neTypeActivityJobProperties.isEmpty()) {
            getNeTypeActivityJobProperites(scriptParameters, neType, activityName, neTypeActivityJobProperties);
        }
        LOGGER.info("Activity Job Script parameters : {} ", scriptParameters);
    }

    /**
     * @param scriptParameters
     * @param neType
     * @param activityName
     * @param neTypeActivityJobProperties
     */
    private void getNeTypeActivityJobProperites(final Map<String, String> scriptParameters, final String neType, final String activityName,
            final List<Map<String, Object>> neTypeActivityJobProperties) {
        for (final Map<String, Object> neTypeActivityJobProperty : neTypeActivityJobProperties) {
            final String neTypeValue = (String) neTypeActivityJobProperty.get(ShmConstants.NETYPE);
            if (neTypeValue.equals(neType)) {
                final List<Map<String, Object>> activityJobProperties = (List<Map<String, Object>>) neTypeActivityJobProperty.get(ShmConstants.ACTIVITYJOB_PROPERTIES);
                getActivityJobProperties(scriptParameters, activityName, activityJobProperties);
                break;
            }
        }
    }

    /**
     * @param scriptParameters
     * @param activityName
     * @param activityJobProperties
     */
    private void getActivityJobProperties(final Map<String, String> scriptParameters, final String activityName, final List<Map<String, Object>> activityJobProperties) {
        for (final Map<String, Object> activityJobProperty : activityJobProperties) {
            final String activityNameValue = (String) activityJobProperty.get(ShmConstants.ACTIVITYNAME);
            if (activityNameValue.equals(activityName)) {
                final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) activityJobProperty.get(ShmConstants.JOBPROPERTIES);
                for (final Map<String, Object> jobProperty : jobProperties) {
                    scriptParameters.put(jobProperty.get(ShmConstants.KEY).toString(), jobProperty.get(ShmConstants.VALUE).toString());
                }
                break;
            }
        }
    }

    private void getNeTypeJobProperties(final Map<String, String> scriptParameters, final Map<String, Object> jobConfigurationDetails, final String neType) {
        final List<Map<String, Object>> neTypeJobProperties = (List<Map<String, Object>>) jobConfigurationDetails.get(ShmConstants.NETYPEJOBPROPERTIES);
        if (neTypeJobProperties != null && !neTypeJobProperties.isEmpty()) {
            for (final Map<String, Object> neTypeJobProperty : neTypeJobProperties) {
                final String neTypeValue = (String) neTypeJobProperty.get(ShmConstants.NETYPE);
                if (neTypeValue.equals(neType)) {
                    final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) neTypeJobProperty.get(ShmConstants.JOBPROPERTIES);
                    for (final Map<String, Object> jobProperty : jobProperties) {
                        scriptParameters.put(jobProperty.get(ShmConstants.KEY).toString(), jobProperty.get(ShmConstants.VALUE).toString());
                    }
                    break;
                }
            }
        }
        LOGGER.info("neType Job Script parameters : {} ", scriptParameters);
    }

    public String getSoftwarPackagePath(final String swPkgName) {
        final String upgradePackageFilePath = remoteSoftwarePackageManager.getSoftwarPackagePath(swPkgName);
        LOGGER.debug("Software package Name : {} and filePath is : {}", swPkgName, upgradePackageFilePath);
        return upgradePackageFilePath;
    }

    public boolean isAxeHandleTimeoutTriggered(final long activityJobId) {
        if (retrieveJobProperty(activityJobId, AxeUpgradeActivityConstants.IS_AXE_HANDLETIMEOUT_TRIGGERED) != null) {
            return Boolean.valueOf(retrieveJobProperty(activityJobId, AxeUpgradeActivityConstants.IS_AXE_HANDLETIMEOUT_TRIGGERED));
        }
        return false;
    }

    public boolean isResponseTimeStampAttributePersisted(final long activityJobId) {
        return retrieveJobProperty(activityJobId, AxeUpgradeActivityConstants.RESPONSE_TIMESTAMP_ATTRIBUTE) != null;
    }

    public Long getResponseTimeStampAttribute(final long activityJobId) {
        if (retrieveJobProperty(activityJobId, AxeUpgradeActivityConstants.RESPONSE_TIMESTAMP_ATTRIBUTE) != null) {
            return Long.valueOf(retrieveJobProperty(activityJobId, AxeUpgradeActivityConstants.RESPONSE_TIMESTAMP_ATTRIBUTE));
        }
        return null;
    }

    /**
     * @param activityJobId
     * @return
     */
    private String retrieveJobProperty(final long activityJobId, final String jobProperty) {
        final Map<String, Object> activityJobAttributes = jobUpdateService.retrieveJobWithRetry(activityJobId);
        final List<Map<String, String>> activityJobProperties = (List<Map<String, String>>) activityJobAttributes.get(ShmConstants.JOBPROPERTIES);
        if (activityJobProperties != null) {
            for (final Map<String, String> activityJobProperty : activityJobProperties) {
                if (jobProperty.equals(activityJobProperty.get(ShmConstants.KEY))) {
                    return activityJobProperty.get(ShmConstants.VALUE);
                }
            }
        }
        return null;
    }
    
    public boolean readAndUpdateRunningJobAttributes(final long jobId, final List<Map<String, Object>> jobPropertyList, final List<Map<String, Object>> jobLogList,
            final Double activityProgressPercentage) {
        boolean isJobAttributesPersisted = false;
        final int noOfRetries = getNoOfRetries();
        int i = 1;
        for (; i <= noOfRetries; i++) {
            try {
                isJobAttributesPersisted = axeJobConfigurationService.readAndPersistRunningJobAttributes(jobId, jobPropertyList, jobLogList, activityProgressPercentage);
                LOGGER.info("Successfully updated job attributes for jobId {} in trial with log update {}, job property update{} and activityProgressPercentage {}", jobId, jobLogList,
                        jobPropertyList, activityProgressPercentage);
                break;
            } catch (final Exception ex) {
                if (i == noOfRetries) {
                    LOGGER.error("Retries exhausted for job attributes update for JobId={} , discarding log update - {}, discarding job property update - {} with exception: {}", jobId, jobLogList,
                            jobPropertyList, ex.getMessage());
                } else {
                    sleep(ex);
                }
            }
        }
        return isJobAttributesPersisted;
    }
    
    protected int getNoOfRetries() {
        final int configuredRetries = dpsConfigurationParamProvider.getdpsRetryCount();
        return configuredRetries > NUMBER_OF_DEFAULT_DPS_RETRIES ? configuredRetries : NUMBER_OF_DEFAULT_DPS_RETRIES;
    }
    
    /**
     * Method to wait the control to wait for defined time.
     * 
     * wait interval may vary depending on DPS unavailable , Optimistic Lock issue.
     * 
     */
    private void sleep(final Exception exception) {
        try {
            if (exception instanceof EJBException && dpsAvailabilityInfoProvider.isDatabaseDown()) {
                LOGGER.warn("Database is down, waiting to retry");
                Thread.sleep(dpsConfigurationParamProvider.getdpsWaitIntervalInMS());
            } else if (exception instanceof EJBTransactionRolledbackException) {
                LOGGER.warn("Optimistic Lock issue occurred, waiting to retry");
                Thread.sleep(dpsConfigurationParamProvider.getDpsOptimisticLockWaitIntervalInMS());
            }
        } catch (final InterruptedException ie) {
            LOGGER.error("Job updation failed, because:", ie);
            Thread.currentThread().interrupt();
        }
    }

}
