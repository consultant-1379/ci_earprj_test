/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson AB. The programs may be used and/or copied only with written
 * permission from Ericsson AB. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.loadcontrol.local.api;

import java.io.Serializable;
import java.util.List;

import com.ericsson.oss.services.shm.es.api.SHMStagedActivityRequest;

public class StagedActivityRequestBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<SHMStagedActivityRequest> shmStagedActivityRequest;

    public List<SHMStagedActivityRequest> getShmStagedActivityRequest() {
        return shmStagedActivityRequest;
    }

    public void setShmStagedActivityRequest(final List<SHMStagedActivityRequest> shmStagedActivityRequest) {
        this.shmStagedActivityRequest = shmStagedActivityRequest;
    }

}