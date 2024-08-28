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
package com.ericsson.oss.services.shm.es.impl.axe.backup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.inventory.remote.axe.api.AxeApgProductRevisionProvider;
import com.ericsson.oss.services.shm.jobs.common.constants.ShmConstants;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class ApgProductRevisionProviderImpl {
    
    @EServiceRef
    AxeApgProductRevisionProvider axeApgProductRevisionProvider;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ApgProductRevisionProviderImpl.class);

    public String readNeProductRevisionValueFromSwInventory(final String componentName) {
        String neApgVersion = null;
        List<String> apgComponentsList = getComponentListToFetchProductRevision(componentName);
        Map<String, String> neComponentWithApgVersion = new HashMap<>();
        try {
            neComponentWithApgVersion = axeApgProductRevisionProvider.getApgComponentsProductRevision(apgComponentsList);
        } catch (Exception e) {
            LOGGER.error("Unable to get apg version data from inventory remote call for components {} due to {} ", apgComponentsList, e.getMessage());
        }
        if (neComponentWithApgVersion != null && !neComponentWithApgVersion.isEmpty()) {
            for (Map.Entry<String, String> neapgVersionDetails : neComponentWithApgVersion.entrySet()) {
                neApgVersion = neapgVersionDetails.getValue();
                LOGGER.debug("product Revision value is {} for component: {}", neapgVersionDetails.getValue(), neapgVersionDetails.getKey());
            }
        }
        return neApgVersion;
    }
    
    /**
     * For AXE nodes having single APG, SoftwareVer MO can have componentname as parentname__APG/APG1/APG2 so sending all 3 names to read the MO data.
     * @param componentName
     */
    private List<String> getComponentListToFetchProductRevision(String componentName) {
        if (componentName.endsWith(ShmConstants.APG_COMPONENT_IDENTIFIER_FROM_UI)) {
            componentName = componentName.replace(ShmConstants.DELIMITER_DOUBLE_UNDERSCORE, ShmConstants.DELIMITER_UNDERSCORE);
            List<String> singleApgComps = new ArrayList<>();
            singleApgComps.add(componentName);
            singleApgComps.add(componentName + "1");
            singleApgComps.add(componentName + "2");
            return singleApgComps; 
        } else {
            componentName = componentName.replace(ShmConstants.DELIMITER_DOUBLE_UNDERSCORE, ShmConstants.DELIMITER_UNDERSCORE);
            return Arrays.asList(componentName);
        }
    }
    
}
