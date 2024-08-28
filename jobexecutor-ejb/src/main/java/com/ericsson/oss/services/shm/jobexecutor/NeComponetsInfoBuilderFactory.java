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
package com.ericsson.oss.services.shm.jobexecutor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.services.shm.activities.NeComponentBuilder;
import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;

/**
 * Implementation to fetch the NeComponent Builder class specific to input platform type
 * 
 * @author xaniama
 * 
 */
@ApplicationScoped
public class NeComponetsInfoBuilderFactory {

    @Inject
    @PlatformAnnotation(name = PlatformTypeEnum.AXE)
    NeComponentBuilder axeNeComponentBuilder;

    @Inject
    NeComponentBuilder commonNeComponentBuilder;

    /**
     * Method to fetch the Ne Component Builder class specific to input platform type.
     * 
     * @return specific implementation of NeComponentBuilder or null in case of non-existence of validator.
     */
    public NeComponentBuilder getNeComponentBuilderInstance(final PlatformTypeEnum platformType) {
        NeComponentBuilder neComponentBuilderInstance = null;
        if (PlatformTypeEnum.AXE.equals(platformType)) {
            neComponentBuilderInstance = axeNeComponentBuilder;
        } else {
            neComponentBuilderInstance = commonNeComponentBuilder;
        }
        return neComponentBuilderInstance;
    }
}
