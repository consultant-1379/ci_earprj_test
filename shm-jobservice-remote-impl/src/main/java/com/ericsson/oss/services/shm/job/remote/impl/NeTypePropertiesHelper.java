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
package com.ericsson.oss.services.shm.job.remote.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

/**
 * This is a helper to get NE type Properties.
 * 
 * @author xnagvar
 * 
 */

public class NeTypePropertiesHelper {

    public List<Map<String, Object>> getNeTypeProperties(final List<String> activityProperties, final List<Map<String, Object>> platformSpecificProperties) {
        final List<Map<String, Object>> neTypeProperties = new ArrayList<Map<String, Object>>();
        for (String activityProperty : activityProperties) {
            for (Map<String, Object> property : platformSpecificProperties) {
                if (isNeTypeSpecificProperty(activityProperty, property.get(ShmConstants.KEY).toString()) && property.get(ShmConstants.VALUE) != null) {
                    neTypeProperties.add(createPropertyMap(activityProperty, property.get(ShmConstants.VALUE).toString()));
                }
            }
        }
        return neTypeProperties;
    }

    private static boolean isNeTypeSpecificProperty(final String activityProperty, final String neTypeSpecificProperty) {
        return activityProperty.equals(neTypeSpecificProperty) ? true : false;
    }

    public Map<String, Object> createPropertyMap(final String key, final Object value) {
        final HashMap<String, Object> property = new HashMap<String, Object>();
        property.put(ShmConstants.KEY, key);
        property.put(ShmConstants.VALUE, value);
        return property;
    }
}
