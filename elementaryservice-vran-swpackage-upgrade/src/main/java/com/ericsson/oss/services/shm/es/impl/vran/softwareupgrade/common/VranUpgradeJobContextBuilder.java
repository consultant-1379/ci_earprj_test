/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.FdnServiceBeanRetryHelper;
import com.ericsson.oss.services.shm.common.constants.SHMCapabilities;
import com.ericsson.oss.services.shm.common.modelservice.api.NetworkElement;
import com.ericsson.oss.services.shm.es.impl.ActivityUtils;
import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;
import com.ericsson.oss.services.shm.vran.common.VNFMInformationProvider;
import com.ericsson.oss.services.shm.vran.common.VranActivityUtil;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;

@Stateless
public class VranUpgradeJobContextBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(VranUpgradeJobContextBuilder.class);

    @Inject
    protected ActivityUtils activityUtils;

    @Inject
    private VranJobActivityServiceHelper vranJobActivityService;

    @Inject
    protected FdnServiceBeanRetryHelper fdnServiceBeanRetryHelper;

    @Inject
    private VNFMInformationProvider virtualNetwrokFunctionManagerInformationProvider;

    @Inject
    private VranActivityUtil vranActivityUtil;

    private String actionTriggered;
    private int vnfJobId;
    private String vnfDescriptorId;
    private String vnfPackageId;
    private String vnfId;

    public UpgradePackageContext build(final long activityJobId) {

        final JobEnvironment jobEnvironment = activityUtils.getJobEnvironment(activityJobId);
        final Map<String, Object> neJobAttributes = jobEnvironment.getNeJobAttributes();
        final String nodeName = jobEnvironment.getNodeName();

        final List<Map<String, Object>> activityJobProperties = vranJobActivityService.getActivityJobAttributes(jobEnvironment);
        final List<Map<String, Object>> neJobProperties = vranJobActivityService.getNeJobAttributes(jobEnvironment);
        final String businessKey = (String) neJobAttributes.get(ShmConstants.BUSINESS_KEY);

        final List<NetworkElement> networkElements = fetchNetworkElements(nodeName);
        final String nodeFdn = networkElements.get(0).getNetworkElementFdn();

        final String vnfmFdn = virtualNetwrokFunctionManagerInformationProvider.getVnfmFdn(nodeFdn);

        if (activityJobProperties != null && !activityJobProperties.isEmpty()) {
            actionTriggered = vranJobActivityService.getJobPropertyValueAsString(VranJobConstants.ACTION_TRIGGERED, activityJobProperties);
        }
        LOGGER.debug("ActivityJob ID - [{}] :  Action Triggered in VranUpgrade {}.", activityJobId, actionTriggered);

        extractValuesFromNeProperties(neJobProperties);

        final String vnfmName = vranActivityUtil.getNeNameFromFdn(vnfmFdn);

        final UpgradePackageContext upgradePackageContext = constructPackageContext(jobEnvironment, nodeName, businessKey, nodeFdn, vnfmFdn, vnfmName);

        LOGGER.debug("ActivityJob ID - [{}] : VRAN software upgrade information built is: {}", activityJobId, upgradePackageContext);

        return upgradePackageContext;
    }

    private void extractValuesFromNeProperties(final List<Map<String, Object>> neJobProperties) {
        if (neJobProperties != null && !neJobProperties.isEmpty()) {
            vnfDescriptorId = vranJobActivityService.getJobPropertyValueAsString(VranJobConstants.VNF_DESCRIPTOR_ID, neJobProperties);
            vnfPackageId = vranJobActivityService.getJobPropertyValueAsString(VranJobConstants.VNF_PACKAGE_ID, neJobProperties);
            vnfId = vranJobActivityService.getJobPropertyValueAsString(VranJobConstants.VNF_ID, neJobProperties);
            vnfJobId = vranJobActivityService.getJobPropertyValueAsInt(VranJobConstants.VNF_JOB_ID, neJobProperties);
        }
    }

    private UpgradePackageContext constructPackageContext(final JobEnvironment jobEnvironment, final String nodeName, final String businessKey, final String nodeFdn, final String vnfmFdn,
            final String vnfmName) {
        return new UpgradePackageContextBuilder().setActionTriggered(actionTriggered).setBusinessKey(businessKey).setJobEnvironment(jobEnvironment).setVnfJobId(vnfJobId).setNodeFdn(nodeFdn)
                .setNodeName(nodeName).setVnfId(vnfId).setSoftwarePackageName(vnfPackageId).setVnfPackageId(vnfPackageId).setVnfDescription(vnfDescriptorId).setVnfmFdn(vnfmFdn).setVnfmName(vnfmName)
                .buildUpgradePackageContext();

    }

    private List<NetworkElement> fetchNetworkElements(final String nodeName) {
        final List<String> neNames = new ArrayList<>();
        neNames.add(nodeName);
        return fdnServiceBeanRetryHelper.getNetworkElementsByNeNames(neNames, SHMCapabilities.UPGRADE_JOB_CAPABILITY);

    }

}
