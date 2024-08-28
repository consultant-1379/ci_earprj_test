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
package com.ericsson.oss.services.shm.job.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.common.annotations.PlatformQualifier;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.api.DomainTypeProvider;

@ApplicationScoped
public class DomainTypeProviderFactory {

    @Inject
    @Any
    private Instance<DomainTypeProvider> domainTypeProvider;

    public DomainTypeProvider getDomainTypeProvider(final PlatformTypeEnum platformType) {
        final Instance<DomainTypeProvider> domainType = domainTypeProvider.select(new PlatformQualifier(platformType));
        if (domainType != null && !domainType.isUnsatisfied()) {
            return domainType.get();
        } else {
            return null;
        }
    }

}
