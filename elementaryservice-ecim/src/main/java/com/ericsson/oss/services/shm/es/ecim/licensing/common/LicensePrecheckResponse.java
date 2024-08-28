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
package com.ericsson.oss.services.shm.es.ecim.licensing.common;

import com.ericsson.oss.services.shm.es.api.ActivityStepResultEnum;
import com.ericsson.oss.services.shm.es.common.PrecheckResponse;
import com.ericsson.oss.services.shm.model.NetworkElementData;

public class LicensePrecheckResponse extends PrecheckResponse {

    private final EcimLicensingInfo ecimLicensingInfo;

    private final NetworkElementData networkElement;

    public LicensePrecheckResponse(final EcimLicensingInfo ecimLicensingInfo, final NetworkElementData networkElement, final ActivityStepResultEnum activityStepResultEnum) {
        super(activityStepResultEnum);
        this.ecimLicensingInfo = ecimLicensingInfo;
        this.networkElement = networkElement;
    }

    public EcimLicensingInfo getEcimLicenseInfo() {
        return ecimLicensingInfo;
    }

    public NetworkElementData getNetworkElement() {
        return networkElement;
    }

}
