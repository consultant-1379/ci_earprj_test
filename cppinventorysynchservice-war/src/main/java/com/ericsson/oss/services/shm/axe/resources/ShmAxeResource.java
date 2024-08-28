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
package com.ericsson.oss.services.shm.axe.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ericsson.oss.services.shm.jobservice.axe.OpsInputData;

/**
 * This interface is used to listen to the axe nodes related rest url and generate the response accordingly
 * 
 * @author Team Royals
 *
 */
@Path("/axe")
public interface ShmAxeResource {

    /**
     * Method to get ops cluster id and session id for given axe nodes
     * 
     * @URL url : http://localhost:8080/oss/shm/rest/job/ops-sessionid-clusterid
     * @param opsInputData
     *            contains jobType, nejobids and node names
     * 
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ops-sessionid-clusterid")
    Response getSessionIdAndClusterId(final OpsInputData opsInputData);
}
