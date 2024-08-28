/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh;

import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.itpf.sdk.recording.CommandPhase;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.SHMEvents;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.model.events.instlicense.NodeCapacity;
import com.ericsson.oss.services.shm.model.events.instlicense.ShmLicenseRefreshElisRequest;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

public class RequestServiceExecuteTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestServiceExecuteTask.class);

    private static final ActivityInfo activityAnnotation = RequestService.class.getAnnotation(ActivityInfo.class);

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected JobLogUtil jobLogUtil;

    @Inject
    protected SystemRecorder systemRecorder;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private LicenseRefreshServiceProvider licenseRefreshServiceProvider;

    @Inject
    @Modeled
    private EventSender<ShmLicenseRefreshElisRequest> shmLicenseRefreshElisRequestEventSender;

    @Inject
    private UpgradeLicenseKeyServiceProvider upgradeLicenseKeyServiceProvider;

    public void execute(long activityJobId, final JobActivityInfo jobActivityInfo) {

        final List<Map<String, Object>> jobLogs = new ArrayList<>();
        final List<Map<String, Object>> jobProperties = new ArrayList<>();
        long mainJobId = 0L;
        String nodeName = null;
        String businessKey = null;
        try {
            final NEJobStaticData neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY);
            mainJobId = neJobStaticData.getMainJobId();
            nodeName = neJobStaticData.getNodeName();
            businessKey = neJobStaticData.getNeJobBusinessKey();
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_INITIATED, activityAnnotation.activityName()), new Date(), JobLogType.SYSTEM.toString(),
                    JobLogLevel.INFO.toString());
            final Map<String, Object> mainJobAttributes = activityUtils.getPoAttributes(mainJobId);
            ShmLicenseRefreshElisRequest shmLicenseRefreshElisRequest;
            final String lkfRequestType = upgradeLicenseKeyServiceProvider.getLkfRequestTypeInitiatedByNodeOrSoftwarePackage(mainJobAttributes);
            LOGGER.info("Licence refresh type for the node {} is  {}", nodeName, lkfRequestType);
            if (lkfRequestType != null && lkfRequestType.equals(LicenseRefreshConstants.UPGRADE_LICENSE_KEYS)) {
                shmLicenseRefreshElisRequest = upgradeLicenseKeyServiceProvider.prepareShmLicenseRefreshElisRequestEvent(neJobStaticData, mainJobAttributes);
            } else {
                shmLicenseRefreshElisRequest = prepareShmLicenseRefreshElisRequestEvent(nodeName, neJobStaticData);
            }
            sendShmLicenseRefreshElisRequestEvent(shmLicenseRefreshElisRequest);
            jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(ELIS_NOTIFICATION, shmLicenseRefreshElisRequest), new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
            activityUtils.subscribeToMoNotifications(buildSubscriptionKey(shmLicenseRefreshElisRequest.getFingerprint(), neJobStaticData.getNeJobId()), activityJobId, jobActivityInfo);
            systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REQUEST_EXECUTE, CommandPhase.FINISHED_WITH_SUCCESS, neJobStaticData.getNodeName(), neJobStaticData.getNeJobBusinessKey(),
                    activityUtils.additionalInfoForCommand(activityJobId, neJobStaticData.getMainJobId(), JobTypeEnum.LICENSE_REFRESH));
            LOGGER.debug("LicenseRefreshJob:Request activity {} - execute result finished with success ", activityJobId);
            activityUtils.prepareJobPropertyList(jobProperties, LicenseRefreshConstants.FINGERPRINT, shmLicenseRefreshElisRequest.getFingerprint());
            jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobProperties, jobLogs, null);

        } catch (final JobDataNotFoundException jobDataNotFoundException) {
            failRequest(activityJobId, jobLogs, nodeName, businessKey, mainJobId, JobLogConstants.DATABASE_SERVICE_NOT_ACCESSIBLE);
        } catch (final Exception exception) {
            failRequest(activityJobId, jobLogs, nodeName, businessKey, mainJobId, exception.getMessage());
        }
    }

    private ShmLicenseRefreshElisRequest prepareShmLicenseRefreshElisRequestEvent(String nodeName, final NEJobStaticData neJobStaticData) {
        ShmLicenseRefreshElisRequest shmLicenseRefreshElisRequest = new ShmLicenseRefreshElisRequest();
        final Map<String, Object> licenseRefreshRequestInfoAttributes = licenseRefreshServiceProvider.prepareShmLicenseRefreshElisRequestEvent(nodeName);
        shmLicenseRefreshElisRequest.setJobId(String.valueOf(neJobStaticData.getNeJobId()));
        int actionId = -1;
        if (licenseRefreshRequestInfoAttributes != null && !licenseRefreshRequestInfoAttributes.isEmpty()) {
            shmLicenseRefreshElisRequest.setFingerprint((String) licenseRefreshRequestInfoAttributes.get(FINGERPRINT));
            if (licenseRefreshRequestInfoAttributes.get(LICENSE_REFRESH_REQUEST_TYPE) != null && !((String) licenseRefreshRequestInfoAttributes.get(LICENSE_REFRESH_REQUEST_TYPE)).isEmpty()) {
                shmLicenseRefreshElisRequest.setRequestType(getRequestType((String) licenseRefreshRequestInfoAttributes.get(LICENSE_REFRESH_REQUEST_TYPE)));
            } else {
                shmLicenseRefreshElisRequest.setRequestType(REFRESH);
            }
            shmLicenseRefreshElisRequest.setEuft((String) licenseRefreshRequestInfoAttributes.get(EUFT));
            shmLicenseRefreshElisRequest.setSwltId((String) licenseRefreshRequestInfoAttributes.get(SWLT_ID));
            shmLicenseRefreshElisRequest.setSwRelease((String) licenseRefreshRequestInfoAttributes.get(SW_RELEASE));
            shmLicenseRefreshElisRequest.setNodeType((String) licenseRefreshRequestInfoAttributes.get(NODE_TYPE));
            actionId = (int) licenseRefreshRequestInfoAttributes.get(ACTION_ID);
            final List<NodeCapacity> nodeCapacities = (List<NodeCapacity>) licenseRefreshRequestInfoAttributes.get(CAPACITIES);
            if (nodeCapacities != null && !nodeCapacities.isEmpty()) {
                shmLicenseRefreshElisRequest.setNodeCapacities(nodeCapacities);
            }
            updateNeJobProperties(actionId, neJobStaticData);
        }
        return shmLicenseRefreshElisRequest;
    }

    private void failRequest(final long activityJobId, final List<Map<String, Object>> jobLogs, final String nodeName, final String businessKey, final Long mainJobId, final String message) {
        LOGGER.error("LicenseRefreshJob:Request activity {} failed due to: {}", activityJobId, message);
        jobLogUtil.prepareJobLogAtrributesList(jobLogs, String.format(JobLogConstants.ACTIVITY_EXECUTE_FAILED_WITH_REASON, activityAnnotation.activityName(), message), new Date(),
                JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        systemRecorder.recordCommand(SHMEvents.LICENSEREFRESH_REQUEST_EXECUTE, CommandPhase.FINISHED_WITH_ERROR, nodeName, activityAnnotation.activityName(),
                activityUtils.additionalInfoForCommand(activityJobId, mainJobId, JobTypeEnum.LICENSE_REFRESH));
        activityUtils.failActivity(activityJobId, jobLogs, businessKey, activityAnnotation.activityName());
    }

    private void sendShmLicenseRefreshElisRequestEvent(final ShmLicenseRefreshElisRequest shmLicenseRefreshElisRequest) {
        shmLicenseRefreshElisRequestEventSender.send(shmLicenseRefreshElisRequest);
        LOGGER.debug("LicenseRefreshJob:Request activity event is sent to ELIS for lkf with attributes : [{}]", shmLicenseRefreshElisRequest);
    }

    private String buildSubscriptionKey(final String fingerPrint, final long neJobId) {
        return fingerPrint + LicenseRefreshConstants.SUBSCRIPTION_KEY_DELIMETER + neJobId;

    }

    private void updateNeJobProperties(final int actionId, final NEJobStaticData neJobStaticData) {
        final Map<String, String> requestIdJobProperty = new HashMap<>();
        final Map<String, String> requestTypeJobProperty = new HashMap<>();
        final List<Map<String, String>> neJobProperties = new ArrayList<>();
        final Map<String, Object> neJobAttributes = new HashMap<>();
        requestIdJobProperty.put(ActivityConstants.JOB_PROP_KEY, LicenseRefreshConstants.REQUEST_ID);
        requestIdJobProperty.put(ActivityConstants.JOB_PROP_VALUE, String.valueOf(actionId));
        neJobProperties.add(requestIdJobProperty);
        requestTypeJobProperty.put(ActivityConstants.JOB_PROP_KEY, LicenseRefreshConstants.REQUEST_TYPE);
        requestTypeJobProperty.put(ActivityConstants.JOB_PROP_VALUE, LicenseRefreshConstants.LKF_REFRESH);
        neJobProperties.add(requestTypeJobProperty);
        neJobAttributes.put(ActivityConstants.JOB_PROPERTIES, neJobProperties);
        neJobAttributes.put(NE_NAME, neJobStaticData.getNodeName());
        jobUpdateService.updateJobAttributes(neJobStaticData.getNeJobId(), neJobAttributes);
    }

    private String getRequestType(final String licenseRefreshRequestType) {
        String requestType = "";
        switch (licenseRefreshRequestType) {
        case LKF_REFRESH:
            requestType = REFRESH;
            break;
        case CAPACITY_REQUEST:
            requestType = EXPANSION;
            break;
        default:
            requestType = REFRESH;
        }
        return requestType;
    }

}
