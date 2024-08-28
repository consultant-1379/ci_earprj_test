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
 *----------------------------------------------------------------------------
package com.ericsson.oss.services.shm.es.impl.cpp.upgrade;

import java.util.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.cm.cmreader.api.CmReaderService;
import com.ericsson.oss.services.cm.cmshared.dto.*;
import com.ericsson.oss.services.shm.es.api.*;
import com.ericsson.oss.services.shm.es.impl.FdnNotificationSubject;
import com.ericsson.oss.services.shm.es.impl.NotificationRegistry;
import com.ericsson.oss.shm.inventory.api.CmServiceAdapter;
import com.ericsson.oss.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.shm.jobs.common.modelentities.JobState;

public class CancelService {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Inject
	JobLogService jobLogService;

	@Inject
	NotificationRegistry notificationRegistry;

	@Inject
	CmServiceAdapter cmServiceAdapter;

	@Inject
	JobPropertyService jobPropertyService;

	@EServiceRef
	private CmReaderService cmreaderservice;

	@Inject
	// QUES: CmServiceAdapter will be Inject or EService?
	CmServiceAdapter cmServiceAdpaterBean;

	public ActivityStepResult cancel(final long neJobId) {

		final CmResponse cmResponse = getUpMo(neJobId);
		ActivityStepResult activityStepResult = null;

		if (cmResponse != null) {

			if (cmResponse.getStatusCode() == 0) {
				final String logMessage = "Unable to proceed with cancel as MO doesn't exist.";
				jobLogService.persistJobLog(neJobId, logMessage,
						JobLogType.SYSTEM);
			} else if (cmResponse.getStatusCode() == 1) {
				final List<CmObject> cmObjectList = (List<CmObject>) cmResponse
						.getCmObjects();
				final CmObject cmObject = cmObjectList.get(0);
				final Map<String, Object> upCmAttributes = cmObject
						.getAttributes();

				final UpgradePackageState upState = (UpgradePackageState) upCmAttributes
						.get(UpgradePackageMoConstants.UP_MO_STATE);
				final String stateMessage = upState.getStateMessage();

				if (upState == UpgradePackageState.AWAITING_CONFIRMATION
						|| upState == UpgradePackageState.UPGRADE_EXECUTING
						|| upState == UpgradePackageState.INSTALL_EXECUTING) {

					String activityJobState = null;
					String activityName = null;
					final String upMoFdn = getUpMoFdn(neJobId, cmResponse);
					logger.debug("Trying to retrieve activityJob details with neJobId");
					final CmResponse activityJobs = cmServiceAdapter
							.retrieveActivityJobs(neJobId);
					for (final CmObject activityJobObject : activityJobs
							.getCmObjects()) {
						final Map<String, Object> cmObjectAttributes = activityJobObject
								.getAttributes();
						activityJobState = (String) cmObjectAttributes
								.get(ShmConstants.STATE);
						activityName = (String) cmObjectAttributes
								.get(ShmConstants.NAME);

						if (activityJobState.equals(JobState.RUNNING)) {
							if (upMoFdn != null) {
								logger.debug("upMoFdn "+upMoFdn);
								if (activityName.equals("Install")) {
									logger.debug("Proceeding with cancel Install");
									activityStepResult = cancelInstall(neJobId,
											upMoFdn);
									cmObjectAttributes.put(ShmConstants.STATE,
											JobState.CANCELLING);
									logger.debug("Changed the job state to cancelling");
									return activityStepResult;

								} else if (activityName.equals("Upgrade")) {
									logger.debug("Proceeding with cancel upgrade");
									activityStepResult = cancelUpgrade(neJobId,
											upMoFdn);
									cmObjectAttributes.put(ShmConstants.STATE,
											JobState.CANCELLING);
									logger.debug("Changed the job state to cancelling");
									return activityStepResult;
								}
							}
							else{
								logger.debug("upMoFdn is null");
							}
						}
					}

				} else {
					final String logMessage = "Unable to proceed cancel activity because "
							+ stateMessage;
					jobLogService.persistJobLog(neJobId, logMessage,
							JobLogType.SYSTEM);
				}
			} else if (cmResponse.getStatusCode() > 1) {
				final String logMessage = "Unable to proceed cancel activity because multiple UP MO exists.";
				jobLogService.persistJobLog(neJobId, logMessage,
						JobLogType.SYSTEM);
			}
		} else {
			logger.debug("Exception occured while retrieving Upgrade Package MO");
		}

		return activityStepResult;
	}

