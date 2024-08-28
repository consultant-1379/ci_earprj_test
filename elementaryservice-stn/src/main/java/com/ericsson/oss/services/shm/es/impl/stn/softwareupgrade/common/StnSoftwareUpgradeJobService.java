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
package com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.common;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.exception.MoNotFoundException;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.stn.softwareupgrade.constants.StnJobConstants;
import com.ericsson.oss.services.shm.model.NetworkElementData;
import com.ericsson.oss.services.shm.nejob.cache.JobDataNotFoundException;
import com.ericsson.oss.services.shm.nejob.cache.NEJobStaticData;
import com.ericsson.oss.services.shm.nejob.cache.NeJobStaticDataProvider;
import com.ericsson.oss.services.shm.networkelement.cache.impl.NetworkElementRetrievalBean;

/**
 * Service to build upgrade information required for SHM Upgrade
 * 
 * @author xgowbom
 *
 */

public class StnSoftwareUpgradeJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StnSoftwareUpgradeJobService.class);

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    private StnJobActivityServiceHelper stnJobActivityService;

    @Inject
    protected FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private NeJobStaticDataProvider neJobStaticDataProvider;

    @Inject
    private NetworkElementRetrievalBean networkElementRetrivalBean;

    /**
     * Builds STN upgrade information based on job properties for the activity job id
     * 
     * @param activityJobId
     * @return stnUpgradeInformation
     */
    public StnJobInformation buildStnUpgradeInformation(final long activityJobId) {

        String stnPackagename = null;
        String filepath;
        NEJobStaticData neJobStaticData = null;
        final JobEnvironment jobContext = activityUtils.getJobEnvironment(activityJobId);
        final Map<String, Object> neJobAttributes = jobContext.getNeJobAttributes();
        LOGGER.debug("JobAttributes are : {}", neJobAttributes);

        try {

            neJobStaticData = neJobStaticDataProvider.getNeJobStaticData(activityJobId, SHMCapabilities.UPGRADE_JOB_CAPABILITY);
            final String nodeName = neJobStaticData.getNodeName();
            LOGGER.debug("NodeName fetched from neJobStaticDataProvider is : {}", nodeName);
            final NetworkElementData networkElement = networkElementRetrivalBean.getNetworkElementData(nodeName);
            final String nodeFdn = networkElement.getNeFdn();
            LOGGER.debug("NodeFdn fetched from networkElementRetrivalBean is : {}", nodeFdn);
            final List<Map<String, Object>> activityJobProperties = stnJobActivityService.getActivityJobAttributes(jobContext);
            filepath = (String) stnJobActivityService.getJobPropertyValue(StnJobConstants.STN_PACKAGE_FILE_PATH, activityJobProperties);
            final String productRevision = (String) stnJobActivityService.getJobPropertyValue(StnJobConstants.PRODUCT_REVISION, activityJobProperties);
            final String productNumber = (String) stnJobActivityService.getJobPropertyValue(StnJobConstants.PRODUCT_NUMBER, activityJobProperties);
            filepath = filepath + StnJobConstants.SLASH + productRevision + StnJobConstants.UNDERSCORE + productNumber + StnJobConstants.SLASH;
            stnPackagename = (String) stnJobActivityService.getJobPropertyValue(StnJobConstants.STN_PACKAGE_NAME, activityJobProperties);
            final String filename = (String) stnJobActivityService.getJobPropertyValue(StnJobConstants.SOFTWAREPACKAGE_FILENAME, activityJobProperties);
            final StnJobInformation stnSoftwareUpgradeInformation = new StnJobInformation(jobContext, nodeFdn, nodeName, stnPackagename);
            stnSoftwareUpgradeInformation.setPackageLocation(filepath);
            stnSoftwareUpgradeInformation.setFileName(filename);
            stnSoftwareUpgradeInformation.setNeJobId(neJobStaticData.getNeJobId());
            LOGGER.debug("ActivityJob ID - [{}] : software upgrade information built is: {}", activityJobId, stnSoftwareUpgradeInformation);
            return stnSoftwareUpgradeInformation;

        } catch (JobDataNotFoundException ex) {
            LOGGER.error("Error while getting nodeName {} due to {}", activityJobId, ex);
        } catch (MoNotFoundException ex) {
            LOGGER.error("Failed to fetch node Fdn of {} due to {}", activityJobId, ex);
        }
        return null;
    }

}
