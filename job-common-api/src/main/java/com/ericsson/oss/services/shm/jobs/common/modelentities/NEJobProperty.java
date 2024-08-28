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
package com.ericsson.oss.services.shm.jobs.common.modelentities;

import java.io.Serializable;
import java.util.List;

public class NEJobProperty implements Serializable {
    private static final long serialVersionUID = 1234567L;

    private String neName;
    private List<JobProperty> jobProperties;

    /**
     * @return the jobProperties
     */
    public List<JobProperty> getJobProperties() {
        return jobProperties;
    }

    /**
     * @param jobProperties
     *            the jobProperties to set
     */
    public void setJobProperties(final List<JobProperty> jobProperties) {
        this.jobProperties = jobProperties;
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

}
