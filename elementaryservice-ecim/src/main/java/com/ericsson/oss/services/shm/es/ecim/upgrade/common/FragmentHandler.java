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
package com.ericsson.oss.services.shm.es.ecim.upgrade.common;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.exception.ecim.UnsupportedFragmentException;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfo;
import com.ericsson.oss.services.shm.common.modelservice.api.OssModelInfoProvider;
import com.ericsson.oss.services.shm.ecim.common.FragmentType;
import com.ericsson.oss.services.shm.ecim.common.FragmentVersionCheck;
import com.ericsson.oss.services.shm.es.impl.JobLogConstants;
import com.ericsson.oss.services.shm.jobs.common.api.JobLogLevel;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;
import com.ericsson.oss.services.shm.shared.enums.JobLogType;
import com.ericsson.oss.services.shm.shared.util.JobLogUtil;

public class FragmentHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FragmentHandler.class);

    @Inject
    private OssModelInfoProvider ossModelInfoProvider;

    @Inject
    private FragmentVersionCheck fragmentVersionCheck;

    @Inject
    private JobLogUtil jobLogUtil;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrievalBean;

    public void logNodeFragmentInfo(final String nodeName, final FragmentType fragmentType, final List<Map<String, Object>> jobLogList) {
        String logMessage = null;

        try {
            final NetworkElementData networkElementData = networkElementRetrievalBean.getNetworkElementData(nodeName);
            final OssModelInfo ossModelInfo = ossModelInfoProvider.getOssModelInfo(networkElementData.getNeType(), networkElementData.getOssModelIdentity(), fragmentType.getFragmentName());
            if (ossModelInfo != null) {
                logMessage = fragmentVersionCheck.checkFragmentVersion(fragmentType, ossModelInfo.getReferenceMIMVersion());
                if (logMessage != null) {
                    jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.INFO.toString());
                }
            } else {
                logMessage = String.format(JobLogConstants.UNSUPPORTED_NODE_MODEL, networkElementData.getNeType(), networkElementData.getOssModelIdentity());
                jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
            }
        } catch (final MoNotFoundException | UnsupportedFragmentException ex) {
            LOGGER.error("Exception occurred while fetching node:{} fragment information, Reason:", nodeName, ex);
            logMessage = ex.getMessage();
            jobLogUtil.prepareJobLogAtrributesList(jobLogList, logMessage, new Date(), JobLogType.SYSTEM.toString(), JobLogLevel.ERROR.toString());
        }
    }
}
