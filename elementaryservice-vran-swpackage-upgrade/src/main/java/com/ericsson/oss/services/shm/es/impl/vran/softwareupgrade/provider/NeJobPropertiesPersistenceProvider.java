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
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.provider;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.exception.NoNetworkElementAssociatedException;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;
import com.ericsson.oss.services.shm.vran.shared.persistence.JobAttributesPersistenceProvider;

public class NeJobPropertiesPersistenceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeJobPropertiesPersistenceProvider.class);

    @Inject
    private ActivityUtils activityUtils;

    @Inject
    protected FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    @Inject
    private VnfInformationProvider vnfInformationProvider;

    @Inject
    private VnfPackageDataProvider vnfPackageDataProvider;

    @Inject
    private JobAttributesPersistenceProvider jobAttributesPersistenceProvider;

    @Inject
    private NeJobPropertiesBuilder neJobPropertiesBuilder;

    public void persistVnfJobId(final UpgradePackageContext upgradePackageContext, final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse, final JobEnvironment jobContext) {
        final List<Map<String, Object>> neJobProperties = neJobPropertiesBuilder.buildVnfJobId(upgradePackageContext, vranSoftwareUpgradeJobResponse);
        persist(jobContext, neJobProperties);
    }

    public void persistVnfInformation(final long activityJobId) {
        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final List<NetworkElement> networkElements = vnfInformationProvider.fetchNetworkElements(jobEnvironment);
        if (networkElementsExists(networkElements)) {

            final List<Map<String, Object>> vnfPackageDetails = vnfPackageDataProvider.fetchVnfPackageDetails(jobEnvironment, networkElements);

            final String vnfId = vnfInformationProvider.getVnfId(networkElements);

            LOGGER.debug("ActivityJob ID - [{}] : VNF package details retrieved for vnf id {} are {}", activityJobId, vnfId, vnfPackageDetails);

            final Map<String, Object> vnfIdProperty = vranJobActivityServiceHelper.buildJobProperty(VranJobConstants.VNF_ID, vnfId);

            persist(jobEnvironment, neJobPropertiesBuilder.buildVnfPackageAndVnfId(vnfPackageDetails, vnfIdProperty, jobEnvironment));
        } else {
            LOGGER.debug("ActivityJob ID - [{}] : Network Elements do not exist for node {}", activityJobId, jobEnvironment.getNodeName());
            throw new NoNetworkElementAssociatedException("Network Element do not exist for node " + jobEnvironment.getNodeName());
        }
    }

    public void persistFromAndToVnfIds(final long activityJobId, final String fromVnfId, final String toVnfId, final JobEnvironment jobContext) {
        LOGGER.info("ActivityJob ID - [{}] : persisting vnfId  {} to nejobproperties", activityJobId, fromVnfId);

        final List<Map<String, Object>> neJobProperties = neJobPropertiesBuilder.buildFromVnfAndToVnfIds(fromVnfId, toVnfId, jobContext);
        persist(jobContext, neJobProperties);
    }

    private void persist(final JobEnvironment jobEnvironment, final List<Map<String, Object>> neJobProperties) {
        final long neJobId = jobEnvironment.getNeJobId();
        jobAttributesPersistenceProvider.persistJobProperties(neJobId, neJobProperties);
        LOGGER.debug("ActivityJob ID - [{}] : Updated vnf job id in jobProperties {}", neJobId, neJobProperties);
    }

    private boolean networkElementsExists(final List<NetworkElement> networkElements) {
        return !networkElements.isEmpty();
    }

}
