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
package com.ericsson.oss.services.shm.test.elementaryservices.upgrade;

import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.es.api.Activity;

/*
 * @author xcharoh
 */
@Stateless
public class InstallServiceTestFactory {

    @EServiceRef(qualifier = "CPP.UPGRADE.install")
    private Activity remoteInstallService;

    public Activity getInstallService() {
        return remoteInstallService;
    }

}
