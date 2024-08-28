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
package com.ericsson.oss.services.shm.jobservice.common;

import java.io.Serializable;


public class JobProperty implements Serializable {

	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = -3260278994679152774L;

	private JobPropertyPK jobPropertyKey;


    private String value;

    public JobProperty()
    {
        // Default no arg constructor.
    }

    public JobProperty(final String name, final String value)
    {
        this.jobPropertyKey = new JobPropertyPK();
        this.jobPropertyKey.setName(name);
        this.value = value;
    }

    public JobPropertyPK getJobPropertyKey()
    {
        return jobPropertyKey;
    }

    public void setJobPropertyKey(final JobPropertyPK jobPropertyKey)
    {
        this.jobPropertyKey = jobPropertyKey;
    }

    public String getName()
    {
        return this.jobPropertyKey.getName();
    }

    public void setName(final String name)
    {
        if (jobPropertyKey == null) {
            jobPropertyKey = new JobPropertyPK();
        }
        this.jobPropertyKey.setName(name);
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(final String value)
    {
        this.value = value;
    }

}
