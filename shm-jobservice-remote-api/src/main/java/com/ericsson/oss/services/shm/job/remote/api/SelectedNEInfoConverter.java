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
import java.util.List;

/**
 * As we cannot pass a local parameters like NetworkElement (directly or indirectly) to a remote business method, defining NetworkElementConverter attribute here which is duplicate of NetworkElement
 * pojo
 * 
 * @author xkalkil
 *
 */
public class SelectedNEInfoConverter implements Serializable {
    private static final long serialVersionUID = 1234567L;
    private final List<String> collectionNames;
    private final List<NetworkElementConverter> networkElements;
    private final List<String> savedSearchIds;

    public SelectedNEInfoConverter(final List<String> collectionNames, final List<NetworkElementConverter> networkElementConverter, final List<String> savedSearchIds) {
        this.collectionNames = collectionNames;
        this.networkElements = networkElementConverter;
        this.savedSearchIds = savedSearchIds;
    }

    public List<String> getCollectionNames() {
        return collectionNames;
    }

    public List<NetworkElementConverter> getNetworkElementConverter() {
        return networkElements;
    }

    public List<String> getSavedSearchIds() {
        return savedSearchIds;
    }

}
