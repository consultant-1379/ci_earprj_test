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
package com.ericsson.oss.services.shm.es.impl.cpp.common;

/**
 * each instance of this POJO will represent a row in CV MO struct:CVActionAdditionalResultData
 * 
 * @author xchedoo
 * 
 */

public class CvActionAdditionalInfo {

    private String additionalInformation;
    private CVActionResultInformation information;

    /**
     * Default constructor.
     */
    public CvActionAdditionalInfo() {

    }

    public CvActionAdditionalInfo(final String additionalInformation, final CVActionResultInformation information) {
        this.additionalInformation = additionalInformation;
        this.information = information;
    }

    /**
     * @param additionalInformation
     *            the additionalInformation to set
     */
    public void setAdditionalInformation(final String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    /**
     * @param information
     *            the information to set
     */
    public void setInformation(final CVActionResultInformation information) {
        this.information = information;
    }

    /**
     * @return the additionalInformation
     */
    public String getAdditionalInformation() {
        return additionalInformation;
    }

    /**
     * @return the information
     */
    public CVActionResultInformation getInformation() {
        return information;
    }

}
