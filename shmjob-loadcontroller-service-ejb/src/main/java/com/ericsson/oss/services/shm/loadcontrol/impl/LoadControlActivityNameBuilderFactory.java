/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.jobs.common.enums.JobTypeEnum;

/**
 * Implementation to fetch the LoadControl ActivityName Builder class specific to input platform type
 * 
 */
@ApplicationScoped
public class LoadControlActivityNameBuilderFactory {

    @Inject
    @PlatformAnnotation(name = PlatformTypeEnum.AXE)
    LoadControlActivityNameBuilder axeLoadControlActivityNameBuilder;

    @Inject
    LoadControlActivityNameBuilder commonLoadControlActivityNameBuilder;
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadControlActivityNameBuilderFactory.class);
    /**
     * Method to fetch the LoadControl Activity Name Builder class specific to input platform type.
     */
    public LoadControlActivityNameBuilder getLoadControlActivityNameBuilder(final String platformType, final String jobType) {
        LoadControlActivityNameBuilder loadControlActivityNameBuilderInstance = commonLoadControlActivityNameBuilder;
        if (PlatformTypeEnum.AXE.getName().equals(platformType) && JobTypeEnum.UPGRADE.getAttribute().equals(jobType)) {
            loadControlActivityNameBuilderInstance = axeLoadControlActivityNameBuilder;
        }
        LOGGER.trace(" Received loadControlActivityNameBuilderInstance  {}", loadControlActivityNameBuilderInstance);
        return loadControlActivityNameBuilderInstance;
    }
}
