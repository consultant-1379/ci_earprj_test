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
package com.ericsson.oss.services.shm.job.rbac;

public interface AccessCheck {

    String createJob();

    String deleteJob();

    String viewInventory();

    String viewJobs();

    String viewSWPackages();

    String viewJobLogs();

    String exportJobLogs();

    String importFile();

    String deleteFile();

    String controlJob();

    String deleteLkf();

    String deleteSWPackage();

    String cancelJob();

    String manualInvocationOfJob();

    String launchOpsGui();
}
