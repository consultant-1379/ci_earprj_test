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
package com.ericsson.oss.services.shm.es.impl.cpp.backup;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.services.shm.common.exception.MoActionAbortRetryException;
import com.ericsson.oss.services.shm.common.exception.MoActionRetryException;
import com.ericsson.oss.services.shm.common.retry.DpsRetryPolicies;
import com.ericsson.oss.services.shm.common.retry.ShmDpsRetriableCommand;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.es.impl.NodeMediationServiceExceptionParser;
import com.ericsson.oss.services.shm.es.impl.cpp.common.ConfigurationVersionMO;
import com.ericsson.oss.services.shm.es.impl.cpp.common.UpgradePackageMO;

/**
 * Retry Proxy class for retrying few CV operations
 * 
 * @author xrajeke
 * 
 */
@ApplicationScoped
public class CommonCvOperationsRetryProxy implements CommonCvOperations {

    @Inject
    private CommonCvOperationsImpl commonCvOperationsImpl;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRetryPolicies dpsRetryPolicies;

    @Override
    public int executeActionOnMo(final String actionType, final String cvMoFdn, final Map<String, Object> actionParameters) {
        return executeActionOnMo(actionType, cvMoFdn, actionParameters, dpsRetryPolicies.getDpsGeneralRetryPolicy());

    }

    @Override
    public boolean precheckForSetStartCVSetFistInRolback(final Map<String, Object> cvMoAttr, final String cvName) {
        return commonCvOperationsImpl.precheckForSetStartCVSetFistInRolback(cvMoAttr, cvName);
    }

    @Override
    public ConfigurationVersionMO getCVMo(final String nodeName) {
        final ConfigurationVersionMO cvMo = retryManager.executeCommand(dpsRetryPolicies.getReadAttributesRetryPolicy(), new ShmDpsRetriableCommand<ConfigurationVersionMO>() {
            @Override
            public ConfigurationVersionMO execute() {
                return commonCvOperationsImpl.getCVMo(nodeName);
            }
        });
        return cvMo;
    }

    @Override
    public String getCVMoFdn(final String nodeName) {
        final String cvMoFdn = retryManager.executeCommand(dpsRetryPolicies.getDpsGeneralRetryPolicy(), new ShmDpsRetriableCommand<String>() {
            @Override
            public String execute() {
                return commonCvOperationsImpl.getCVMoFdn(nodeName);
            }
        });
        return cvMoFdn;
    }

    @Override
    public UpgradePackageMO getUPMo(final String nodeName, final String searchValue) {

        final UpgradePackageMO upMo = retryManager.executeCommand(dpsRetryPolicies.getReadAttributesRetryPolicy(), new ShmDpsRetriableCommand<UpgradePackageMO>() {
            @Override
            public UpgradePackageMO execute() {
                return commonCvOperationsImpl.getUPMo(nodeName, searchValue);
            }
        });
        return upMo;
    }

    @Override
    public boolean precheckForUploadCVAction(final Map<String, Object> cvMoAttr, final Map<String, Object> actionParameters) {
        return commonCvOperationsImpl.precheckForUploadCVAction(cvMoAttr, actionParameters);
    }

    @Override
    public int executeActionOnMo(final String actionType, final String cvMoFdn, final Map<String, Object> actionParameters, final RetryPolicy retryPolicy) {
        return retryManager.executeCommand(retryPolicy, new ShmDpsRetriableCommand<Integer>() {
            @Override
            public Integer execute() {
                int returnValue = -1;
                try {
                    returnValue = commonCvOperationsImpl.executeActionOnMo(actionType, cvMoFdn, actionParameters);
                } catch (final Exception ex) {

                    processFailureReason(actionParameters, ex);
                }
                return returnValue;
            }

            /**
             * @param actionParameters
             * @param ex
             * @throws Throwable
             * @throws Exception
             */
            private void processFailureReason(final Map<String, Object> actionParameters, final Exception ex) {
                String failureMessage = null;
                final String exceptionMessage = NodeMediationServiceExceptionParser.getReason(ex);
                if (exceptionMessage != null && exceptionMessage.toLowerCase().contains(JobLogConstants.CV_ALREADY_EXISTS_EXCEPTION)) {
                    failureMessage = JobLogConstants.UNABLE_TO_CREATE_DUPLICATE_CV;
                    throw new MoActionAbortRetryException(failureMessage);
                } else if (exceptionMessage != null && exceptionMessage.toLowerCase().contains(JobLogConstants.CV_MAX_NUMBER_OF_INSTANCES_EXISTS)) {
                    failureMessage = JobLogConstants.UNABLE_TO_CREATE_CV_MAX_NUMBER_OF_INSTANCES_EXISTS;
                    throw new MoActionAbortRetryException(failureMessage);
                } else if (exceptionMessage != null && exceptionMessage.toLowerCase().contains(JobLogConstants.CONFIGURATION_VERSION_FILE_SYSTEM_ERROR)) {
                    failureMessage = JobLogConstants.UNABLE_TO_CREATE_CONFIGURATION_VERSION_FILE_SYSTEM_ERROR;
                    throw new MoActionAbortRetryException(failureMessage);
                } else if (exceptionMessage != null && exceptionMessage.toLowerCase().contains(JobLogConstants.CONFIGURATION_VERSION_FORMAT_ERROR)) {
                    failureMessage = JobLogConstants.UNABLE_TO_CREATE_CONFIGURATION_VERSION_FORMAT_ERROR;
                    throw new MoActionAbortRetryException(failureMessage);
                } else if (exceptionMessage != null) {
                    throw new MoActionRetryException(exceptionMessage, ex);
                } else {
                    failureMessage = "Exception received while invoking MO Action";
                    throw new MoActionRetryException(failureMessage, ex);
                }
            }
        });
    }

    @Override
    public Map<String, Object> getCVMoAttributesFromNode(final String cvMoFdn, final String[] requiredCvMoAttributes) {
        return commonCvOperationsImpl.getCVMoAttributesFromNode(cvMoFdn, requiredCvMoAttributes);
    }

}