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
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import static com.ericsson.oss.services.shm.es.api.ProgressPercentageConstants.ACTIVITY_END_PROGRESS_PERCENTAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceQualifier;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.exception.utils.ExceptionParser;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.common.job.utils.UpgradeJobConfigurationListener;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.api.NeJobValidator;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.upgrade.remote.api.RemoteSoftwarePackageRetryProxy;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

/**
 * 
 * @author xnagvar
 */
@EServiceQualifier("CPP.UPGRADE")
@Stateless
@Traceable
@Profiled
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CppUpgradeJobValidator implements NeJobValidator {

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    UpgradeJobConfigurationListener upgradeJobConfigurationListener;

    @Inject
    FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    JobPropertyUtils jobPropertyUtils;

    @Inject
    EAccessControl accessControl;

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private RemoteSoftwarePackageRetryProxy remoteSoftwarePackageRetryProxy;

    private static final Logger LOGGER = LoggerFactory.getLogger(CppUpgradeJobValidator.class);

    /*
     * (non-Javadoc)
     * 
     * @see com.ericsson.oss.services.shm.es.api.NeJobLevelValidation#validate(long)
     */
    @Override
    public boolean validate(final long neJobId) {
        LOGGER.debug("Enter into validate with nejobId{}", neJobId);
        String nodeName = null;
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        boolean isNodeSkippedFromUpgrade = false;
        try {
            final Map<String, Object> neJobAttributes = activityUtils.getPoAttributes(neJobId);
            nodeName = (String) neJobAttributes.get(ShmConstants.NE_NAME);
            final long mainJobId = (long) neJobAttributes.get(ShmConstants.MAIN_JOB_ID);
            final Map<String, Object> mainJobAttributes = activityUtils.getPoAttributes(mainJobId);
            final long templateJobId = mainJobAttributes.get(ShmConstants.JOBTEMPLATEID) != null ? (long) mainJobAttributes.get(ShmConstants.JOBTEMPLATEID) : 0;
            final Map<String, Object> jobTemplate = activityUtils.getPoAttributes(templateJobId);
            final String jobOwner = (String) jobTemplate.get(ShmConstants.OWNER);
            accessControl.setAuthUserSubject(jobOwner);
            final Map<String, String> nodeSwPkgDetailsMap = getNodeSwPkgDetails(neJobId, nodeName, mainJobAttributes);

            isNodeSkippedFromUpgrade = isNodeSkippedFromUpgrade(remoteSoftwarePackageRetryProxy.validateUPMoState(nodeSwPkgDetailsMap), neJobId, nodeName, jobLogList);
        } catch (Exception exception) {
            final String exceptionMessage = ExceptionParser.getReason(exception);
            final String logMessage = "validate of Upgrade Job of Failed." + String.format(JobLogConstants.FAILURE_REASON, exceptionMessage);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            jobUpdateService.readAndUpdateRunningJobAttributes(neJobId, null, jobLogList, null);
            LOGGER.error("validate failed for the node : {}", nodeName, exception);
        }
        LOGGER.debug("Exit validate with result {}", isNodeSkippedFromUpgrade);
        return isNodeSkippedFromUpgrade;
    }

    /**
     * This method determines that whether UP MO is valid for Installation or not.
     * 
     * @param neJobId
     * @param proceedWithNodeUpgrade
     * @return NeLevelResultEnum
     */
    private boolean isNodeSkippedFromUpgrade(final Map<String, Object> swMgmtresponse, final long neJobId, final String neName, final List<Map<String, Object>> jobLogList) {
        LOGGER.debug("Entering validateUPMoState with neJobId  {} ", neJobId);
        boolean isNodeSkippedFromUpgrade = false;
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        if (upgradeJobConfigurationListener.isSkipUpgradeEnabled() && (boolean) swMgmtresponse.get(UpgradeActivityConstants.IS_UP_ACTIVE_ON_NODE)) {
            String activeSwProductData = null;
            if (swMgmtresponse.get(UpgradeActivityConstants.UP_PO_SWP_PRODUCT_DETAILS) != null) {
                activeSwProductData = (String) swMgmtresponse.get(UpgradeActivityConstants.UP_PO_SWP_PRODUCT_DETAILS);
            }
            final String logMessage = String.format("Node is already at same software %s. Skipping node upgrade", activeSwProductData);
            LOGGER.debug("{}  {} for NeJob {}", neName, logMessage, neJobId);
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.recordEvent(SHMEvents.NE_JOB_VALIDATION, neName, activeSwProductData, activityUtils.additionalInfoForEvent(neJobId, neName, logMessage));
            activityUtils.prepareJobPropertyList(jobPropertyList, ShmConstants.RESULT, JobResult.SKIPPED.toString());
            //update activityJobs as skipped
            updateActivityJobsResultAsSkipped(neJobId);
            jobUpdateService.readAndUpdateRunningJobAttributes(neJobId, jobPropertyList, jobLogList, ACTIVITY_END_PROGRESS_PERCENTAGE);
            isNodeSkippedFromUpgrade = true;
        }
        return isNodeSkippedFromUpgrade;
    }

    private void updateActivityJobsResultAsSkipped(final long neJobId) {
        LOGGER.info("In CppUpgradeJobValidator.updateActivityJobsResultAsSkipped, Skipping all activities under nejob : {}", neJobId);
        final List<Long> activityJobIds = jobConfigurationServiceRetryProxy.getJobPoIdsFromParentJobId(neJobId, ShmConstants.ACTIVITY_JOB, ShmConstants.NE_JOB_ID);
        for (final Long activityJobId : activityJobIds) {
            jobUpdateService.updateActivityAsSkipped(activityJobId);
        }
    }

    private Map<String, String> getNodeSwPkgDetails(final long neJobId, final String neName, final Map<String, Object> mainJobAttributes) {
        String neType = null;
        String platform = null;
        final Map<String, Object> jobConfigurationDetails = (Map<String, Object>) mainJobAttributes.get(ActivityConstants.JOB_CONFIGURATION_DETAILS);
        try {
            final List<NetworkElement> networkElementsList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(Arrays.asList(neName));
            if (!networkElementsList.isEmpty()) {
                neType = networkElementsList.get(0).getNeType();
                platform = networkElementsList.get(0).getName();
            }
        } catch (final RetriableCommandException | IllegalArgumentException e) {
            LOGGER.error("Exception while fetching neType of node :  {}", neName);
        }

        LOGGER.debug("Nemane {},neType {}, platform {} Fetched for neJobId {} are ", neName, neType, platform, neJobId);
        final List<String> keyList = new ArrayList<>();
        keyList.add(UpgradeActivityConstants.SWP_NAME);
        keyList.add(UpgradeActivityConstants.UCF);
        final Map<String, String> keyValueMap = jobPropertyUtils.getPropertyValue(keyList, jobConfigurationDetails, neName, neType, platform);
        LOGGER.debug("Fetched SwPkgNameandUcfNameMap {} for neJobId: {}", keyValueMap, neJobId);
        keyValueMap.put(ShmConstants.NETYPE, neType);
        keyValueMap.put(ShmConstants.NE_NAME, neName);
        keyValueMap.put(ShmConstants.PLATFORM, platform);
        return keyValueMap;
    }
}
