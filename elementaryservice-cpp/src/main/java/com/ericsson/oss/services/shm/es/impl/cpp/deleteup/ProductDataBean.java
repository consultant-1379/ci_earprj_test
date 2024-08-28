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
package com.ericsson.oss.services.shm.es.impl.cpp.deleteup;

public class ProductDataBean {

    private String productNumber ; 
    private String productRevision;
    /**
     * @return the productNumber
     */
    public String getProductNumber() {
        return productNumber;
    }
    
    /**
     * @param productNumber the productNumber to set
     */
    public void setProductNumber(final String productNumber) {
        this.productNumber = productNumber;
    }
    
    /**
     * @return the productRevision
     */
    public String getProductRevision() {
        return productRevision;
    }
    
    /**
     * @param productRevision the productRevision to set
     */
    public void setProductRevision(final String productRevision) {
        this.productRevision = productRevision;
    }
    
}
