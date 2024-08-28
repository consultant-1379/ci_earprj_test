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
package com.ericsson.oss.services.shm.notifications.impl.license;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.common.exception.UnsupportedPlatformException;
import com.ericsson.oss.services.shm.common.modelservice.ProductTypeProviderImpl;

@Stateless
public class LicenseUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseUtil.class);
    @Inject
    private ProductTypeProviderImpl productTypeProviderImpl;

    /**
     * method validates whether the LKF belongs to RadioNode or not.
     * 
     * @param lkfProductType
     * @param neType
     * @return
     */
    public boolean isRadioNodeLKF(final String lkfProductType, final String neType) {
        String mappedProductType = "";
        try {
            //As of now we have mapping only for Networkelement RadioNode and it's productType. So, getProductType returns productType is as BASEBAND because in the capibility the default value is set to BASEBAND
            mappedProductType = productTypeProviderImpl.getProductType(neType);
            LOGGER.debug("Product Type :{} for NeType :{}", mappedProductType, neType);
            if (lkfProductType.equalsIgnoreCase(mappedProductType)) {
                return true;
            }
        } catch (final UnsupportedPlatformException e) {
            LOGGER.error("ProductType not found for neType {}. Reason {}", neType, e);
        }
        return false;
    }

}
