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
package com.ericsson.oss.services.shm.jobexecutor;

import javax.inject.Inject;

import com.ericsson.oss.services.shm.jobexecutorlocal.TargetResolver;
import com.ericsson.oss.services.shm.jobs.vran.constants.VranConstants;

/**
 * 
 * @author xeswpot
 * 
 */
public class TargetResolverFactory {

    @Inject
    private NetworkElementResolver networkElementResolver;

    @Inject
    private NFVOResolver nfvoResolver;

    /**
     * 
     * @param platformType
     * @return
     */
    public TargetResolver getTargetResolver(final String jobType) {

        TargetResolver targetResolver = networkElementResolver;

        if (jobType != null) {
            switch (jobType) {
            case VranConstants.ONBOARD:
                targetResolver = nfvoResolver;
                break;
            case VranConstants.DELETE_SOFTWAREPACKAGE:
                targetResolver = nfvoResolver;
                break;
            default:
                targetResolver = networkElementResolver;
                break;
            }
        }
        return targetResolver;
    }
}
