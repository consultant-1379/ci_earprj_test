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
package com.ericsson.oss.services.shm.loadcontrol.impl.common;

import com.ericsson.oss.services.shm.loadcontrol.impl.LoadControlActivityNameBuilder;

/**
 * Implementation to return the same Activity Name in case of all other platforms apart from AXE
 * 
 */
public class CommonLoadControlActivityNameBuilder implements LoadControlActivityNameBuilder {

    @Override
    public String buildActivityName(final String activityName) {
        return activityName;
    }

}
