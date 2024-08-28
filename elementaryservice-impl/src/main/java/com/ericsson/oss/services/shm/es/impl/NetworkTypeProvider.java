/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.es.impl;

import com.ericsson.nms.security.smrs.api.NetworkType;
import com.ericsson.oss.itpf.sdk.instrument.annotation.Profiled;
import com.ericsson.oss.itpf.sdk.tracing.annotation.Traceable;

@Traceable
@Profiled
public class NetworkTypeProvider {

    private static final String LRAN = "lran";

    public NetworkType getNetworkType() {
        final NetworkType networkType = NetworkType.getEnumForString(LRAN);
        return networkType;
    }

}