	public ActivityStepResult cancelInstall(final long neJobId,
			final String upMoFdn) {
		logger.debug("Inside cancel install");
		final String actionType = ActivityConstants.ACTION_CANCEL_INSTALL;
		CmResponse cmResponse;

		logger.debug("Registering for Notification");
		final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(
				upMoFdn, neJobId);
		notificationRegistry.register(fdnNotificationSubject);
		logger.debug("Registered for Notification");

		logger.debug("actionType:" + actionType);
		cmResponse = cmServiceAdapter.performAction(upMoFdn, actionType);

		final String logMessage = "MO Action Initiated with action "
				+ actionType;

		jobLogService.persistJobLog(neJobId, logMessage, JobLogType.SYSTEM);

		int statusCode = -1;
		if (cmResponse != null) {
			statusCode = cmResponse.getStatusCode();
		} else {
			logger.debug("Exception occured while performing action");
		}
		final ActivityStepResult activityStepResult = new ActivityStepResult();
		activityStepResult.setSuccess(true);
		if (statusCode == 1) {
			activityStepResult.setSuccess(true);
		} else {
			notificationRegistry.removeSubject(fdnNotificationSubject);
		}
		logger.debug("Sending back ActivityStepResult to WorkFlow: activityStepResult= "
				+ activityStepResult.isSuccess());

		return activityStepResult;

	}

	public ActivityStepResult cancelUpgrade(final long neJobId,
			final String upMoFdn) {
		logger.debug("Inside cancel upgrade");
		final String actionType = ActivityConstants.ACTION_CANCEL_UPGRADE;
		CmResponse cmResponse;

		logger.debug("Registering for Notification");
		final FdnNotificationSubject fdnNotificationSubject = new FdnNotificationSubject(
				upMoFdn, neJobId);
		notificationRegistry.register(fdnNotificationSubject);
		logger.debug("Registered for Notification");

		cmResponse = cmServiceAdapter.performAction(upMoFdn, actionType);

		final String logMessage = "MO Action Initiated with action "
				+ actionType;

		jobLogService.persistJobLog(neJobId, logMessage, JobLogType.SYSTEM);

		int statusCode = -1;
		if (cmResponse != null) {
			statusCode = cmResponse.getStatusCode();
		} else {
			logger.debug("Exception occured while performing action");
		}
		final ActivityStepResult activityStepResult = new ActivityStepResult();
		activityStepResult.setSuccess(true);
		if (statusCode == 1) {
			activityStepResult.setSuccess(true);
		} else {
			notificationRegistry.removeSubject(fdnNotificationSubject);
		}
		logger.debug("Sending back ActivityStepResult to WorkFlow: activityStepResult= "
				+ activityStepResult.isSuccess());
		return activityStepResult;

	}

	@SuppressWarnings("unchecked")
	public String getUpMoFdn(final long neJobId,
			final CmResponse cmResponseNeJobs) {

		String upMoFdn = null;

		final List<CmObject> cmObjectList = (List<CmObject>) cmResponseNeJobs
				.getCmObjects();
		final CmObject cmObject = cmObjectList.get(0);
		final List<Map<String, Object>> jobPropertiesList = (List<Map<String, Object>>) cmObject
				.getAttributes().get(ActivityConstants.JOB_PROPERTIES);
		for (final Map<String, Object> jobProperty : jobPropertiesList) {
			if (ActivityConstants.UP_FDN.equals(jobProperty
					.get(ActivityConstants.JOB_PROP_KEY))) {
				upMoFdn = (String) jobProperty
						.get(ActivityConstants.JOB_PROP_VALUE);
			}
		}
		return upMoFdn;
	}

