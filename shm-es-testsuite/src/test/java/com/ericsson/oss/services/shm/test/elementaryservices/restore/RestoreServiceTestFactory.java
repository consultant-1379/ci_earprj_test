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
package com.ericsson.oss.services.shm.test.elementaryservices.restore;

import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.es.api.Activity;

/*
 * @author xmanush
 */
@Stateless
public class RestoreServiceTestFactory {

    @EServiceRef(qualifier = "CPP.RESTORE.restore")
    private Activity remoteRestoreES;

    @EServiceRef(qualifier = "CPP.RESTORE.install")
    private Activity remoteRestoreInstallES;

    @EServiceRef(qualifier = "CPP.RESTORE.confirm")
    private Activity remoteRestoreConfirmES;

    @EServiceRef(qualifier = "CPP.RESTORE.download")
    private Activity remoteRestoreDownloadES;

    @EServiceRef(qualifier = "CPP.RESTORE.verify")
    private Activity remoteRestoreVerifyES;

    public Activity getConfirmRestoreService() {
        return remoteRestoreConfirmES;
    }

    public Activity getRestoreService() {
        return remoteRestoreES;
    }

    public Activity getInstallService() {
        return remoteRestoreInstallES;
    }

    public Activity getDownloadCVService() {
        return remoteRestoreDownloadES;
    }

    public Activity getVerifyService() {
        return remoteRestoreVerifyES;
    }

}
