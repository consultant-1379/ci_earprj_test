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

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.axe.service.ShmAxeService;
import com.ericsson.oss.services.shm.jobservice.axe.OpsInputData;
import com.ericsson.oss.services.shm.jobservice.axe.OpsResponseData;

/**
 * This Class Implements ShmAxeResource Interface and will generate the response accordingly
 * 
 * @author Team Royals
 *
 */
public class ShmAxeResourceImpl implements ShmAxeResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShmAxeResourceImpl.class);

    @Inject
    private ShmAxeService shmAxeService;

    @Override
    public Response getSessionIdAndClusterId(final OpsInputData opsInputData) {
        LOGGER.debug("in getSessionIdAndClusterId {}", opsInputData);
        if (opsInputData.getNeTypeToNeJobs().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Request,need valid inputs in Request").build();
        }
        final OpsResponseData opsResponseData = shmAxeService.getSessionIdAndClusterId(opsInputData);
        return Response.status(Response.Status.OK).entity(opsResponseData).build();
    }

}
