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
package com.ericsson.oss.services.shm.job.rbac.impl;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.EPredefinedRole;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.services.shm.job.rbac.AccessCheck;

public class AccessCheckImpl implements AccessCheck {

    private static final String GRANT = "grant";

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.CREATE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public String createJob() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.DELETE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public String deleteJob() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    public String viewInventory() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    public String viewJobs() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    public String viewSWPackages() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    public String viewJobLogs() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    public String exportJobLogs() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.EXECUTE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR,
            EPredefinedRole.OPERATOR })
    public String importFile() {
        return "grant";
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.DELETE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public String deleteFile() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.UPDATE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public String controlJob() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.DELETE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public String deleteLkf() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.DELETE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public String deleteSWPackage() {
        return GRANT;
    }

    // the actionId given for this operation is temparory. Need to update once multiple policies are defined
    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.UPDATE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public String cancelJob() {
        return GRANT;
    }

    // the actionId given for this operation is temparory. Need to update once multiple policies are defined
    @Override
    @Authorize(resource = AccessControlConstants.SHM_SUPERVISION_CONTROLLER_SERVICE, action = AccessControlConstants.UPDATE_OPERATION, role = { EPredefinedRole.ADMINISTRATOR })
    public String manualInvocationOfJob() {
        return GRANT;
    }

    @Override
    @Authorize(resource = AccessControlConstants.OPS_ENM, action = AccessControlConstants.EXECUTE_OPERATION)
    public String launchOpsGui() {
        return GRANT;
    }

}
