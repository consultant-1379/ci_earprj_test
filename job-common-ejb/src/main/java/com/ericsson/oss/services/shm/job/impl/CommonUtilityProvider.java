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
package com.ericsson.oss.services.shm.job.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonUtilityProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonUtilityProvider.class);

    private CommonUtilityProvider() {
    }

    public static String convertListToString(final List<Map<String, Object>> map) {
        String listOfDataAsString = null;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            listOfDataAsString = mapper.writeValueAsString(map);
        } catch (Exception exception) {
            LOGGER.error("conversion of List to String is failed, reason : ", exception);
        }
        return listOfDataAsString;
    }

    public static List<Map<String, Object>> convertStringToList(final String input) {
        final ObjectMapper mapperObject = new ObjectMapper();
        final List<Map<String, Object>> list = new ArrayList<>();
        try {
            return mapperObject.readValue(input, new TypeReference<List<Map<String, Object>>>() {
            });

        } catch (Exception exception) {
            LOGGER.error("conversion of String to List is failed, reason : {}", exception);
        }
        return list;
    }

}