	public CmResponse getUpMo(final long neJobId) {

		final Map<String, String> upgradePackageMap = processUpgradePackageMO(neJobId);
		final String upgradePackageId = upgradePackageMap
				.get("upgradePackageId");
		final String parentFdn = upgradePackageMap.get("parentFdn");
		final CmResponse cmResponseUpMo = cmServiceAdapter.getUpgradePackageMO(
				upgradePackageId, parentFdn);

		return cmResponseUpMo;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> processUpgradePackageMO(final long neJobId) {
		final List<Map<String, Object>> jobPropertyList = cmServiceAdapter.getJobDetailslsForElementary(neJobId);
		String swPkgName = null;
		for (final Map<String, Object> jobProperty : jobPropertyList) {
			if (ActivityConstants.CPP_SWP_NAME.equals(jobProperty
					.get(ActivityConstants.JOB_PROP_KEY))) {
				swPkgName = (String) jobProperty
						.get(ActivityConstants.JOB_PROP_VALUE);
			}
		}

		// set the UP name for querying DPS
		final Map<String, Object> upgardePkgMap = new HashMap<String, Object>();
		upgardePkgMap.put("packageName", swPkgName);
		upgardePkgMap.put("nodePlatform", null);
		upgardePkgMap.put("filePath", null);
		upgardePkgMap.put("hash", null);
		upgardePkgMap.put("importedBy", null);
		upgardePkgMap.put("importDate", null);
		upgardePkgMap.put("description", null);
		// checking if SW pkg already exists
		Map<String, Object> upgradePackageAttributes = null;
		logger.info(
				"Call being passed to CmServiceAdapter to fetch UCF file names for package name {}",
				swPkgName);
		upgradePackageAttributes = cmServiceAdapter.getSoftwarePackageInfo(
				swPkgName.trim(), upgardePkgMap);

		// get the product details from
		String upgradePackageId = null;
		String filePath = null;
		String parentFdn = null;
		if (upgradePackageAttributes != null) {
			final List<HashMap<String, Object>> swpProductDetails = (List<HashMap<String, Object>>) upgradePackageAttributes
					.get("swpProductDetails");
			String productNumber = (String) swpProductDetails.get(0).get(
					"productNumber");
			String productRevision = (String) swpProductDetails.get(0).get(
					"productRevision");
			filePath = (String) swpProductDetails.get(0).get("filePath");

			productNumber = productNumber.trim();
			productRevision = productRevision.trim();

			upgradePackageId = productNumber + "_" + productRevision;
			String nodeFdn = null;
			String nodeName = null;

			final List<Long> neJobIdList = new ArrayList<Long>();
			neJobIdList.add(neJobId);

			final CmResponse neCmResponse = cmreaderservice.getPosByPoIds(
					neJobIdList, CmConstants.LIVE_CONFIGURATION);

			if (neCmResponse.getStatusCode() > 0) {
				for (final CmObject cmObject : neCmResponse.getCmObjects()) {
					final Map<String, Object> cmObjectAttributes = cmObject
							.getAttributes();
					nodeName = (String) cmObjectAttributes
							.get(ShmConstants.NE_NAME);
				}
				nodeFdn = "MeContext=" + nodeName;

			} else {
				logger.error("No NE job found for the activityId:" + neJobId);
			}
			parentFdn = cmServiceAdapter.getUpgradePkgParentFDN(nodeFdn);

		}
		final Map<String, String> upgradePackageMap = new HashMap<String, String>();
		upgradePackageMap.put("upgradePackageId", upgradePackageId);
		upgradePackageMap.put("filePath", filePath);
		upgradePackageMap.put("parentFdn", parentFdn);

		return upgradePackageMap;
	}
}
*/