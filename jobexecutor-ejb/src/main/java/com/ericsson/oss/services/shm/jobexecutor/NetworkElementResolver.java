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
package com.ericsson.oss.services.shm.jobexecutor;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.exception.DatabaseNotAvailableException;
import com.ericsson.oss.services.shm.common.exception.ServerInternalException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.api.JobUpdateService;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.mapper.JobCapabilityProvider;
import com.ericsson.oss.services.shm.networkelement.NetworkElementResponse;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.workflow.WorkflowInstanceNotifier;

/**
 * 
 * @author xeswpot
 * 
 */
public class NetworkElementResolver extends AbstractTargetResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkElementResolver.class);

    @Inject
    private FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private JobUpdateService jobUpdateService;

    @Inject
    private WorkflowInstanceNotifier workflowInstanceNotifier;

    @Inject
    private JobCapabilityProvider capabilityProvider;

    @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public NetworkElementResponse getNetworkElementResponse(final long mainJobId, final List<String> neNames, final long templateJobId, final Map<String, Object> attributeMap,
            final JobTypeEnum jobType, final boolean isMainJobCreated) {
        List<NetworkElement> networkElementList = null;
        final NetworkElementResponse networkElementResponse = new NetworkElementResponse();
        try {
            networkElementList = fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neNames, capabilityProvider.getCapability(jobType));
            if (networkElementList != null && !networkElementList.isEmpty()) {
                networkElementResponse.setSupportedNes(networkElementList);
            }
        } catch (ServerInternalException | DatabaseNotAvailableException e) {
            if (isMainJobCreated) {
                LOGGER.error(JobExecutorConstants.NE_PLATFORMQUERY_FAILED, e);
                updateJobJobAttributes(mainJobId, templateJobId, attributeMap);
                return null;
            } else {
                LOGGER.debug("Error in retrieving supported and unsupported network element list: {}", e);
                return null;
            }
        }

        //log UnsupportedNodes to the main Job
        if (networkElementList != null && neNames.size() != networkElementList.size()) {
            LOGGER.info("{} NeNames are Untransformed to NetworkElements ", neNames.size() - networkElementList.size());
            for (final NetworkElement ne : networkElementList) {
                neNames.remove(ne.getName());
            }
            if (!neNames.isEmpty()) {

                setInvalidNes(neNames, networkElementResponse);
            }
            if (isMainJobCreated) {
                LOGGER.warn("These Nodes {} are not going to be handled in Job execution service.", neNames);
                updateJobLog(mainJobId, String.format(JobExecutorConstants.UNSUPPORTED_NODES, neNames));
            }
        }
        return networkElementResponse;

    }

    /**
     * @param neNames
     * @param jobId
     */
    protected void updateJobLog(final long jobId, final String message) {
        final Map<String, Object> activityAttributes = new HashMap<String, Object>();
        activityAttributes.put(ActivityConstants.JOB_LOG_MESSAGE, message);
        activityAttributes.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        activityAttributes.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        activityAttributes.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.INFO.toString());

        final Map<String, Object> jobAttributes = new HashMap<String, Object>();
        jobAttributes.put(ShmConstants.LOG, Arrays.asList(activityAttributes));
        jobUpdateService.updateJobAttributes(jobId, jobAttributes);
    }
    
    protected void updateJobJobAttributes(final long mainJobId, final long templateJobId, final Map<String, Object> attributeMap) {
    	 final Map<String, Object> jobLogList = new HashMap<String, Object>();
        jobLogList.put(ActivityConstants.JOB_LOG_MESSAGE, JobExecutorConstants.NE_PLATFORMQUERY_FAILED);
        jobLogList.put(ActivityConstants.JOB_LOG_ENTRY_TIME, new Date());
        jobLogList.put(ActivityConstants.JOB_LOG_TYPE, JobLogType.SYSTEM.toString());
        jobLogList.put(ActivityConstants.JOB_LOG_LEVEL, JobLogLevel.ERROR.toString());
        attributeMap.put(ActivityConstants.JOB_LOG, Arrays.asList(jobLogList));
        jobUpdateService.updateJobAttributes(mainJobId, attributeMap);
        workflowInstanceNotifier.sendAllNeDone(Long.toString(templateJobId));
    }

    private void setInvalidNes(final List<String> neNames, final NetworkElementResponse networkElementResponse) {
        final Map<NetworkElement, String> invalidNEs = new HashMap<NetworkElement, String>();
        final List<NetworkElement> invalidNetworkElementsList = buildNetworkElement(neNames);
        final String logMessage = "Failing job execution as NetworkElement doesn't exist";
        for (final NetworkElement networkElement : invalidNetworkElementsList) {
            invalidNEs.put(networkElement, logMessage);
        }
        networkElementResponse.setInvalidNes(invalidNEs);
    }

}
