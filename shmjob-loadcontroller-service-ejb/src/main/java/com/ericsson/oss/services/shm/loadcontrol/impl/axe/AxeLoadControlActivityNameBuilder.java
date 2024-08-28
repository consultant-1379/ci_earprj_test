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
package com.ericsson.oss.services.shm.loadcontrol.impl.axe;

import com.ericsson.oss.services.shm.common.annotations.PlatformAnnotation;
import com.ericsson.oss.services.shm.common.enums.PlatformTypeEnum;
import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControlActivityNameBuilder;

/**
 * Implementation to return a constant value as Activity Name in case of AXE platform
 * 
 */
@PlatformAnnotation(name = PlatformTypeEnum.AXE)
public class AxeLoadControlActivityNameBuilder implements LoadControlActivityNameBuilder {

    public static final String AXE_ACTIVITY = "axeActivity";
   
    @Override
    public String buildActivityName(final String activityName) {
        return AXE_ACTIVITY;
    }

}
