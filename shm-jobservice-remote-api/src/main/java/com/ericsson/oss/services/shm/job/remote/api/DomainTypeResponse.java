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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;
import java.util.List;

import com.ericsson.oss.services.shm.job.remote.api.errorcodes.DomainTypeResponseCode;

/**
 * Class holds response of DomainType.
 * 
 * @author xnagvar
 * 
 */
public class DomainTypeResponse implements Serializable {

    private static final long serialVersionUID = -140098741020580880L;
    private String neType;
    private String neName;
    private List<String> domainTypeList;
    private DomainTypeResponseCode domainTypeResponseCode;
    private ShmJobResponseResult shmJobResponseResult;

    /**
     * @return the neType
     */
    public String getNeType() {
        return neType;
    }

    /**
     * @param neType
     *            the neType to set
     */
    public void setNeType(final String neType) {
        this.neType = neType;
    }

    /**
     * @return the neName
     */
    public String getNeName() {
        return neName;
    }

    /**
     * @param neName
     *            the neName to set
     */
    public void setNeName(final String neName) {
        this.neName = neName;
    }

    /**
     * @return the domainTypeList
     */
    public List<String> getDomainTypeList() {
        return domainTypeList;
    }

    /**
     * @param domainTypeList
     *            the domainTypeList to set
     */
    public void setDomainTypeList(final List<String> domainTypeList) {
        this.domainTypeList = domainTypeList;
    }

    /**
     * @return the domainTypeResponseCode
     */
    public DomainTypeResponseCode getDomainTypeResponseCode() {
        return domainTypeResponseCode;
    }

    /**
     * @param domainTypeResponseCode
     *            the domainTypeResponseCode to set
     */
    public void setDomainTypeResponseCode(final DomainTypeResponseCode domainTypeResponseCode) {
        this.domainTypeResponseCode = domainTypeResponseCode;
    }

    /**
     * @return the shmJobResponseResult
     */
    public ShmJobResponseResult getDomainTypeErrorCode() {
        return shmJobResponseResult;
    }

    /**
     * @param shmJobResponseResult
     *            the shmJobResponseResult to set
     */
    public void setDomainTypeErrorCode(final ShmJobResponseResult shmJobResponseResult) {
        this.shmJobResponseResult = shmJobResponseResult;
    }

}
