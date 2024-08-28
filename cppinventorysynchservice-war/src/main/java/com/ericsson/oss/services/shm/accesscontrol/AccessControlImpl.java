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

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.ericsson.oss.services.shm.job.rbac.AccessCheck;

@Path("/rbac")
public class AccessControlImpl implements AccessControl {
    @Inject
    AccessCheck accessCheck;

    public Response checkAccessForCreateJob() {
        return Response.status(Response.Status.OK).entity(accessCheck.createJob()).build();
    }

    public Response checkAccessForDeletJob() {
        return Response.status(Response.Status.OK).entity(accessCheck.deleteJob()).build();
    }

    public Response checkAccessForViewInventory() {
        return Response.status(Response.Status.OK).entity(accessCheck.viewInventory()).build();
    }

    public Response checkAccessForViewJobs() {
        return Response.status(Response.Status.OK).entity(accessCheck.viewJobs()).build();
    }

    public Response checkAccessForViewSWPackages() {
        return Response.status(Response.Status.OK).entity(accessCheck.viewSWPackages()).build();
    }

    public Response checkAccessForViewJobLogs() {
        return Response.status(Response.Status.OK).entity(accessCheck.viewJobLogs()).build();
    }

    public Response checkAccessForExportJobLogs() {
        return Response.status(Response.Status.OK).entity(accessCheck.exportJobLogs()).build();
    }

    public Response checkAccessForImportFile() {
        return Response.status(Response.Status.OK).entity(accessCheck.importFile()).build();
    }

    public Response checkAccessForDeleteFile() {
        return Response.status(Response.Status.OK).entity(accessCheck.deleteFile()).build();
    }

    public Response checkAccessForControlJob() {
        return Response.status(Response.Status.OK).entity(accessCheck.controlJob()).build();
    }

    @Override
    public Response checkAccessForCancelJob() {
        return Response.status(Response.Status.OK).entity(accessCheck.cancelJob()).build();
    }

    @Override
    public Response checkAccessForDeleteSWPackage() {
        return Response.status(Response.Status.OK).entity(accessCheck.deleteSWPackage()).build();
    }

    @Override
    public Response checkAccessForDeleteLicenseKeyFiles() {
        return Response.status(Response.Status.OK).entity(accessCheck.deleteLkf()).build();
    }

    @Override
    public Response checkAccessForInvocationOfMainJob() {
        return Response.status(Response.Status.OK).entity(accessCheck.manualInvocationOfJob()).build();
    }

    @Override
    public Response checkAccessForInvocationOfNeJob() {
        return Response.status(Response.Status.OK).entity(accessCheck.manualInvocationOfJob()).build();
    }

    @Override
    public Response checkAccessToLaunchOpsGui() {
        return Response.status(Response.Status.OK).entity(accessCheck.launchOpsGui()).build();
    }

}
