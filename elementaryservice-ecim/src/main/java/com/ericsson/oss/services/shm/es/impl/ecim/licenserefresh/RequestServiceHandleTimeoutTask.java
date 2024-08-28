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

import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.HANDLE_TIMEOUT;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.es.api.ActivityInfo;
import com.ericsson.oss.services.shm.es.api.ActivityStepResult;
import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.instantaneouslicensing.LkfRefreshRequestResultMTRSender;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.modelentities.JobResult;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

public class RequestServiceHandleTimeoutTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestServiceHandleTimeoutTask.class);

    private static final ActivityInfo activityAnnotation = RequestService.class.getAnnotation(ActivityInfo.class);

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected JobLogUtil jobLogUtil;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private LicenseRefreshServiceProvider licenseRefreshServiceProvider;

    @Inject
    private LkfRefreshRequestResultMTRSender lkfRefreshRequestResultMTRSender;

    @Inject
    private LkfRefreshRequestResultBuilder lkfRefreshRequestResultBuilder;

    public ActivityStepResult handleTimeout(final long activityJobId, final JobActivityInfo jobActivityInfo) {
        final ActivityStepResult activityStepResult = new ActivityStepResult();
        final List<Map<String, Object>> jobLogList = new ArrayList<>();
        final List<Map<String, Object>> jobPropertyList = new ArrayList<>();
        NEJobStaticData neJobStaticData = null;
        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.LICENSE_REFRESH_JOB_CAPABILITY);
            activityUtils.prepareJobPropertyList(jobPropertyList, ActivityConstants.ACTIVITY_RESULT, JobResult.FAILED.getJobResult());
            sendLkfRequestResultForFailure(neJobStaticData);
            licenseRefreshServiceProvider.deleteShmNodeLicenseRefreshRequestDataPO(neJobStaticData.getNodeName());
            final Map<String, Object> activityJobProperties = jobUpdateService.retrieveJobWithRetry(activityJobId);
            LOGGER.debug("LicenseRefreshJob:Request activity handleTimeout having activityJobProperties : {}", activityJobProperties);

            final List<Map<String, Object>> jobProperties = (List<Map<String, Object>>) activityJobProperties.get(ActivityConstants.JOB_PROPERTIES);

            for (final Map<String, Object> jobProperty : jobProperties) {
                if (LicenseRefreshConstants.FINGERPRINT.equals(jobProperty.get(ShmConstants.KEY))) {
                    activityUtils.unSubscribeToMoNotifications(buildSubscriptionKey((String) jobProperty.get(ShmConstants.VALUE), neJobStaticData.getNeJobId()), activityJobId, jobActivityInfo);
                    licenseRefreshServiceProvider.deleteLkfRequestDataPo(neJobStaticData.getNeJobId(), (String) jobProperty.get(ShmConstants.VALUE));
                    break;
                }
            }
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(HANDLE_TIMEOUT, activityAnnotation.activityName(), neJobStaticData.getNodeName()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        } catch (final JobDataNotFoundException jobDataNotFoundException) {
            LOGGER.error("Unable to fetch NeJobStaticData. Reason :{} ", jobDataNotFoundException.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, activityAnnotation.activityName(), jobDataNotFoundException.getMessage()),
                    new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            sendLkfRequestResultForFailure(neJobStaticData);
        } catch (final Exception exception) {
            LOGGER.error("Exception ocuured in handleTimeout().reason: {}", exception.getMessage());
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, String.format(JobLogConstants.ACTIVITY_FAILED_IN_TIMEOUT, activityAnnotation.activityName(), exception.getMessage()), new Date(),
                    JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            sendLkfRequestResultForFailure(neJobStaticData);
        }
        activityStepResult.setActivityResultEnum(ActivityStepResultEnum.TIMEOUT_RESULT_FAIL);
        jobUpdateService.readAndUpdateRunningJobAttributes(activityJobId, jobPropertyList, jobLogList, 0d);
        LOGGER.debug("LicenseRefreshJob:Request activity {} - handletimeout finished", activityJobId);
        return activityStepResult;
    }

    private String buildSubscriptionKey(final String fingerPrint, final long neJobId) {
        return fingerPrint + LicenseRefreshConstants.SUBSCRIPTION_KEY_DELIMETER + neJobId;

    }

    private void sendLkfRequestResultForFailure(final NEJobStaticData neJobStaticData) {
        if (neJobStaticData != null && neJobStaticData.getNeJobId() > 0 && neJobStaticData.getNodeName() != null) {
            LOGGER.debug("LicenseRefreshJob:Request activity handleTimeout - sendLkfRequestResultForFailure for the neJobId {} and nodeName {}", neJobStaticData.getNeJobId(),
                    neJobStaticData.getNodeName());
            Map<String, Object> lkfRefreshRequestResultProperties = lkfRefreshRequestResultBuilder.buildLkfRequestResultForFailure(neJobStaticData,
                    LicenseRefreshConstants.LKF_REFRESH_REQUEST_SERVICE_HANDLE_TIMEOUT);
            if (isLkfRefreshRequestResultRequired(lkfRefreshRequestResultProperties)) {
                lkfRefreshRequestResultMTRSender.sendMTR(lkfRefreshRequestResultProperties);
            }
        } else {
            LOGGER.error("LicenseRefreshJob:LkfRequestResult sending to node is not triggerred due to values not available in neJobStaticData");
        }
    }

    private boolean isLkfRefreshRequestResultRequired(final Map<String, Object> lkfRefreshRequestResultProperties) {
        boolean isLkfRefreshRequestResultRequired = false;
        if (lkfRefreshRequestResultProperties.get(LicenseRefreshConstants.REQUEST_TYPE) != null
                && ((String) lkfRefreshRequestResultProperties.get(LicenseRefreshConstants.REQUEST_TYPE)).equals(LicenseRefreshConstants.LKF_REFRESH)) {
            isLkfRefreshRequestResultRequired = true;
        }
        LOGGER.debug("LicenseRefreshJob:Request activity handleTimeout - isLkfRefreshRequestResultRequired :  {} ", isLkfRefreshRequestResultRequired);
        return isLkfRefreshRequestResultRequired;
    }
}
