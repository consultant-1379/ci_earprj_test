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
package com.ericsson.oss.services.shm.job.remote.api;

import java.io.Serializable;

/**
 * As we cannot pass a local parameter like ProductData (directly or indirectly) to a remote business method, defining ProductDataConverter here as a duplicate of ProductData pojo
 * 
 * @author xkalkil
 *
 */
public class ProductDataConverter implements Serializable {
    private static final long serialVersionUID = 1234567L;
    private final String revision;
    private final String identity;

    public ProductDataConverter(final String revision, final String identity) {
        this.revision = revision;
        this.identity = identity;
    }

    public String getRevision() {
        return revision;
    }

    public String getIdentity() {
        return identity;
    }

}
