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
package com.ericsson.oss.services.shm.test.elementaryservices.license;

import javax.ejb.Stateless;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.services.shm.es.api.Activity;

/*
 * @author xmanush
 */
@Stateless
public class InstallLicenseKeyFileServiceTestFactory {

    @EServiceRef(qualifier = "CPP.LICENSE.install")
    private Activity remoteES;

    public Activity getInstallLicenseKeyFileService() {
        System.out.println("License Object");
        return remoteES;
    }
}
