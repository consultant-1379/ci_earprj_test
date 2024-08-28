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
package com.ericsson.oss.services.shm.accesscontrol;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/rbac")
public interface AccessControl {
    @GET
    @Path("/createjob")
    Response checkAccessForCreateJob();

    @GET
    @Path("/deletejob")
    Response checkAccessForDeletJob();

    @GET
    @Path("/viewinventory")
    Response checkAccessForViewInventory();

    @GET
    @Path("/viewjobs")
    Response checkAccessForViewJobs();

    @GET
    @Path("/viewswpackages")
    Response checkAccessForViewSWPackages();

    @GET
    @Path("/viewjoblogs")
    Response checkAccessForViewJobLogs();

    @GET
    @Path("/exportjoblogs")
    Response checkAccessForExportJobLogs();

    @GET
    @Path("/importfile")
    Response checkAccessForImportFile();

    @GET
    @Path("/deletefile")
    Response checkAccessForDeleteFile();

    @GET
    @Path("/controljob")
    Response checkAccessForControlJob();

    @GET
    @Path("/canceljob")
    Response checkAccessForCancelJob();

    @GET
    @Path("/deleteswpkg")
    Response checkAccessForDeleteSWPackage();

    @GET
    @Path("/deletelkf")
    Response checkAccessForDeleteLicenseKeyFiles();

    @GET
    @Path("/jobs/continue")
    Response checkAccessForInvocationOfMainJob();

    @GET
    @Path("/nejobs/continue")
    Response checkAccessForInvocationOfNeJob();

    @GET
    @Path("/launch-ops-gui")
    Response checkAccessToLaunchOpsGui();

}