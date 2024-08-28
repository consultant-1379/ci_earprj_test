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
package com.ericsson.oss.services.shm.es.impl.ecim.licenserefresh;

import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.REQUEST_ID;
import static com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants.REQUEST_TYPE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnServiceBean;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.ecim.licenserefresh.common.LicenseRefreshConstants;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.shared.constants.ActivityConstants;

public class LkfRefreshRequestResultBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(LkfRefreshRequestResultBuilder.class);

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigurationServiceRetryProxy;

    @Inject
    private FdnServiceBean fdnServiceBean;

    public Map<String, Object> buildLkfRequestResultForSuccess(final NEJobStaticData neJobStaticData) {
        final Map<String, Object> lkfRefreshRequestResultProperties = buildLkfRefreshRequestResultProperties(neJobStaticData);
        lkfRefreshRequestResultProperties.put(LicenseRefreshConstants.RESULT_CODE, LicenseRefreshConstants.LKF_REFRESH_COMPLETED);
        LOGGER.debug("LicenseRefreshJob: Building attributes of Lkf Request Result for success scenario : {}", lkfRefreshRequestResultProperties);
        return lkfRefreshRequestResultProperties;
    }

    public Map<String, Object> buildLkfRequestResultForFailure(final NEJobStaticData neJobStaticData, final String errorInfo) {
        final Map<String, Object> lkfRefreshRequestResultProperties = buildLkfRefreshRequestResultProperties(neJobStaticData);
        lkfRefreshRequestResultProperties.put(LicenseRefreshConstants.RESULT_CODE, LicenseRefreshConstants.LKF_REFRESH_REJECTED);
        lkfRefreshRequestResultProperties.put(LicenseRefreshConstants.REQUEST_INFO, errorInfo);
        LOGGER.debug("LicenseRefreshJob: Building attributes of Lkf Request Result for failure scenario : {}", lkfRefreshRequestResultProperties);
        return lkfRefreshRequestResultProperties;
    }

    private Map<String, Object> buildLkfRefreshRequestResultProperties(final NEJobStaticData neJobStaticData) {
        final Map<String, Object> lkfRefreshRequestResultProperties = new HashMap<>();
        final Map<String, Object> neJobAttributes = jobConfigurationServiceRetryProxy.getNeJobAttributes(neJobStaticData.getNeJobId());
        if (neJobAttributes != null && neJobAttributes.size() > 0) {
            final List<Map<String, String>> neJobProperties = (List<Map<String, String>>) neJobAttributes.get(ActivityConstants.JOB_PROPERTIES);
            if (neJobProperties != null && !neJobProperties.isEmpty()) {
                for (Map<String, String> jobproperty : neJobProperties) {
                    if (jobproperty.get(ActivityConstants.JOB_PROP_KEY).equalsIgnoreCase(REQUEST_ID)) {
                        lkfRefreshRequestResultProperties.put(jobproperty.get(ActivityConstants.JOB_PROP_KEY), Integer.valueOf(jobproperty.get(ActivityConstants.JOB_PROP_VALUE)));
                    } else if (jobproperty.get(ActivityConstants.JOB_PROP_KEY).equalsIgnoreCase(REQUEST_TYPE)) {
                        lkfRefreshRequestResultProperties.put(jobproperty.get(ActivityConstants.JOB_PROP_KEY), jobproperty.get(ActivityConstants.JOB_PROP_VALUE));
                    }
                }
                final List<NetworkElement> nodeFdn = fdnServiceBean.getNetworkElementsByNeNames(Arrays.asList(neJobStaticData.getNodeName()));
                lkfRefreshRequestResultProperties.put("NODE_NAME", nodeFdn.get(0).getNodeRootFdn());
            }
        }
        return lkfRefreshRequestResultProperties;
    }
}
