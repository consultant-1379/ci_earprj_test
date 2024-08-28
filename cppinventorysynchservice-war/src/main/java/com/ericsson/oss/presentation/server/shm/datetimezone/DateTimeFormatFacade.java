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
package com.ericsson.oss.presentation.server.shm.datetimezone;

import java.util.Date;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ericsson.oss.services.shm.common.DateTimeUtils;
import com.ericsson.oss.services.shm.common.DateWithTimeZone;

/**
 * Class for handling the rest call for server time
 * 
 * @author xamakha
 * 
 */

@Path("/servertime")
public class DateTimeFormatFacade {

    @Inject
    DateTimeUtils dateTimeUtils;

    /**
     * Method to retrieve the server time , zone and offset
     * 
     * @URL http://localhost:8080/oss/shm/rest/servertime/getTimeOffset
     * 
     * @return JSON response {"date":1436338924280,"offset":3600000, "serverLocation":"Eire"}
     */

    @GET
    @Path("/getTimeOffset/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimeOffset() {
        final DateWithTimeZone dateWithTimeZone = dateTimeUtils.getDateTimeAndOffsetWithZoneOfDate(new Date());
        return Response.status(Response.Status.OK).entity(dateWithTimeZone).build();
    }
}
