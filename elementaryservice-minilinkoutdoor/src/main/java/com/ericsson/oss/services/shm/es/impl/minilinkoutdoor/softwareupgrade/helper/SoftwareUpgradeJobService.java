/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.minilinkoutdoor.softwareupgrade.helper;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.common.job.utils.JobPropertyUtils;
import com.ericsson.oss.services.shm.es.api.UpgradeActivityConstants;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.job.api.JobConfigurationServiceRetryProxy;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

/**
 * Service to build upgrade information required for SHM Upgrade
 * 
 * @author NightsWatch
 *
 */

public class SoftwareUpgradeJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoftwareUpgradeJobService.class);

    @Inject
    private JobConfigurationServiceRetryProxy jobConfigServiceRetryProxy;

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    protected FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    @Inject
    private JobPropertyUtils jobPropertyUtils;

    /**
     * Builds MINI-LINK Outdoor upgrade information based on job properties for the activity job id
     * 
     * @param activityJobId
     * @return UpgradeInformation
     */
    public UpgradeJobInformation buildUpgradeInformation(final long activityJobId) {

        NEJobStaticData neJobStaticData = null;
        final Map<String, Object> neJobAttributes = jobConfigServiceRetryProxy.getActivityJobAttributes(activityJobId);
        LOGGER.debug("JobAttributes for Mini-Link Outdoor : {} ", neJobAttributes);

        try {
            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final String nodeName = neJobStaticData.getNodeName();
            LOGGER.debug("NodeName fetched from neJobStaticDataProvider is : {}", nodeName);
            final NetworkElementData networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            final String nodeFdn = networkElement.getNeFdn();
            final String neType = networkElement.getNeType();
            final String platformtype = neJobStaticData.getPlatformType();
            LOGGER.debug("NodeFdn fetched from Mini-Link Outdoor networkElementRetrivalBean is : {}", nodeFdn);
            final String fileName = jobPropertyUtils.getPropertyValue(Collections.singletonList(UpgradeActivityConstants.SWP_NAME), activityUtils.getJobConfigurationDetails(activityJobId), nodeName, neType, platformtype).get(
                    UpgradeActivityConstants.SWP_NAME);
            final String productRevision = jobPropertyUtils.getPropertyValue(Collections.singletonList(UpgradeActivityConstants.UP_PO_PROD_REVISION),
                    activityUtils.getJobConfigurationDetails(activityJobId), nodeName, neType, platformtype).get(UpgradeActivityConstants.UP_PO_PROD_REVISION);
            final String productNumber = jobPropertyUtils.getPropertyValue(Collections.singletonList(UpgradeActivityConstants.UP_PO_PROD_NUMBER),
                    activityUtils.getJobConfigurationDetails(activityJobId), nodeName, neType, platformtype).get(UpgradeActivityConstants.UP_PO_PROD_NUMBER);
            final UpgradeJobInformation softwareUpgradeInformation = new UpgradeJobInformation(nodeFdn, nodeName);
            softwareUpgradeInformation.setFileName(fileName);
            softwareUpgradeInformation.setProductRevision(productRevision);
            softwareUpgradeInformation.setProductNumber(productNumber);
            LOGGER.debug("Mini-Link Outdoor ActivityJob ID - [{}] : software upgrade information built is: {}", activityJobId, softwareUpgradeInformation);
            return softwareUpgradeInformation;

        } catch (JobDataNotFoundException ex) {
            LOGGER.error("Error while getting nodeName {} due to {}", activityJobId, ex);
        } catch (MoNotFoundException ex) {
            LOGGER.error("Failed to fetch node Fdn of {} due to {}", activityJobId, ex);
        }
        return null;
    }

}
