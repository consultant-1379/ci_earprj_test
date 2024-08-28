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
package com.ericsson.oss.presentation.server.shm.util;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.ericsson.oss.services.shm.common.FdnFilterResponse;
import com.ericsson.oss.services.shm.common.FdnServiceBean;

@Path("fdn")
public class FdnValidationFacade {

    @Inject
    private FdnServiceBean fdnServiceBean;

    @POST
    @Path("/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public FdnFilterResponse filterUnSupportedFdns(final Set<String> fdns) {
        return fdnServiceBean.filterUnSupportedFdns(fdns);
    }
}
