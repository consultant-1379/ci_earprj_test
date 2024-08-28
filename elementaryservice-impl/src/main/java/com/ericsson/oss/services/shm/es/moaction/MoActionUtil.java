/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.moaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.es.api.JobActivityInfo;
import com.ericsson.oss.services.shm.es.moaction.api.ShmEcimMoActionAttributes;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConfiguration;
import com.ericsson.oss.services.shm.es.polling.api.PollingActivityConstants;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.model.ShmEBMCMoActionData;
import com.ericsson.oss.services.shm.model.event.based.mediation.ShmEcimMOActionMediationTaskRequest;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

/**
 * This Util class is used to prepare the Mo Action attributes for Event Based Mediation
 * 
 * @author xpavdeb
 */
public class MoActionUtil {

    @Inject
    private OssModelInfoProvider ossModelInfoProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private PollingActivityConfiguration activityConfiguration;

    private static final Logger LOGGER = LoggerFactory.getLogger(MoActionUtil.class);

    /**
     * @param nodeName
     * @param moFdn
     * @param moName
     * @param actionName
     * @param uploadActionArguments
     * @param jobActivityInfo
     * @return
     * @throws MoNotFoundException
     */
    public ShmEcimMOActionMediationTaskRequest prepareMTRAttributes(final String nodeName, final String moFdn, final String moName, final String actionName,
            final Map<String, Object> uploadActionArguments, final JobActivityInfo jobActivityInfo) throws MoNotFoundException {
        final ShmEcimMOActionMediationTaskRequest moActionMTRRequest = new ShmEcimMOActionMediationTaskRequest();
        final Map<String, Object> additionalInformation = new HashMap<>();
        final NetworkElementData networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
        final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElement.getNeType(), networkElement.getOssModelIdentity(), FragmentType.ECIM_BRM_TYPE.getFragmentName());
        final String mimVersion = ossModelInfo.getVersion();
        final String nameSpace = ossModelInfo.getNamespace();
        final long activityJobId = jobActivityInfo.getActivityJobId();
        additionalInformation.put(ShmConstants.PLATFORM, jobActivityInfo.getPlatform().name());
        additionalInformation.put(ShmConstants.JOB_TYPE, jobActivityInfo.getJobType().name());
        additionalInformation.put(ShmConstants.ACTIVITYNAME, jobActivityInfo.getActivityName());
        additionalInformation.put(ShmConstants.RETRY_COUNT, ShmConstants.ZERO_INT);
        additionalInformation.put(PollingActivityConstants.MO_ATTRIBUTES, new ArrayList<String>());
        additionalInformation.put(PollingActivityConstants.OPERATION_TYPE, ShmConstants.ACTION);

