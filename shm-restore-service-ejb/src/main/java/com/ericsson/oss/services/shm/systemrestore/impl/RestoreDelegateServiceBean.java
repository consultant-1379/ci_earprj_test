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
package com.ericsson.oss.services.shm.systemrestore.impl;

import static com.ericsson.oss.services.shm.systemrestore.impl.RestoreInitiationServiceRecordingConstants.*;

import java.util.List;
import java.util.concurrent.*;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.cluster.restore.*;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;
import com.ericsson.oss.services.shm.system.restore.JobRecoveryService;
import com.ericsson.oss.services.shm.system.restore.JobRestoreResult;

@Profiled
@Traceable
@Stateless
public class RestoreDelegateServiceBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreDelegateServiceBean.class);

    @Inject
    private RestoreManager restoreManager;

    @Inject
    private SystemRecorder systemRecorder;

    @EServiceRef
    private JobRecoveryService jobRecoveryService;

    @Asynchronous
    public void triggerJobRestore() {
        ServiceRestoreResponse response = null;
        ServiceRestoreStatus status = null;

        do {
            response = restoreManager.tryRestore(30, TimeUnit.SECONDS);
        } while (!validResponse(response.getStatus()));

        status = response.getStatus();
        if (status == ServiceRestoreStatus.ALLOWED) {
            LOGGER.info("Preparing for SHM Restore");
            systemRecorder.recordEvent(SYSTEM_RESTORE_RESPONSE_ALLOWED, EventLevel.COARSE, SHM_SOURCE, SHM_SOURCE, ADDITIONAL_INFO_WHEN_RESTORE_STATUS_IS_ALLOWED);
            prepareForShmRestore();
        } else if (status == ServiceRestoreStatus.NOT_ALLOWED) {
            LOGGER.info("ServiceRestoreStatus is {}. Restore can't be executed", status);
            systemRecorder.recordEvent(SYSTEM_RESTORE_RESPONSE_NOT_ALLOWED, EventLevel.COARSE, SHM_SOURCE, SHM_SOURCE, ADDITIONAL_INFO_WHEN_RESTORE_STATUS_IS_NOT_ALLOWED);
        } else if (status == ServiceRestoreStatus.COMPLETED) {
            LOGGER.info("ServiceRestoreStatus is {}. {}", status, ADDITIONAL_INFO_WHEN_RESTORE_STATUS_IS_COMPLETED);
            systemRecorder.recordEvent(SYSTEM_RESTORE_RESPONSE_COMPLETED, EventLevel.COARSE, SHM_SOURCE, SHM_SOURCE, ADDITIONAL_INFO_WHEN_RESTORE_STATUS_IS_COMPLETED);
        }
    }

    private boolean validResponse(final ServiceRestoreStatus serviceRestoreStatus) {
        return serviceRestoreStatus == ServiceRestoreStatus.NOT_ALLOWED || serviceRestoreStatus == ServiceRestoreStatus.COMPLETED || serviceRestoreStatus == ServiceRestoreStatus.ALLOWED;
    }

    private void prepareForShmRestore() {
        LOGGER.info(ADDITIONAL_INFO_WHEN_RESTORE_IS_INITIATED);
        systemRecorder.recordEvent(SYSTEM_RESTORE_INITIATED, EventLevel.COARSE, SHM_SOURCE, SHM_SOURCE, ADDITIONAL_INFO_WHEN_RESTORE_IS_INITIATED);
        // Async  - Call SHM Jobs Synchronization Service
        LOGGER.info("JobRecoveryService :{} has been invoked", jobRecoveryService);
        final Future<List<JobRestoreResult>> jobSynchronizationResult = jobRecoveryService.handleJobRestore();

        try {
            jobSynchronizationResult.get();
        } catch (final InterruptedException | ExecutionException e) {
            LOGGER.error("Unable to retrieve the status of the shm jobs restore: ", e);
        }

        final ServiceRestoreCompletionStatus completionStatus = ServiceRestoreCompletionStatus.SUCCESS;
        restoreManager.finishRestoreWith(completionStatus);
        systemRecorder.recordEvent(SYSTEM_RESTORE_FINISHED, EventLevel.COARSE, SHM_SOURCE, SHM_SOURCE, ADDITIONAL_INFO_WHEN_RESTORE_IS_FINISHED);
        LOGGER.info(ADDITIONAL_INFO_WHEN_RESTORE_IS_FINISHED);
    }

}
