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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.es.impl.JobEnvironment;
import com.ericsson.oss.services.shm.es.impl.vran.softwareupgrade.common.UpgradePackageContext;
import com.ericsson.oss.services.shm.es.vran.notifications.api.VranSoftwareUpgradeJobResponse;
import com.ericsson.oss.services.shm.vran.constants.VranJobConstants;
import com.ericsson.oss.services.shm.vran.shared.VranJobActivityServiceHelper;

public class NeJobPropertiesBuilder {

    @Inject
    private VranJobActivityServiceHelper vranJobActivityServiceHelper;

    public List<Map<String, Object>> buildVnfJobId(final UpgradePackageContext upgradePackageContext, final VranSoftwareUpgradeJobResponse vranSoftwareUpgradeJobResponse) {
        final List<Map<String, Object>> neJobProperties = vranJobActivityServiceHelper.getNeJobAttributes(upgradePackageContext.getJobContext());

        final Map<String, Object> vnfJobId = vranJobActivityServiceHelper.buildJobProperty(VranJobConstants.VNF_JOB_ID, Integer.toString(vranSoftwareUpgradeJobResponse.getJobId()));
        neJobProperties.add(vnfJobId);

        return neJobProperties;
    }

    public List<Map<String, Object>> buildVnfPackageAndVnfId(final List<Map<String, Object>> vnfPackageDetails, final Map<String, Object> vnfIdProperty, final JobEnvironment jobContext) {
        List<Map<String, Object>> neJobProperties = vranJobActivityServiceHelper.getNeJobAttributes(jobContext);
        if (neJobProperties == null) {
            neJobProperties = new ArrayList<>();
        }
        neJobProperties.add(vnfIdProperty);
        neJobProperties.addAll(vnfPackageDetails);
        return neJobProperties;
    }

    public List<Map<String, Object>> buildFromVnfAndToVnfIds(final String fromVnfId, final String toVnfId, final JobEnvironment jobContext) {
        final List<Map<String, Object>> neJobProperties = vranJobActivityServiceHelper.getNeJobAttributes(jobContext);
        neJobProperties.add(vranJobActivityServiceHelper.buildJobProperty(VranJobConstants.VNF_ID, fromVnfId));
        neJobProperties.add(vranJobActivityServiceHelper.buildJobProperty(VranJobConstants.TO_VNF_ID, toVnfId));
        return neJobProperties;
    }

}