        moActionMTRRequest.setActionName(actionName);
        moActionMTRRequest.setMoName(moName);
        moActionMTRRequest.setMoFdn(moFdn);
        moActionMTRRequest.setActivityJobId(activityJobId);
        moActionMTRRequest.setActionAttributes(uploadActionArguments);
        moActionMTRRequest.setNamespace(nameSpace);
        moActionMTRRequest.setMimVersion(mimVersion);
        moActionMTRRequest.setAdditionalInformation(additionalInformation);
        moActionMTRRequest.setNodeAddress(moFdn);
        return moActionMTRRequest;
    }

    /**
     * 
     * @param ebmcMoActionData
     * @return
     */
    public ShmEcimMOActionMediationTaskRequest prepareMTRAttributes(final ShmEBMCMoActionData ebmcMoActionData, final int retryCountInCache) {
        final ShmEcimMOActionMediationTaskRequest moActionMTRRequest = new ShmEcimMOActionMediationTaskRequest();
        final Map<String, Object> additionalInformation = ebmcMoActionData.getAdditionalInformation();
        final String platform = (String) additionalInformation.get(ShmConstants.PLATFORM);
        final String jobType = (String) additionalInformation.get(ShmConstants.JOB_TYPE);
        final String activityName = (String) additionalInformation.get(ShmConstants.ACTIVITYNAME);
        final List<String> moAttributes = (List<String>) additionalInformation.get(PollingActivityConstants.MO_ATTRIBUTES);

        additionalInformation.put(ShmConstants.PLATFORM, platform);
        additionalInformation.put(ShmConstants.JOB_TYPE, jobType);
        additionalInformation.put(ShmConstants.ACTIVITYNAME, activityName);
        additionalInformation.put(ShmConstants.RETRY_COUNT, retryCountInCache);
        additionalInformation.put(PollingActivityConstants.MO_ATTRIBUTES, moAttributes);
        additionalInformation.put(PollingActivityConstants.OPERATION_TYPE, ShmConstants.ACTION);

        moActionMTRRequest.setActionName(ebmcMoActionData.getActionName());
        moActionMTRRequest.setMoName(ebmcMoActionData.getMoName());
        moActionMTRRequest.setMoFdn(ebmcMoActionData.getMoFdn());
        moActionMTRRequest.setActivityJobId(ebmcMoActionData.getActivityJobId());
        moActionMTRRequest.setActionAttributes(ebmcMoActionData.getMoActionAttributes());
        moActionMTRRequest.setNamespace(ebmcMoActionData.getNamespace());
        moActionMTRRequest.setMimVersion(ebmcMoActionData.getMimVersion());
        moActionMTRRequest.setAdditionalInformation(additionalInformation);
        moActionMTRRequest.setNodeAddress(ebmcMoActionData.getMoFdn());
        return moActionMTRRequest;
    }

    /**
     * @param mtrRequest
     * @return
     */
    public Map<String, Object> prepareMoActionDataforCache(final ShmEcimMOActionMediationTaskRequest mtrRequest) {
        final Map<String, Object> moActionMTRData = new HashMap<>();

        moActionMTRData.put(PollingActivityConstants.MO_FDN, mtrRequest.getMoFdn());
        moActionMTRData.put(PollingActivityConstants.ACTION_NAME, mtrRequest.getActionName());
        moActionMTRData.put(PollingActivityConstants.MIM_VERSION, mtrRequest.getMimVersion());
        moActionMTRData.put(PollingActivityConstants.NAMESPACE, mtrRequest.getNamespace());
        moActionMTRData.put(PollingActivityConstants.MO_ACTION_ATTRIBUTES, mtrRequest.getActionAttributes());
        moActionMTRData.put(PollingActivityConstants.ADDITIONAL_INFORMATION, mtrRequest.getAdditionalInformation());

        return moActionMTRData;
    }

    /**
     * @param activityJobId
     * @param moActionMTRData
     * @return
     */
    public ShmEBMCMoActionData retrieveMoActionDataAndAddToCache(final long activityJobId, final ShmEcimMOActionMediationTaskRequest moActionMTRRequest, final String platform) {
        LOGGER.debug("Enter retrieveMoActionDataAndAddToCache with activityJobId {}", activityJobId);
        final String actionName = moActionMTRRequest.getActionName();
        final String moFdn = moActionMTRRequest.getMoFdn();
        final String namespace = moActionMTRRequest.getNamespace();
        final String mimVersion = moActionMTRRequest.getMimVersion();
        final Map<String, Object> moActionAttributes = moActionMTRRequest.getActionAttributes();
        final Map<String, Object> additionalInformation = moActionMTRRequest.getAdditionalInformation();
        final String moName = moActionMTRRequest.getMoName();
        final long maxTimeout = getMaxWaitTime(platform);
        return new ShmEcimMoActionAttributes(activityJobId, moFdn, actionName, namespace, mimVersion, moActionAttributes, additionalInformation, moName, maxTimeout);
    }

    /**
     * @param platform
     * @return
     */
    private long getMaxWaitTime(final String platform) {
        return System.currentTimeMillis() + activityConfiguration.getOperationTimeOutBasedOnPlatformType(platform);
    }
}
